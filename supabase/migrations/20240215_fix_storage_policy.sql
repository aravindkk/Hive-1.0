-- Drop existing policies to avoid conflicts if they partially exist
drop policy if exists "Public Access" on storage.objects;
drop policy if exists "Authenticated Uploads" on storage.objects;
drop policy if exists "User Update Own" on storage.objects;

-- Policy 1: Allow Public Read Access
create policy "Public Access"
on storage.objects for select
using ( bucket_id = 'audio-notes' );

-- Policy 2: Allow Authenticated Users to Upload
create policy "Authenticated Uploads"
on storage.objects for insert
to authenticated
with check ( bucket_id = 'audio-notes' );

-- Policy 3: Allow Users to Update their own files (Optional)
create policy "User Update Own"
on storage.objects for update
to authenticated
using ( bucket_id = 'audio-notes' and auth.uid() = owner );
