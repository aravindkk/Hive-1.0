-- Add topic_id to voices table
alter table public.voices
add column topic_id uuid references public.topics(id);

-- Create index for faster lookups by topic
create index voices_topic_id_idx on public.voices(topic_id);

-- Update RLS if necessary (currently public read is fine for MVP)
