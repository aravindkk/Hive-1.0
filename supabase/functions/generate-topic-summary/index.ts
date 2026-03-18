import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.21.0";
import { GoogleGenerativeAI } from "https://esm.sh/@google/generative-ai@0.1.3";

declare const EdgeRuntime: { waitUntil: (promise: Promise<unknown>) => void };

const corsHeaders = {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

function estimateTimings(segments: { text: string; attributed_to: string[] }[]): {
    text: string;
    start_ms: number;
    attributed_to: string[];
}[] {
    const MS_PER_WORD = (60 / 150) * 1000;
    const PAUSE_MS = 300;
    let cumMs = 0;
    return segments.map((seg) => {
        const wordCount = seg.text.trim().split(/\s+/).length;
        const startMs = cumMs;
        cumMs += wordCount * MS_PER_WORD + PAUSE_MS;
        return { text: seg.text, start_ms: Math.round(startMs), attributed_to: seg.attributed_to };
    });
}

async function generateSummary(topic_id: string) {
    const supabase = createClient(
        Deno.env.get("SUPABASE_URL") ?? "",
        Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
        { auth: { persistSession: false } }
    );

    const apiKey = Deno.env.get("GEMINI_API_KEY");
    if (!apiKey) throw new Error("GEMINI_API_KEY not set");
    const genAI = new GoogleGenerativeAI(apiKey);
    const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });

    const { data: topic } = await supabase
        .from("topics")
        .select("id, title")
        .eq("id", topic_id)
        .single();
    if (!topic) throw new Error("Topic not found");

    const { data: voices } = await supabase
        .from("voices")
        .select("id, user_id, transcript")
        .eq("topic_id", topic_id)
        .not("transcript", "is", null)
        .order("created_at", { ascending: true });

    if (!voices || voices.length === 0) {
        console.log(`No transcribed voices for topic ${topic_id}, skipping`);
        return;
    }

    const contributions = voices
        .map((v: any) => `[${v.user_id.slice(0, 8)}]: "${v.transcript}"`)
        .join("\n");

    const summaryPrompt = `You are summarizing a neighborhood voice discussion for the Hive app. Assume you are a person in that neighbourhood.

Topic: "${topic.title}"

Contributor voices (format: [userId]: "transcript"):
${contributions}

Task: Write a 60-120 second informally spoken narrative summary of this discussion. Structure it as:
1. Open with the core theme and why it matters to the neighborhood (1-2 sentences)
2. Group similar perspectives together ("Many residents feel...", "Some people note...")
3. Highlight contrasting viewpoints if any
4. Close with the overall sentiment and emerging consensus

Return ONLY valid JSON, no markdown:
{
  "segments": [
    {
      "text": "The spoken text for this segment",
      "attributed_to": ["userId1", "userId2"]
    }
  ]
}

Rules:
- Each segment should be 1-2 sentences
- "attributed_to" lists the user IDs whose voices most support this segment (use the 8-char prefix IDs from the contributions list)
- If a segment is the intro/outro with no direct attribution, use an empty array
- Keep language conversational and suited for text-to-speech
- Write in present tense, as if summarizing live community sentiment`;

    const result = await model.generateContent(summaryPrompt);
    const rawText = result.response.text().trim()
        .replace(/```json\n?/g, "").replace(/```\n?/g, "").trim();

    let parsed: { segments: { text: string; attributed_to: string[] }[] };
    try {
        parsed = JSON.parse(rawText);
    } catch {
        throw new Error("Gemini returned invalid JSON for summary");
    }

    if (!parsed.segments || parsed.segments.length === 0) {
        throw new Error("No segments generated");
    }

    const timedSegments = estimateTimings(parsed.segments);
    const lastSeg = timedSegments[timedSegments.length - 1];
    const totalDurationSeconds =
        (lastSeg.start_ms + lastSeg.text.split(/\s+/).length * (60000 / 150)) / 1000;

    // TTS
    const ttsApiKey = Deno.env.get("GOOGLE_TTS_API_KEY") ?? apiKey;
    const fullNarrative = parsed.segments.map((s) => s.text).join(" ");

    let audioPath: string | null = null;
    try {
        const ttsResponse = await fetch(
            `https://texttospeech.googleapis.com/v1/text:synthesize?key=${ttsApiKey}`,
            {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    input: { text: fullNarrative },
                    voice: { languageCode: "en-IN", name: "en-IN-Neural2-B" },
                    audioConfig: { audioEncoding: "MP3" },
                }),
            }
        );

        if (!ttsResponse.ok) {
            console.error(`TTS HTTP ${ttsResponse.status}: ${await ttsResponse.text()}`);
        } else {
            const ttsData = await ttsResponse.json();
            const audioBase64: string = ttsData.audioContent;
            if (audioBase64) {
                const binaryStr = atob(audioBase64);
                const bytes = new Uint8Array(binaryStr.length);
                for (let i = 0; i < binaryStr.length; i++) bytes[i] = binaryStr.charCodeAt(i);

                const storagePath = `summaries/${topic_id}.mp3`;
                const { error: uploadError } = await supabase.storage
                    .from("audio-notes")
                    .upload(storagePath, bytes, { contentType: "audio/mpeg", upsert: true });

                if (!uploadError) {
                    audioPath = storagePath;
                    console.log(`TTS audio uploaded to ${storagePath}`);
                } else {
                    console.error("TTS upload error:", uploadError.message);
                }
            }
        }
    } catch (ttsErr) {
        console.error("TTS generation failed:", ttsErr);
    }

    const { error: upsertError } = await supabase
        .from("topic_summaries")
        .upsert(
            {
                topic_id,
                audio_path: audioPath,
                duration_seconds: totalDurationSeconds,
                segments: timedSegments,
                generated_at: new Date().toISOString(),
            },
            { onConflict: "topic_id" }
        );

    if (upsertError) throw upsertError;
    console.log(`Summary upserted for topic ${topic_id}, audioPath=${audioPath}`);
}

serve(async (req) => {
    if (req.method === "OPTIONS") {
        return new Response("ok", { headers: corsHeaders });
    }

    try {
        const { topic_id } = await req.json();
        if (!topic_id) throw new Error("Missing topic_id");

        // Respond immediately; do the heavy work in the background
        EdgeRuntime.waitUntil(
            generateSummary(topic_id).catch((e) =>
                console.error(`generateSummary failed for ${topic_id}:`, e.message)
            )
        );

        return new Response(
            JSON.stringify({ status: "generating", topic_id }),
            { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 202 }
        );
    } catch (error) {
        console.error("Error:", error.message);
        return new Response(
            JSON.stringify({ error: error.message }),
            { headers: { ...corsHeaders, "Content-Type": "application/json" }, status: 400 }
        );
    }
});
