-- Topic AI summaries: stores Gemini-generated narrative + TTS audio path + attributed segments
CREATE TABLE IF NOT EXISTS public.topic_summaries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    topic_id UUID NOT NULL UNIQUE REFERENCES public.topics(id) ON DELETE CASCADE,
    audio_path TEXT,                         -- storage path under audio-notes/ bucket
    duration_seconds FLOAT DEFAULT 0,
    segments JSONB DEFAULT '[]'::jsonb,      -- [{text, start_ms, attributed_to: [userId,...]}]
    generated_at TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE public.topic_summaries ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can read summaries"
    ON public.topic_summaries FOR SELECT USING (true);

CREATE POLICY "Service role can insert/update summaries"
    ON public.topic_summaries FOR ALL USING (auth.role() = 'service_role');

GRANT SELECT ON public.topic_summaries TO anon, authenticated;
GRANT ALL ON public.topic_summaries TO service_role;
