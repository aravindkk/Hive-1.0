import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.21.0";
import { GoogleGenerativeAI } from "https://esm.sh/@google/generative-ai@0.1.3";

declare const EdgeRuntime: { waitUntil: (promise: Promise<unknown>) => void };

const OLD_TAGS = new Set([
    "Traffic", "Food", "Community", "Safety", "Infrastructure", "Health",
    "Nature", "Environment", "Work", "Education", "Events", "Idea", "Personal",
]);

function needsBackfill(mood_tags: string[] | null): boolean {
    if (!mood_tags || mood_tags.length === 0) return true;
    return mood_tags.some((t) => OLD_TAGS.has(t));
}

const corsHeaders = {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

async function extractTags(model: any, transcript: string): Promise<string[]> {
    const result = await model.generateContent(
        `Extract 1-2 mood tags that best describe the emotional tone of this voice note. Choose ONLY from: Excited, Grateful, Energized, Happy, Hopeful, Calm, Reflective, Curious, Nostalgic, Determined, Focused, Frustrated, Concerned, Anxious, Stressed, Disappointed. Return ONLY valid JSON with no markdown: {"tags": ["Tag1"]}. Transcript: "${transcript.replace(/"/g, "'")}"`
    );
    const rawText = result.response.text().trim()
        .replace(/```json\n?/g, "").replace(/```\n?/g, "").trim();
    const parsed = JSON.parse(rawText);
    return (parsed.tags ?? []).slice(0, 3) as string[];
}

async function runBackfill() {
    const supabase = createClient(
        Deno.env.get("SUPABASE_URL") ?? "",
        Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
        { auth: { persistSession: false } }
    );

    const apiKey = Deno.env.get("GEMINI_API_KEY");
    if (!apiKey) throw new Error("GEMINI_API_KEY not set");
    const genAI = new GoogleGenerativeAI(apiKey);
    const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash" });

    // Fetch all voices that have a transcript but no mood tags (empty array or null)
    const { data: allVoices, error } = await supabase
        .from("voices")
        .select("id, transcript, mood_tags")
        .not("transcript", "is", null);

    if (error) {
        console.error("Failed to fetch voices:", error.message);
        return;
    }

    const voices = (allVoices ?? []).filter((v: any) => needsBackfill(v.mood_tags));

    if (voices.length === 0) {
        console.log("No voices need backfilling.");
        return;
    }

    console.log(`Backfilling mood tags for ${voices.length} voices (${(allVoices ?? []).length} total)...`);

    let succeeded = 0;
    let failed = 0;

    for (const voice of voices as { id: string; transcript: string }[]) {
        try {
            const tags = await extractTags(model, voice.transcript);

            const { error: updateError } = await supabase
                .from("voices")
                .update({ mood_tags: tags })
                .eq("id", voice.id);

            if (updateError) {
                console.error(`Failed to update voice ${voice.id}:`, updateError.message);
                failed++;
            } else {
                console.log(`voice ${voice.id} → [${tags.join(", ")}]`);
                succeeded++;
            }
        } catch (e) {
            console.error(`Error processing voice ${voice.id}:`, e);
            failed++;
        }

        // Small delay to avoid hitting Gemini rate limits
        await new Promise((r) => setTimeout(r, 250));
    }

    console.log(`Backfill complete. succeeded=${succeeded}, failed=${failed}`);
}

serve(async (req) => {
    if (req.method === "OPTIONS") {
        return new Response("ok", { headers: corsHeaders });
    }

    try {
        // Count how many voices need backfilling so we can include it in the response
        const supabase = createClient(
            Deno.env.get("SUPABASE_URL") ?? "",
            Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
            { auth: { persistSession: false } }
        );

        const { data: sample } = await supabase
            .from("voices")
            .select("mood_tags")
            .not("transcript", "is", null);

        const toProcess = (sample ?? []).filter((v: any) => needsBackfill(v.mood_tags)).length;

        EdgeRuntime.waitUntil(
            runBackfill().catch((e) => console.error("Backfill failed:", e.message))
        );

        return new Response(
            JSON.stringify({ status: "backfilling", voices_to_process: toProcess }),
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
