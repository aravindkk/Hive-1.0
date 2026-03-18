CREATE OR REPLACE FUNCTION public.get_topic_by_id(topic_id uuid)
RETURNS TABLE (
  id uuid,
  title text,
  latitude float,
  longitude float,
  radius int,
  active boolean,
  voice_count bigint
)
LANGUAGE sql AS $$
  SELECT
    t.id,
    t.title,
    ST_Y(t.location::geometry) AS latitude,
    ST_X(t.location::geometry) AS longitude,
    t.radius,
    t.active,
    COUNT(v.id) AS voice_count
  FROM public.topics t
  LEFT JOIN public.voices v ON t.id = v.topic_id
  WHERE t.id = topic_id
  GROUP BY t.id;
$$;

GRANT EXECUTE ON FUNCTION public.get_topic_by_id(uuid) TO anon, authenticated, service_role;
