create table public.voices (
  id uuid not null default gen_random_uuid (),
  user_id uuid not null references auth.users(id),
  storage_path text not null,
  transcript text,
  created_at timestamptz not null default now(),
  constraint voices_pkey primary key (id)
);

create index voices_user_id_idx on public.voices(user_id);
