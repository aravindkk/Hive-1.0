
import { serve } from "https://deno.land/std@0.168.0/http/server.ts";
import { createClient } from "https://esm.sh/@supabase/supabase-js@2.21.0";
import { GoogleGenerativeAI } from "https://esm.sh/@google/generative-ai@0.1.3";

const corsHeaders = {
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
};

serve(async (req) => {
    // Handle CORS
    if (req.method === "OPTIONS") {
        return new Response("ok", { headers: corsHeaders });
    }

    try {
        const { storage_path } = await req.json();

        if (!storage_path) {
            throw new Error("Missing storage_path");
        }

        console.log(`Starting transcription for: ${storage_path}`);

        // 1. Initialize Supabase
        // Using Service Role Key to bypass RLS for reading/writing logic if needed, 
        // but standard anon key might suffice if policies are open. 
        // However, function usually needs admin rights to update 'transcript' column if RLS restricts it.
        const supabaseClient = createClient(
            Deno.env.get("SUPABASE_URL") ?? "",
            Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
            {
                auth: {
                    persistSession: false,
                },
            }
        );

        // 2. Download File
        const { data: fileData, error: downloadError } = await supabaseClient.storage
            .from("audio-notes")
            .download(storage_path);

        if (downloadError) throw downloadError;

        if (!fileData) throw new Error("File not found or empty.");

        console.log("File downloaded successfully.");

        // 3. Convert Blob to Base64 (Gemini inline data)
        // Note: This loads file into memory. Ensure functions has enough RAM (150MB limit on free tier).
        const arrayBuffer = await fileData.arrayBuffer();
        const bytes = new Uint8Array(arrayBuffer);

        // Efficient Base64 encoding for Deno
        let binary = "";
        const len = bytes.byteLength;
        for (let i = 0; i < len; i++) {
            binary += String.fromCharCode(bytes[i]);
        }
        const base64Audio = btoa(binary);

        // 4. Call Gemini
        const apiKey = Deno.env.get("GEMINI_API_KEY");
        if (!apiKey) throw new Error("GEMINI_API_KEY not set");

        const genAI = new GoogleGenerativeAI(apiKey);
        const model = genAI.getGenerativeModel({ model: "gemini-2.5-flash-lite" });

        const prompt = "Transcribe the following audio file. Return only the transcript text. Do not include timestamps or speaker labels unless clear. If audio is unclear, say [Unclear Audio].";

        const result = await model.generateContent([
            prompt,
            {
                inlineData: {
                    mimeType: "audio/mp4", // Adjust if using different format
                    data: base64Audio,
                },
            },
        ]);
        const response = await result.response;
        const transcript = response.text();

        console.log("Transcription complete.");

        // 5. Update Database
        const { error: updateError } = await supabaseClient
            .from("voices")
            .update({ transcript: transcript })
            .eq("storage_path", storage_path);

        if (updateError) throw updateError;

        return new Response(JSON.stringify({ transcript }), {
            headers: { ...corsHeaders, "Content-Type": "application/json" },
            status: 200,
        });
    } catch (error) {
        console.error("Error:", error.message);
        return new Response(JSON.stringify({ error: error.message }), {
            headers: { ...corsHeaders, "Content-Type": "application/json" },
            status: 400,
        });
    }
});
