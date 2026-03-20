-- Only show topics with activity in the last 3 days:
-- either the topic was created recently, or at least one voice was added recently.
-- voice_count is still all-time (used for bubble sizing).

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
  AND (
    t.created_at > NOW() - INTERVAL '3 days'
    OR EXISTS (
      SELECT 1 FROM public.voices rv
      WHERE rv.topic_id = t.id
      AND rv.created_at > NOW() - INTERVAL '3 days'
    )
  )
  GROUP BY t.id
  ORDER BY voice_count DESC;
$$;

GRANT EXECUTE ON FUNCTION public.get_popular_topics() TO anon, authenticated, service_role;
