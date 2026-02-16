-- RPC Function to fetch topics sorted by popularity (voice count)
-- Usage: select * from get_popular_topics()
create or replace function get_popular_topics()
returns table (
  id uuid,
  title text,
  latitude float,
  longitude float,
  radius int,
  active boolean,
  voice_count bigint
)
language sql
as $$
  select
    t.id,
    t.title,
    st_y(t.location::geometry) as latitude,
    st_x(t.location::geometry) as longitude,
    t.radius,
    t.active,
    count(v.id) as voice_count
  from public.topics t
  left join public.voices v on t.id = v.topic_id
  where t.active = true
  group by t.id
  order by voice_count desc;
$$;
