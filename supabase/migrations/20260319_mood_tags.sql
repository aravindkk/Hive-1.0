-- Add mood_tags column to voices for AI journaling feature
ALTER TABLE voices ADD COLUMN IF NOT EXISTS mood_tags TEXT[] DEFAULT '{}';
