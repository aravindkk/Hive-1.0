-- Seed Data for Bangalore Topics
-- Schema Verified: public.topics (title, location, radius, created_at, active)

-- HSR Layout (12.9121, 77.6446)
INSERT INTO public.topics (title, location, radius, created_at)
VALUES 
('HSR Startup Founders', st_point(77.6446, 12.9121)::geography, 2000, now()),
('HSR Foodies', st_point(77.6446, 12.9121)::geography, 2000, now()),
('Agara Lake Morning Run', st_point(77.6377, 12.9213)::geography, 1000, now());

-- Jayanagar (12.9308, 77.5838)
INSERT INTO public.topics (title, location, radius, created_at)
VALUES 
('Classic Old Bangalore', st_point(77.5838, 12.9308)::geography, 3000, now()),
('4th Block Shopping', st_point(77.5838, 12.9308)::geography, 1000, now());

-- Koramangala (12.9352, 77.6247)
INSERT INTO public.topics (title, location, radius, created_at)
VALUES 
('Koramangala Nightlife', st_point(77.6247, 12.9352)::geography, 2000, now()),
('Sony Signal Traffic', st_point(77.6300, 12.9340)::geography, 1000, now());

-- Indiranagar (12.9716, 77.6412)
INSERT INTO public.topics (title, location, radius, created_at)
VALUES 
('100 Feet Road', st_point(77.6412, 12.9716)::geography, 2000, now());
