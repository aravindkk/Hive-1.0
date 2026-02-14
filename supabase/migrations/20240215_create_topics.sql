-- Enable PostGIS extension if not already enabled
create extension if not exists postgis schema extensions;

-- Create Topics Table
create table public.topics (
  id uuid not null default gen_random_uuid (),
  title text not null,
  location geography(point) not null,
  radius int not null default 500, -- meters
  created_at timestamptz not null default now(),
  active boolean not null default true,
  constraint topics_pkey primary key (id)
);

-- Index for fast geospatial queries
create index topics_geo_index on public.topics using gist (location);

-- RLS Policies
alter table public.topics enable row level security;

create policy "Enable read access for all users"
on public.topics
for select
to public
using (true);

create policy "Enable insert for authenticated users only"
on public.topics
for insert
to authenticated
with check (true);

-- RPC Function to find nearby topics
-- Usage: select * from get_nearby_topics(lat, long, radius_meters)
create or replace function get_nearby_topics(lat float, long float, radius_meters int)
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
  where st_dwithin(
    location,
    st_point(long, lat)::geography,
    radius_meters
  )
  and active = true
  order by location <-> st_point(long, lat)::geography;
$$;

-- RPC Function to create a topic
create or replace function create_topic(title text, lat float, long float, radius int)
returns void
language sql
as $$
  insert into public.topics (title, location, radius)
  values (title, st_point(long, lat)::geography, radius);
$$;
