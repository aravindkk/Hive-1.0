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