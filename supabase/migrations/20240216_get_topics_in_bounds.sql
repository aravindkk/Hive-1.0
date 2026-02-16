-- RPC Function to find topics within a bounding box (viewport)
-- Usage: select * from get_topics_in_bounds(min_lat, min_long, max_lat, max_long)
create or replace function get_topics_in_bounds(min_lat float, min_long float, max_lat float, max_long float)
returns table (
  id uuid,
  title text,
  latitude float,
  longitude float,
  radius int,
  active boolean
)
language sql
as $$
  select
    id,
    title,
    st_y(location::geometry) as latitude,
    st_x(location::geometry) as longitude,
    radius,
    active
  from public.topics
  where location && st_makeenvelope(min_long, min_lat, max_long, max_lat, 4326)::geography
  and active = true;
$$;
