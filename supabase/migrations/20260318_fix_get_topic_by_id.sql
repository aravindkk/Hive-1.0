-- Fix: parameter name `topic_id` conflicted with column `v.topic_id` in the JOIN.
-- PostgreSQL was resolving `WHERE t.id = topic_id` to the column, not the parameter,
-- causing the function to return all topics instead of filtering by the given ID.
DROP FUNCTION IF EXISTS public.get_topic_by_id(uuid);

CREATE OR REPLACE FUNCTION public.get_topic_by_id(p_topic_id uuid)
RETURNS TABLE (
  id uuid,
  title text,
  latitude float,
  longitude float,
  radius int,
  active boolean,
  voice_count bigint,
  created_at timestamptz
)
LANGUAGE sql AS $$
  SELECT
    t.id,
    t.title,
    ST_Y(t.location::geometry) AS latitude,
    ST_X(t.location::geometry) AS longitude,
    t.radius,
    t.active,
    COUNT(v.id) AS voice_count,
    t.created_at
  FROM public.topics t
  LEFT JOIN public.voices v ON t.id = v.topic_id
  WHERE t.id = p_topic_id
  GROUP BY t.id;
$$;

GRANT EXECUTE ON FUNCTION public.get_topic_by_id(uuid) TO anon, authenticated, service_role;
