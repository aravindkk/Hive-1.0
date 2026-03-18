ALTER TABLE public.voices
  ADD COLUMN IF NOT EXISTS classification TEXT DEFAULT 'pending';

-- values: 'pending' | 'personal' | 'community' | 'community_new'
