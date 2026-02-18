1. [Done-Fixed] The app is not asking any user permissions... (UI Logic Updated)
2. [Done] The voice recording screen has no prompt(see screenshot). It just shows a green mic. Have some random prompt like "What's happening in your area?" or "What's top of mind for you?", "How is it going?", "What's on your mind?" etc. to prompt user to click the mic and speak.
3. [Done] Feed screen and Hive screen should show user name and Settings icon at top right all the time. When user clicks on their username, it should go to the feed screen.
4. [Done-Fixed] Hive screen is stuck at waiting for location... (Added fallback to last known location)
5. [Done] When user records their voice, the voice upload is failing as seen in the screenshot.
6. [Done] [SQL Script Provided for Fix]
7. [Done] Getting this error repeatedly in Android studio in Logcat... (Fixed Ktor Engine Configuration)
8. [Done] The user name is not fancy or memorable... (Implemented Creative Username Generator)
9. [Done] Clicking on user name is not aking user to the Feed screen... (Fixed TopBar Click Handler)
10. [Done] The mic recording page no navigation buttons... (Integrated Recorder into Main Navigation)
11. [Done] [Open-1] The voice clips are stuck at processing... (Fixed Realtime Connection and Storage Policies)
12. [Done] Change the login screen to the attached screenshot.
13. [Done] The app should have a splash screen. Use the design attached as the splash screen.
14. [Done] Add the icon for this Android app(attached icon)
15. [Done] New voice recordings are not shown on the screen. Not sure if they are getting processed. No transcript is generated or shown to user.
16. [Done] Location permission is asked but it is not turning on the location on the phone. So while the location permission is requested from the user, the location is not turned on.

17.
Bug description: POST request to endpoint /functions/v1/transcribe-audio failed with exception Fail to prepare request body for sending: class java.util.Collections$SingletonMap
Reported by: User
Fix: In `VoiceRepositoryImpl.kt`, changed the request body from `mapOf()` to `buildJsonObject`. Ktor's serialization plugin (with Supabase) requires a strictly typed JsonObject to serialize correctly when using the CIO engine.
Fix status: Done
Review status: Resolved

18.
Bug description: Can't replay the audio clip when I click on the play button in the Feed tab for previous voice clips.
Reported by: User
Fix: Implemented `AudioPlayer` class using Android `MediaPlayer`. Updated `TimelineViewModel` and `HiveViewModel` to manage playback state. Added play/stop logic to `TimelineScreen` and `TopicDetailSheet`. Added `getAudioUrl` to `VoiceRepository` to construct public URLs for `audio-notes` bucket.
Fix status: Done
Review status: Resolved

19.
Bug description: Login screen design is screwed up.
Reported by: User
Fix: Redesigned the `AuthScreen.kt` to match the provided mockup. Implemented `HiveCream` radial background, custom logo, generated user ID card, and separate anonymous login flow in `AuthViewModel` with `generatedId` and `createAnonymousAccount`.
Fix status: Done
Review status: Resolved

20.
Bug description: I should be able to see topics from anywhere on the map, not just nearby topics.
Reported by: User
Fix: Implemented `get_topics_in_bounds` RPC in Supabase (PostGIS). Updated `TopicRepository` and `TopicRepositoryImpl` to query topics within min/max lat/lng bounds. Modified `HiveViewModel` to handle `onCameraIdle` events and fetch topics for the current viewport. Updated `HiveScreen.kt` with `MapEffect` to detect camera idle state and trigger fetching.
Fix status: Done
Review status: Resolved

21.
Bug description: Create a third tab called “Local Hive” and just show all topics as circles of varying sizes depending on how popular that topic is.
Reported by: User
Fix: Implemented "Local Hive" tab with bubble visualization in `LocalHiveScreen.kt`, using `voiceCount` from `get_popular_topics` RPC. Clicking a bubble opens `TopicDeepDiveScreen.kt`, which features a mocked AI audio summary with transcript and playback controls. Updated `HiveApp.kt` navigation.
Fix status: Done
Review status: Resolved

22.
Bug description: `PostgrestRestException: invalid input syntax for type uuid: "demo_3"`. App crashes when viewing or contributing to dummy topics.
Reported by: User
Fix: Updated dummy topic IDs in `HiveViewModel` and `LocalHiveViewModel` to use valid UUID strings instead of arbitrary strings like "demo_1". Updated `TopicDeepDiveScreen` to map these new IDs.
Fix status: Done
Review status: Resolved

23.
Bug description: `PostgrestRestException: Could not find the function public.get_topics_in_bounds(max_lat, max_long, min_lat, min_long)`. The RPC function found in schema cache likely doesn't match the Double (float8) parameters sent by the client.
Reported by: User
Fix: Recreated `supabase/migrations/20240216_get_topics_in_bounds.sql` with explicit `float8` types for all parameters to ensure strict matching with Kotlin's `Double` (which maps to 64-bit float).
Fix status: SQL Updated (Requires Execution)
Review status: Resolved

24.
Bug description: Login screen shows static "SilentHawk15" username instead of the generated one. User also requested auto-login if authenticated within the last 30 days by clicking the ID card.
Reported by: User
Fix: Created `PreferenceManager` to store `last_generated_id` and `last_login_timestamp`. Updated `AuthViewModel` to load the stored ID on launch and enable `autoLogin` if within the 30-day window. Modified `AuthScreen` to make the ID card clickable, triggering `attemptAutoLogin` and showing "WELCOME BACK" for returning users.
Fix status: Done
Review status: Resolved

25.
Bug description: "Local Hive" tab is missing from the app navigation bar.
Reported by: User
Fix: Added `NavigationBarItem` for "Local Hive" in `HiveApp.kt` pointing to the `local_hive` route. Using `BubbleChart` icon.
Fix status: Done
Review status: Resolved

26.
Bug description: Users requested that the username not be shown on the login screen if there is no active session (i.e., for new users or after clearing data). It should only be shown for returning users to allow one-click login.
Reported by: User
Fix: Updated `AuthScreen.kt` to conditionally display the User ID Card only if `canAutoLogin` is true (which is determined by `PreferenceManager` checking for a valid ID and recent login timestamp). New users will only see "Create Account" and "Sign In" options.
Fix status: Done
Review status: Resolved

27.
Bug description: Clicking on the "Welcome Back" username to auto-login fails, and the displayed username is often different from the one actually logged in.
Reported by: User
Fix: In `AuthViewModel`, removed `preferenceManager.saveLastGeneratedId(newId)` from `generateNewId()`.  Moved this call to `performAuth`'s `onSuccess` block. This prevents temporary/candidate IDs from overwriting the valid stored ID of the last successfully logged-in user.
Fix status: Done
Review status: Resolved

28.
Bug description: After recording a voice note, the "Saved & Uploaded!" screen persists indefinitely instead of redirecting back to the previous screen.
Reported by: User
Fix: In `RecorderScreen.kt`, added a `LaunchedEffect` that observes `isUploading`, `recordedFile`, and `uploadError`. When the upload is complete (isUploading=false, file!=null, error=null), the effect waits for 2 seconds (for smooth UX) before triggering the navigation callback `onRecordingSaved`.
Fix status: Done
Review status: Resolved

29.
Bug description: New voice records are not appearing in the Feed list view after recording. Redirect happens, but list is stale.
Reported by: User
Fix: Added a `refresh()` method to `TimelineViewModel` that re-launches the flow collection (cancelling any previous job). Updated `TimelineScreen` to call `viewModel.refresh()` inside a `LaunchedEffect(Unit)`, ensuring the list is re-fetched every time the user navigates back to the feed.
Fix status: Done
Review status: Resolved

30.
Bug description: POST request to /functions/v1/transcribe-audio failed with `Fail to prepare request body for sending`. Ktor client lacked `Content-Type` for `JsonObject`.
Reported by: User
Fix: In `SupabaseModule.kt`, configured the `device-less` Ktor client engine to install `ContentNegotiation` with `kotlinx-serialization-json`. Added `ktor-client-content-negotiation` and `ktor-serialization-kotlinx-json` dependencies in `libs.versions.toml` and `build.gradle.kts` to support automatic JSON serialization of `JsonObject` bodies.
Fix status: Done
Review status: Resolved

31.
Bug description: Voice summaries in "Local Hive" tab have no audio when play button is clicked.
Reported by: User
Fix: In `TopicDeepDiveScreen.kt`, the play button was only toggling a visual state without triggering actual audio playback. Updated `LocalHiveViewModel` to inject `AudioPlayer` and added `toggleAudio(url)` method. Connect the screen's play button to this method.
Fix status: Done
Review status: Resolved

32.
Bug description: "Tap to Speak" button in "Local Hive" tab (Topic Deep Dive) does not work.
Reported by: User
Fix: In `TopicDeepDiveScreen.kt`, added `onSpeakClick` callback to the composable and invoked it from the button's `onClick`. Updated `HiveApp.kt` to pass a navigation lambda that navigates to the `record` route with the `topicId`.
Fix status: Done
Review status: Resolved

33.
Bug description: `PostgrestRestException: Could not find the function public.get_popular_topics`. The user reported that the function was not found even after running the script. This likely indicates permission issues (PostgREST treats missing permissions as 404) or a signature mismatch.
Reported by: User
Fix: Updated `supabase/migrations/20240216_get_popular_topics.sql` to explicitly `DROP` the function before creating it (to clear any old signatures) and added `GRANT EXECUTE` permission for `anon` and `authenticated` roles. This ensures the function is accessible to the app.
Fix status: Verified (RPC Accessible)
Review status: Resolved

34.
Bug description: `404 Not Found` when calling `transcribe-audio` Edge Function. The function exists in the codebase but is not deployed to the remote Supabase project.
Reported by: User
Fix: The user must deploy the function using the Supabase CLI and set the required `GEMINI_API_KEY` secret.
Fix status: Verified (Function Deployed)
Review status: Resolved