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
        const { user_id } = await req.json();
        if (!user_id) throw new Error("Missing user_id");

        const supabase = createClient(
            Deno.env.get("SUPABASE_URL") ?? "",
            Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
            { auth: { persistSession: false } }
        );

        const apiKey = Deno.env.get("GEMINI_API_KEY");
        if (!apiKey) throw new Error("GEMINI_API_KEY not set");
        const genAI = new GoogleGenerativeAI(apiKey);
        const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });

        // Fetch last 7 days of voice notes for this user
        const sevenDaysAgo = new Date();
        sevenDaysAgo.setDate(sevenDaysAgo.getDate() - 7);

        const { data: voices } = await supabase
            .from("voices")
            .select("transcript, mood_tags, created_at, classification")
            .eq("user_id", user_id)
            .gte("created_at", sevenDaysAgo.toISOString())
            .not("transcript", "is", null)
            .order("created_at", { ascending: true });

        if (!voices || voices.length === 0) {
            return new Response(
                JSON.stringify({ teaser: null, audio_path: null }),
                { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 200 }
            );
        }

        const contributions = (voices as any[])
            .map((v) => {
                const date = new Date(v.created_at).toLocaleDateString("en-IN", {
                    weekday: "short",
                    timeZone: "Asia/Kolkata",
                });
                const tags = v.mood_tags && v.mood_tags.length > 0
                    ? ` [${v.mood_tags.join(", ")}]`
                    : "";
                return `${date}${tags}: "${v.transcript}"`;
            })
            .join("\n");

        // Step 1: generate the full narrative
        const narrativePrompt = `You are a warm personal AI journaling assistant. Based on these voice notes recorded this week, write a thoughtful 2-4 sentence reflection for the person.

Voice notes this week:
${contributions}

Rules:
- Write in second person ("You talked about...", "This week you...")
- Identify the main themes or emotional patterns across the week
- Be warm, specific, and encouraging — like a thoughtful friend
- Keep it conversational and suited for listening
- Max 80 words total

Return ONLY valid JSON: {"narrative": "..."}`;

        const narrativeResult = await model.generateContent(narrativePrompt);
        const narrativeRaw = narrativeResult.response.text().trim()
            .replace(/```json\n?/g, "").replace(/```\n?/g, "").trim();

        let narrative = "";
        try {
            narrative = JSON.parse(narrativeRaw).narrative ?? "";
        } catch {
            narrative = narrativeRaw.replace(/^\{.*"narrative"\s*:\s*"/, "").replace(/"\s*\}$/, "").trim();
        }

        if (!narrative) throw new Error("Narrative generation failed");

        // Step 2: teaser + TTS in parallel
        const teaserPrompt = `Write a single punchy sentence (max 8 words) that teases this weekly reflection. Be warm and intriguing. Always end with "Listen in!". Examples: "A busy week indeed. Listen in!", "Lots on your mind. Listen in!", "Upbeat week ahead. Listen in!".

Narrative: "${narrative}"

Return ONLY the sentence, nothing else.`;

        const ttsApiKey = Deno.env.get("GOOGLE_TTS_API_KEY") ?? apiKey;

        const [teaserResult, ttsResponse] = await Promise.all([
            model.generateContent(teaserPrompt),
            fetch(`https://texttospeech.googleapis.com/v1/text:synthesize?key=${ttsApiKey}`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    input: { text: narrative },
                    voice: { languageCode: "en-IN", name: "en-IN-Chirp3-HD-Zubenelgenubi" },
                    audioConfig: { audioEncoding: "MP3" },
                }),
            }),
        ]);

        const teaser = teaserResult.response.text().trim().replace(/^["']|["']$/g, "");
        console.log(`Teaser: ${teaser}`);

        // Upload TTS audio
        let audioPath: string | null = null;
        try {
            if (!ttsResponse.ok) {
                console.error(`TTS HTTP ${ttsResponse.status}: ${await ttsResponse.text()}`);
            } else {
                const ttsData = await ttsResponse.json();
                const audioBase64: string = ttsData.audioContent;
                if (audioBase64) {
                    const binaryStr = atob(audioBase64);
                    const bytes = new Uint8Array(binaryStr.length);
                    for (let i = 0; i < binaryStr.length; i++) bytes[i] = binaryStr.charCodeAt(i);

                    const storagePath = `summaries/weekly/${user_id}.mp3`;
                    const { error: uploadError } = await supabase.storage
                        .from("audio-notes")
                        .upload(storagePath, bytes, { contentType: "audio/mpeg", upsert: true });

                    if (!uploadError) {
                        audioPath = storagePath;
                        console.log(`Weekly TTS uploaded to ${storagePath}`);
                    } else {
                        console.error("TTS upload error:", uploadError.message);
                    }
                }
            }
        } catch (ttsErr) {
            console.error("TTS generation failed:", ttsErr);
        }

        return new Response(
            JSON.stringify({ teaser, audio_path: audioPath }),
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
