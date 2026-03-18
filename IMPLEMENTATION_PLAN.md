# Hive — Implementation Plan
## Current State → PRD v1.1 + Designs

**Reference:** PRD v1.1 (March 2026), 5 design screens, existing codebase (ARCHITECTURE.md)
**Target:** Alpha in ~10 weeks, Closed Beta by week 14

---

## Gap Summary

### What exists today
- Basic anonymous auth (adjective+animal+number username)
- Recording → Supabase Storage upload → post-upload Gemini transcription (batch, not streaming)
- Google Maps screen with topic markers (HiveScreen)
- Bubble chart screen with dummy positioning (LocalHiveScreen)
- Simple timeline of own voice notes (TimelineScreen)
- Basic Supabase schema: `topics` (PostGIS) + `voices`
- No AI classification, no voice summaries, no resonance, no moderation

### What the PRD + designs require
- 2-syllable username system (@sunkite, @dewleaf)
- Streaming transcription (word-by-word, < 2s)
- AI classification: personal vs community vs ambiguous + consent gate
- Color aura recording UI (green → orange → red, no explicit timer)
- AI voice summaries per topic (LLM-generated narrative + Indian TTS)
- Attributed transcript view (2-letter initials synced to audio)
- Personal journal ("My Thoughts") with mood detection, weekly reflection, activity chart
- Bubble physics topic view keyed to H3 area name (replaces/merges LocalHive + Map tabs)
- Topic deduplication via pgvector embeddings
- Resonance mechanism (contributor count, milestone animations)
- Localized recording prompts (area + time-of-day based)
- Google Sign-In
- Push notifications (trending topics, resonance milestones, weekly reflection)
- Offline queue with Room DB
- AI content moderation pipeline
- Navigation simplified: **2 tabs** — "My Thoughts" and "Topics"

---

## Navigation Redesign

The designs show a fundamentally different nav structure from what's built:

**Current:** 4 tabs (Feed, Local Hive, Hive/Map, Record modal)

**Target:**
```
Bottom Nav: [My Thoughts] [Topics]
Floating mic button (always visible, center/bottom right)

My Thoughts → Personal journal + weekly reflection + voice history
Topics       → Bubble view for user's H3 area (Trending / New / My Topics tabs)
               → tap bubble → Topic Detail (AI summary + attributed transcript)
Recording    → Full-screen modal with aura + waveform + localized prompt
```

The Google Maps exploration screen (F8) is P1 — it can be deferred past alpha. The LocalHiveScreen bubble view becomes the primary Topics tab, replacing the map as the home screen for topics.

---

## Phase 1 — Alpha (Weeks 1–10)
**Goal:** Recording + personal feed working end-to-end. Internal testing only.
**PRD deliverables:** Recording w/ color aura, streaming transcription, classification, personal feed w/ AI summaries, basic community feed.

### 1A. Backend — Schema & Pipeline

**1. H3 area system**
- Add H3 library to Supabase DB (or compute H3 index in the edge pipeline)
- Add `h3_index` column to `topics` table (resolution 7, ~5 km²)
- Add `area_name` column to `topics` (human-readable: "Koramangala 4th Block")
- Add `h3_index` to `voices` table for direct area scoping
- Replace `get_topics_in_bounds` RPC with `get_topics_by_h3(h3_index)` RPC
- Replace `get_popular_topics` with `get_popular_topics_by_h3(h3_index)` with trending/new/mine filter params
- Migration: update existing Bangalore seed data with H3 indices

**2. pgvector for deduplication**
- Enable pgvector extension in Supabase project
- Add `embedding vector(1536)` column to `topics` table
- Add `embedding vector(1536)` column to `voices` table
- Create HNSW index on `topics.embedding` for fast similarity search
- New RPC: `find_similar_topic(embedding, h3_index, threshold)` → returns matching topic or null

**3. Voices table schema additions**
```sql
ALTER TABLE voices ADD COLUMN classification TEXT;       -- 'personal' | 'community' | 'ambiguous'
ALTER TABLE voices ADD COLUMN mood TEXT;                  -- calm | stressed | happy | anxious | frustrated | excited | reflective
ALTER TABLE voices ADD COLUMN duration_seconds INT;
ALTER TABLE voices ADD COLUMN status TEXT DEFAULT 'processing'; -- processing | complete | moderation_flagged
ALTER TABLE voices ADD COLUMN embedding vector(1536);
ALTER TABLE voices ADD COLUMN h3_index TEXT;
```

**4. Topics table schema additions**
```sql
ALTER TABLE topics ADD COLUMN h3_index TEXT;
ALTER TABLE topics ADD COLUMN area_name TEXT;
ALTER TABLE topics ADD COLUMN contributor_count INT DEFAULT 0;
ALTER TABLE topics ADD COLUMN voice_summary_path TEXT;     -- Storage path to generated TTS audio
ALTER TABLE topics ADD COLUMN voice_summary_transcript TEXT;
ALTER TABLE topics ADD COLUMN voice_summary_updated_at TIMESTAMPTZ;
ALTER TABLE topics ADD COLUMN trend_status TEXT DEFAULT 'new'; -- new | rising | stable | fading
ALTER TABLE topics ADD COLUMN embedding vector(1536);
```

**5. Streaming transcription edge function** _(deferred to Phase 2)_
- Replace batch `transcribe-audio` with a streaming WebSocket-capable function OR use chunked HTTP (Server-Sent Events)
- New function: `process-voice-clip`
  - Input: `{ storage_path, h3_index, user_id }`
  - Steps:
    1. Download audio
    2. Stream Whisper transcription → push partial results back to client via Supabase Realtime channel
    3. Generate embedding (OpenAI `text-embedding-3-small` or Gemini equivalent)
    4. Run classification LLM (personal / community / ambiguous)
    5. Run mood detection LLM
    6. Run content moderation check
    7. If COMMUNITY: call `find_similar_topic` → merge or create topic
    8. Update `voices` row with transcript, classification, mood, embedding, status = 'complete'
    9. Trigger voice summary regeneration if COMMUNITY (debounced)
  - Return: `{ classification, mood, topic_id? }`

**6. Voice summary generation edge function** ✅ DONE
- `generate-topic-summary` deployed with `--no-verify-jwt`
- Uses `EdgeRuntime.waitUntil()` — responds HTTP 202 immediately, Gemini + TTS + storage in background
- Auto-triggered by `transcribe-audio` after each community voice is linked
- Android subscribes via Realtime on `topic_summaries` table, shows spinner until ready
- `topic_summaries` schema: `topic_id UNIQUE`, `audio_path`, `duration_seconds`, `segments JSONB`
- Segment timing estimated at 150 WPM; attributed transcript synced by `AudioPlayer.currentPositionMs`
- Supabase client `requestTimeout = 120.seconds` prevents edge function timeout (default 10s was too short)

**7. Contributor count + resonance**
- Add DB trigger: on `voices` INSERT where `classification = 'community'` → increment `topics.contributor_count`
- New RPC: `get_topic_milestones(topic_id)` → returns milestone crossed (10, 50, 100, 500) if any

---

### 1B. App — Core Infrastructure Changes

**8. Navigation restructure** ✅ DONE
- 2-tab bottom nav: "My Thoughts" (feed) + "Topics" (local_hive)
- Custom `Box`-based overlay nav bar (no Material3 NavigationBar), cream `#F9F9F4` background, no elevation
- Floating mic FAB only on "My Thoughts" tab — hidden on Topics (recording initiated inside topic detail)
- `topic_deep_dive/{topicId}` route for TopicDeepDiveScreen
- Removed redundant `TopAppBar` — each screen owns its own styled header with location + settings
- `enableEdgeToEdge()` in MainActivity with proper `WindowInsets` handling:
  - Nav bar: `Column` with 64dp Row + `windowInsetsBottomHeight(WindowInsets.navigationBars)` Spacer; background fills behind system nav
  - Content padded by `64.dp + navigationBars bottom inset`
  - Screens apply `.statusBarsPadding()` on root Column (TimelineScreen, LocalHiveScreen)

**9. H3 in the app**
- Add H3 library dependency (`com.uber.h3core:h3-java`)
- New utility: `H3Utils` — converts lat/lng to H3 index + reverse geocode area name
- Replace all `getTopicsInBounds` calls with `getTopicsByH3`
- Store user's current H3 index in `PreferenceManager`

**10. Room DB for offline queue**
- Add Room dependency
- Entities: `PendingVoiceUpload(id, filePath, h3Index, status, createdAt)`
- DAO: insert, getAll, updateStatus, delete
- `UploadWorker` (WorkManager): processes queue when network available
- Status progression: Saved → Uploading → Awaiting network → Processing → Complete

**11. Username system redesign** ✅ DONE
- `AuthViewModel.generateNewId()` uses two word pools (40 first-syllables × 40 second-syllables)
- Words: sun/dew/star/moon/rain/mist/dawn... + kite/leaf/drift/glow/stone/light/spark...
- Generates handles like @sunkite, @dewleaf, @stardrift on first launch; refreshable by user
- Stored in Supabase user metadata and local `PreferenceManager`

---

### 1C. Recording Screen Redesign (F1)

**12. RecordingScreen** ✅ DONE
- Full-screen modal with screen-edge aura glow (`drawBehind` + 4 gradient strips)
- Real amplitude waveform from `MediaRecorder.maxAmplitude` sampled every 50ms
- Aura: 0–99s green → 100–109s orange → 110s+ red (`animateColorAsState` 1500ms tween)
- Auto-stop at 120s; colored dot + "Recording" label instead of timer text
- "Discard" + "Finish" buttons; community result card shows topic + voice count
- `community_new` classification shows "Shared with your area" (not "saved to journal"); `personal` shows correct private message
- Upload errors shown via `_uploadError` state (no fake classification fallback)

**13. RecorderViewModel changes** ✅ DONE (partial — WorkManager/Room deferred)
- `AuraColor` enum (GREEN/ORANGE/RED) + `_auraColor` StateFlow derived from recording seconds
- `_amplitudeBars: List<Float>(30)` sampled every 50ms, normalized 0..1
- Auto-stop at 120s in timer coroutine
- `areaName` StateFlow from `LocationRepository.getLastLocation()` + Android `Geocoder`
- Injects `LocationRepository`; passes `lat`/`lng` to `transcribeAudio()` for new topic creation
- Direct upload still in ViewModel (Room/WorkManager queue deferred to item 10)
- Audio quality: `setAudioSamplingRate(44100)`, `setAudioEncodingBitRate(128_000)`, `setAudioChannels(1)`

---

### 1D. My Thoughts Screen (replaces TimelineScreen)

**14. MyThoughtsScreen** ✅ DONE (mood dot + duration + topic chips deferred to Phase 2)
- Header: real area name from `Geocoder` + settings gear; `.statusBarsPadding()` on root Column
- Weekly Reflection card: 7-day activity bar chart (peak day highlighted in green), insight text
- Weekly Reflection **play button**: plays last 5 voice recordings as a sequential playlist via `AudioPlayer.playPlaylist()`; toggles Play/Pause icon based on `isWeeklyReflectionPlaying` state
- Voice History section: count label + `VoiceNoteCard` per clip
- Each card: timestamp ("TODAY • 9:45 AM"), transcript excerpt, waveform bars, **circular play/pause button** (green when playing)
- Community chip → taps through to `topic_deep_dive/{topicId}`; chip shown for `classification == "community"` clips only
- `VoiceNote` model includes embedded `topics: TopicRef?` join (`*, topics(title)` Supabase query)

**15. MyThoughtsViewModel** ✅ DONE (mood filtering deferred to Phase 2)
- `voiceNotes` StateFlow from `VoiceRepository.getMyVoiceNotes()` with Realtime subscription
- `areaName` from `LocationRepository.getLastLocation()` + `Geocoder`
- `playingUrl` from `AudioPlayer` for play/pause state
- `isWeeklyReflectionPlaying` StateFlow derived from `audioPlayer.playingUrl.map { it == WEEKLY_REFLECTION_ID }`
- `toggleWeeklyReflection()` queues last 5 voice notes as playlist
- 7-day activity counts computed in `WeeklyReflectionCard` from `createdAt` timestamps

---

### 1E. Topics Screen (replaces LocalHiveScreen)

**16. TopicsScreen** ✅ DONE (partial — idle animation deferred)
- Header: location pin + real area name from `Geocoder` reverse-geocoding device GPS; `.statusBarsPadding()` on root Column
- Tab row: Trending | New | My Topics (all three tabs wired and loading data)
- Bubble layout: radial packing algorithm, sized by `voiceCount`, category icons, pastel colors
- Mini-player bar when audio playing ("Listening to [Topic]" + dismiss X)
- Area name shown as "Your area" fallback while location resolves
- No FAB on this screen — recording happens inside topic detail via "Add your voice" button

**17. TopicsViewModel** ✅ DONE (H3 replacement deferred to item 9)
- Tab 0 → `getPopularTopics()` (voice_count DESC), Tab 1 → `getNewTopics()` (created_at DESC)
- Tab 2 → `getMyTopics(userId)` (user's contributed topics via their voices)
- `areaName` StateFlow resolved from device GPS via `Geocoder`

---

### 1F. Topic Detail Screen

**18. TopicDetailScreen** ✅ DONE (attributed transcript highlight deferred)
- AI summary card with play button → ExoPlayer streams TTS audio from Supabase Storage
- Auto-triggers `generate-topic-summary` when no summary exists; shows spinner while generating
- "Generating audio…" state when summary text exists but audio not yet ready
- Community voices list with username initials, transcript, waveform, duration
- "Add your voice" button → RecordingScreen with `topic_id` pre-set
- Refresh icon to manually re-trigger summary generation

**19. TopicDetailViewModel** ✅ DONE (attributed transcript sync deferred)
- Realtime subscription on `topic_summaries` for live delivery when summary finishes
- `summaryGenerating` state guards duplicate trigger calls
- `loadForTopic(id)` called from `LaunchedEffect(topicId)` to fix Hilt ViewModel reuse bug

---

### 1G. Auth & Session UX ✅ DONE

- **Splash screen first-install only**: `PreferenceManager.hasSeenSplash()` / `markSplashSeen()` gate. Splash only renders when flag is false; subsequent launches skip it entirely.
- **No auth screen flash for logged-in users**: `HiveApp` waits for `isAuthCheckComplete` (set after `supabase.auth.currentSessionOrNull()` resolves) before choosing `startDestination`. While waiting, shows `SplashContent()` — no NavHost rendered yet. Logged-in users go directly to `"home"`, never see auth screen.
- **30-day session**: Supabase refresh tokens persist across launches. `isUserLoggedIn()` checks `currentSessionOrNull()` which auto-refreshes expired access tokens using stored refresh token.
- `AuthViewModel` exposes `isAuthCheckComplete: StateFlow<Boolean>` + `hasSeenSplash: Boolean` + `markSplashSeen()`.

---

### 1H. Audio Playback Infrastructure ✅ DONE

- `AudioPlayer` (singleton, ExoPlayer/Media3):
  - `play(url)` — single clip playback
  - `playPlaylist(urls, playlistId)` — sequential playlist via `setMediaItems()`; `_playingUrl` set to `playlistId` sentinel for state tracking
  - `currentPositionMs: StateFlow<Long>` polled every 100ms for attributed transcript sync
  - Proper `stop()` + `release()` on player error and `STATE_ENDED`

---

## Phase 2 — Closed Beta (Weeks 11–14)
**Goal:** 50 invited users in Koramangala. Full community loop working.
**PRD deliverables:** Community voice summaries, attributed transcript, resonance, moderation, localized prompts.

### 2A. Backend

**20. Content moderation edge function**
- New function: `moderate-content`
- Input: `{ transcript, user_id }`
- LLM classifier: hate speech, threats, PII about others, explicit content, spam, ads
- Output: `{ flagged: bool, reason?: string }`
- If flagged: set `voices.status = 'moderation_flagged'`, keep in personal feed, block from community
- Notification to user: "We couldn't share this with your area"

**21. Resonance & milestones**
- DB trigger on `topics.contributor_count` update: check if milestone crossed (10, 50, 100, 500)
- Write milestone events to new `topic_milestones` table: `{ topic_id, milestone, crossed_at }`
- RPC: `get_unread_milestones(user_id)` → milestones on topics user contributed to

**22. Attributed transcript storage**
- `generate-topic-summary` to store attribution map as JSON alongside summary:
  ```json
  { "segments": [{ "text": "...", "contributor_id": "uuid", "initials": "AB", "color": "#..." }] }
  ```
- Store in `topics.voice_summary_transcript` as structured JSON

**23. Localized prompts (initial)**
- New `prompts` table: `{ h3_index, prompt_text, category, day_of_week }`
- Seed with Bangalore-specific prompts (30–40 entries covering Koramangala, HSR, Indiranagar)
- New RPC: `get_prompt_for_area(h3_index)` → returns daily rotating prompt

### 2B. App

**24. Consent gate UI**
- After classification returns AMBIGUOUS or COMMUNITY: show bottom sheet
  - "This sounds like something your neighbors might relate to."
  - "Add it to the Hive?" → Yes / Keep Private
- RecorderViewModel handles classification response + consent state

**25. Resonance UI**
- In TopicsScreen: after recording completes and topic is matched, show overlay:
  - "142 people in your area are talking about this too."
  - Milestone animation: confetti/pulse for 10, 50, 100, 500 milestones
  - "You're the 100th voice on this topic. Your neighborhood is listening."

**26. Moderation feedback UI**
- In MyThoughtsScreen: flagged clips show subtle indicator
- Bottom sheet: "We couldn't share this with your area. [I think this was a mistake →]"
- Mistake report queued to moderation dashboard (Phase 4)

**27. Localized recording prompts**
- RecorderViewModel fetches `get_prompt_for_area(h3Index)` on screen open
- Displayed above waveform, dismissable with single swipe
- Cached locally (1 per day per area)

---

## Phase 3 — Public Beta (Weeks 15–18)
**Goal:** Open launch in 3 Bangalore neighborhoods.
**PRD deliverables:** Map exploration w/ bubbles, journaling features, Google Sign-In, push notifications, contributed topics list.

### 3A. Backend

**28. AI journaling pipeline**
- Mood detection: already computed in `process-voice-clip` (Phase 1)
- Weekly reflection generation: new scheduled edge function `generate-weekly-reflection`
  - Runs every Sunday at 8am IST
  - Per user: fetch last 7 days of clips + moods
  - LLM: generates narrative reflection + emotional arc insight
  - TTS: generates audio clip (15–30 seconds)
  - Stores in new `reflections` table: `{ user_id, week_start, audio_path, text, generated_at }`

**29. Push notification infrastructure**
- Integrate Firebase Cloud Messaging (FCM) in Android app
- Store FCM tokens in Supabase: `user_push_tokens(user_id, fcm_token, updated_at)`
- New Supabase scheduled function (pg_cron or edge function):
  - Trending topic detection: topics with contributor_count growth > threshold → send to area users
  - Resonance milestone: send to contributing users
  - Weekly reflection: Sunday morning
  - Re-engagement prompt: users inactive 3+ days → localized prompt notification
- Frequency cap: max 2 trending notifications/day per user

**30. Contributed topics list**
- New table: `user_topic_contributions(user_id, topic_id, contributed_at)` — populated on clip classification
- RPC: `get_contributed_topics(user_id)` → topic list with contributor_count + last_updated

### 3B. App

**31. Google Sign-In**
- Add Google Sign-In dependency (Credential Manager API, modern approach)
- `AuthRepository.signInWithGoogle(idToken)` → Supabase GoTrue Google OAuth
- Add "Sign in with Google" button to AuthScreen landing
- Update Supabase project OAuth settings (Google provider)

**32. Push notification handling**
- `FirebaseMessagingService` implementation
- `NotificationHelper`: builds notifications for each type (trending, resonance, reflection, re-engagement)
- Deep link handling: trending notification → TopicDetailScreen; reflection → MyThoughtsScreen
- FCM token registration on login, refresh on token rotate

**33. AI Journaling UI in MyThoughtsScreen**
- Weekly Reflection card: already scaffolded in Phase 1, now data-driven from `reflections` table
- Activity chart: real data from 7-day clip history
- Mood chip filter: tap mood tag → filter voice history list
- "Your Hive Contributions" section at bottom of MyThoughtsScreen:
  - List of topics user contributed to
  - Each: topic title, contributor count, last updated, tap → TopicDetailScreen

**34. Map Exploration screen (F8 — P1)**
- Full-screen Google Map
- Tap on area → fetch `get_topics_by_h3(tappedH3)` → show bubble overlay
- Read-only for areas other than user's current H3
- Access via settings or secondary nav entry (not in main bottom nav)

---

## Phase 4 — V1 Stable (Weeks 19–24)
**Goal:** 1,000+ WAC. Performance + operations tooling.

**35. Performance**
- Lazy load voice summary audio (don't preload, stream on tap)
- Debounce `generate-topic-summary` calls more aggressively
- Paginate voice history (cursor-based)
- Cache H3 → area name lookups locally
- Profile Compose recomposition in TopicsScreen (bubble layout expensive)

**36. Analytics**
- Integrate PostHog or Mixpanel (self-hosted or cloud)
- Key events: `clip_recorded`, `clip_classified`, `topic_viewed`, `summary_played`, `resonance_seen`, `notification_tapped`
- Funnel: Record → Submit → See resonance → Listen to topic → Contribute again

**37. Moderation dashboard (P2)**
- Lightweight web app (Next.js or simple HTML + Supabase JS client)
- Queue view: flagged clips with transcript + audio playback + AI reason
- One-tap approve / reject
- Basic stats: flag rate, false positive rate

**38. Referral system**
- "Invite a neighbor" share sheet after resonance moment
- Dynamic link: deep links to TopicsScreen with referrer's H3 pre-set
- "Neighborhood builder" badge stored in user metadata

---

## Risk Flags

| Risk | Mitigation |
|---|---|
| Streaming transcription latency | Use Supabase Realtime channel for partial transcript push; fallback to 5s polling if WebSocket unreliable |
| pgvector deduplication accuracy | Tune threshold (0.82) against real Bangalore clips in alpha; add manual merge option in moderation dashboard |
| Bubble physics performance | Use `infiniteTransition` for subtle idle only; tap-open is instant navigation (no physics simulation needed for MVP) |
| TTS cost | Cache summaries aggressively; regenerate only on 5+ new clips or 10min debounce |
| H3 library on Android | `h3-java` is JVM-compatible; test ProGuard rules; fallback to server-side H3 computation if needed |
| Classification false positives (personal shared publicly) | Always show consent gate before publishing; ambiguous defaults to personal |

---

## Recommended Build Order (Week-by-Week)

| Week | Focus |
|---|---|
| 1–2 | Schema migrations (H3, pgvector, new columns), username redesign, nav restructure (2 tabs) |
| 3–4 | RecordingScreen redesign (aura + waveform), Room DB offline queue, UploadWorker |
| 5–6 | `process-voice-clip` edge function (streaming transcription + classification + embedding) |
| 7–8 | TopicsScreen (bubble layout), TopicDetailScreen scaffold, MyThoughtsScreen redesign |
| 9–10 | `generate-topic-summary` edge function, voice summary playback, attributed transcript |
| 11–12 | Consent gate, resonance UI, moderation pipeline, localized prompts |
| 13–14 | Beta polish: error states, loading skeletons, haptics, closed beta with 50 users |
| 15–16 | Google Sign-In, FCM push notifications, contributed topics list |
| 17–18 | Journaling (weekly reflection, mood trends), map exploration (P1), public beta |
| 19–24 | Performance, analytics, referral system, moderation dashboard |
