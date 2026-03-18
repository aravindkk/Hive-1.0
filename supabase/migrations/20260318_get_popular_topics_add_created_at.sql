-- Add created_at to get_popular_topics so the app can sort the New tab chronologically
DROP FUNCTION IF EXISTS public.get_popular_topics();

CREATE OR REPLACE FUNCTION public.get_popular_topics()
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
LANGUAGE sql
AS $$
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
  WHERE t.active = true
  GROUP BY t.id
  ORDER BY voice_count DESC;
$$;

GRANT EXECUTE ON FUNCTION public.get_popular_topics() TO anon, authenticated, service_role;
