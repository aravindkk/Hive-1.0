import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.21.0";
import { GoogleGenerativeAI } from "https://esm.sh/@google/generative-ai@0.1.3";

const corsHeaders = {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
    if (req.method === "OPTIONS") {
        return new Response("ok", { headers: corsHeaders });
    }

    try {
        const { storage_path, lat, lng, area_name: areaName, topic_id: existingTopicId } = await req.json();

        if (!storage_path) throw new Error("Missing storage_path");

        const supabase = createClient(
            Deno.env.get("SUPABASE_URL") ?? "",
            Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
            { auth: { persistSession: false } }
        );

        const apiKey = Deno.env.get("GEMINI_API_KEY");
        if (!apiKey) throw new Error("GEMINI_API_KEY not set");
        const genAI = new GoogleGenerativeAI(apiKey);
        const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });

        // 1. Download audio
        const { data: fileData, error: downloadError } = await supabase.storage
            .from("audio-notes")
            .download(storage_path);
        if (downloadError) throw downloadError;
        if (!fileData) throw new Error("File not found or empty.");

        // 2. Base64 encode
        const arrayBuffer = await fileData.arrayBuffer();
        const bytes = new Uint8Array(arrayBuffer);
        let binary = "";
        for (let i = 0; i < bytes.byteLength; i++) binary += String.fromCharCode(bytes[i]);
        const base64Audio = btoa(binary);

        // 3. Transcribe
        const transcribeResult = await model.generateContent([
            "Transcribe the following audio file. Return only the transcript text, no timestamps or labels. If unclear, say [Unclear Audio].",
            { inlineData: { mimeType: "audio/mp4", data: base64Audio } },
        ]);
        const transcript = transcribeResult.response.text().trim();
        console.log("Transcription complete:", transcript.substring(0, 100));

        // 3b. Location extraction + classification run in parallel (both only need the transcript)
        let effectiveLat = lat;
        let effectiveLng = lng;
        let effectiveAreaName = areaName ?? null;

        let classification = "personal";
        let matchedTopicId: string | null = null;
        let matchedTopicTitle: string | null = null;

        if (existingTopicId) {
            // User explicitly chose this topic — trust their intent, no LLM classification needed
            const { data: topic } = await supabase
                .from("topics")
                .select("id, title")
                .eq("id", existingTopicId)
                .single();

            if (topic) {
                matchedTopicId = topic.id;
                matchedTopicTitle = topic.title;
                classification = "community";
                console.log(`Skipping classification — adding to existing topic: ${matchedTopicTitle}`);
            }
        } else {
            // Fetch topics and run location extraction + classification in parallel
            const { data: topics } = await supabase
                .from("topics")
                .select("id, title")
                .eq("active", true)
                .limit(15);

            const topicList = topics && topics.length > 0
                ? topics.map((t: any) => `ID: ${t.id}, Title: "${t.title}"`).join("\n")
                : "";

            const locationPromise = model.generateContent(
                `Extract the neighborhood or area name mentioned in this transcript. Return ONLY the area name (e.g. "Indiranagar", "HSR Layout"), or "none" if no specific area is mentioned.\n\nTranscript: "${transcript}"`
            );

            const classPromise = topicList ? model.generateContent(`You are classifying a voice note from a neighborhood social app.

Transcript: "${transcript}"

Available community topics:
${topicList}

Rules:
- A topic only matches if BOTH the subject AND the geographic area/neighborhood are the same. Food in Jayanagar ≠ Food in HSR Layout — these are different topics.
- If the transcript is about a specific neighborhood topic that matches one of the above on both subject and location, return that topic's exact ID
- If it's about a neighborhood/community issue or a general topic(with no personal details) but no topic matches (different area, different subject, or area unclear), return "community_new"
- If it's personal (work stress, relationships, feelings, private thoughts), return "personal"

Respond with ONLY valid JSON, no markdown:
{"classification": "community" or "community_new" or "personal", "topic_id": "<exact uuid from list above or null>"}`) : Promise.resolve(null);

            const [locationResult, classResult] = await Promise.all([locationPromise, classPromise]);

            // Process location extraction result
            try {
                const mentionedLocation = locationResult.response.text().trim().replace(/^["']|["']$/g, "");
                if (mentionedLocation.toLowerCase() !== "none" && mentionedLocation.length > 0) {
                    console.log(`Location mentioned in transcript: ${mentionedLocation}`);
                    const query = encodeURIComponent(`${mentionedLocation}, Bangalore, India`);
                    const geoResponse = await fetch(
                        `https://nominatim.openstreetmap.org/search?q=${query}&format=json&limit=1`,
                        { headers: { "User-Agent": "HiveApp/1.0 (neighborhood social app)" } }
                    );
                    const geoData = await geoResponse.json();
                    if (geoData && geoData.length > 0) {
                        effectiveLat = parseFloat(geoData[0].lat);
                        effectiveLng = parseFloat(geoData[0].lon);
                        effectiveAreaName = mentionedLocation;
                        console.log(`Resolved ${mentionedLocation} → (${effectiveLat}, ${effectiveLng})`);
                    }
                }
            } catch (e) {
                console.error("Location extraction/geocoding failed, using GPS coords:", e);
            }

            // Process classification result
            if (classResult && topics && topics.length > 0) {
                try {
                    const classText = classResult.response.text().trim()
                        .replace(/```json\n?/g, "").replace(/```\n?/g, "").trim();
                    const parsed = JSON.parse(classText);
                    classification = parsed.classification || "personal";
                    matchedTopicId = parsed.topic_id || null;

                    if (matchedTopicId) {
                        const match = topics.find((t: any) => t.id === matchedTopicId);
                        if (match) {
                            matchedTopicTitle = match.title;
                        } else {
                            matchedTopicId = null;
                        }
                    }
                    if (classification === "community" && !matchedTopicId) {
                        classification = "community_new";
                    }
                } catch (e) {
                    console.error("Classification parse failed, defaulting to personal");
                    classification = "personal";
                }
            }

            // 6. If community_new, generate a broad topic title and create the topic
            if (classification === "community_new" && effectiveLat != null && effectiveLng != null) {
                const locationPrefix = effectiveAreaName && effectiveAreaName.trim().length > 0
                    ? `- Always prefix the title with "${effectiveAreaName}". Example: "${effectiveAreaName} Food & Restaurants"`
                    : `- Omit a location prefix.`;

                const titlePrompt = `Generate a short, broad community topic title for a neighborhood app based on this voice note.

Transcript: "${transcript}"

Rules:
- BROAD enough that 20+ different people in the same area could add their own experiences (e.g., "Food & Restaurants", "Traffic & Commute", "Water & Power Supply")
- NOT specific to one incident or place (e.g., NOT "New cafe on 12th Main" or "Pothole near forum mall")
${locationPrefix}
- Max 5 words total
- Respond with ONLY the topic title, nothing else`;

                try {
                    const titleResult = await model.generateContent(titlePrompt);
                    const generatedTitle = titleResult.response.text().trim().replace(/^["']|["']$/g, "");
                    console.log(`Generated topic title: ${generatedTitle}`);

                    const { data: newTopic, error: insertError } = await supabase
                        .from("topics")
                        .insert({
                            title: generatedTitle,
                            location: `SRID=4326;POINT(${effectiveLng} ${effectiveLat})`,
                            radius: 500,
                            active: true,
                        })
                        .select("id")
                        .single();

                    if (!insertError && newTopic) {
                        matchedTopicId = newTopic.id;
                        matchedTopicTitle = generatedTitle;
                        classification = "community";
                        console.log(`Created new topic: ${generatedTitle} (${matchedTopicId})`);
                    } else {
                        console.error("Failed to insert topic:", insertError?.message);
                    }
                } catch (e) {
                    console.error("Topic creation failed:", e);
                }
            }
        }

        console.log(`Classification: ${classification}, topic_id: ${matchedTopicId}`);

        // 7. Update voices row
        const updatePayload: any = { transcript, classification };
        if (matchedTopicId) updatePayload.topic_id = matchedTopicId;

        const { error: updateError } = await supabase
            .from("voices")
            .update(updatePayload)
            .eq("storage_path", storage_path);
        if (updateError) throw updateError;

        // 8. Fetch current voice_count for matched topic (for resonance display)
        const MILESTONE_THRESHOLDS = [5, 10, 50, 100, 500];
        let voiceCount = 0;
        let isMilestone = false;
        if (matchedTopicId) {
            const { count } = await supabase
                .from("voices")
                .select("id", { count: "exact", head: true })
                .eq("topic_id", matchedTopicId);
            voiceCount = count ?? 0;
            isMilestone = MILESTONE_THRESHOLDS.includes(voiceCount);

            // 9. Trigger AI summary generation in the background (fire and forget)
            const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
            const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

            // 9a. Fire trending notification at milestone thresholds
            const TRENDING_THRESHOLDS = [5, 10, 20];
            if (TRENDING_THRESHOLDS.includes(voiceCount)) {
                fetch(`${supabaseUrl}/functions/v1/send-trending-notification`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json", "Authorization": `Bearer ${serviceKey}` },
                    body: JSON.stringify({ topic_id: matchedTopicId, voice_count: voiceCount }),
                }).catch(() => {});
            }

            fetch(`${supabaseUrl}/functions/v1/generate-topic-summary`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${serviceKey}`,
                },
                body: JSON.stringify({ topic_id: matchedTopicId }),
            }).catch((e) => console.error("Failed to trigger summary generation:", e));
        }

        return new Response(
            JSON.stringify({ transcript, classification, topic_id: matchedTopicId, topic_title: matchedTopicTitle, voice_count: voiceCount, is_milestone: isMilestone }),
            { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 200 }
        );
    } catch (error) {
        console.error("Error:", error.message);
        return new Response(
            JSON.stringify({ error: error.message }),
            { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 400 }
        );
    }
});
