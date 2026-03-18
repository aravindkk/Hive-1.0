# Hive — Implementation Plan
## Current State → PRD v1.1 + Designs

**Reference:** PRD v1.1 (March 2026), 5 design screens, existing codebase (ARCHITECTURE.md)
**Target:** Alpha in ~10 weeks, Closed Beta by week 14

---

## Gap Summary

### What exists today (as of 2026-03-18)
- Anonymous auth with 2-syllable username system (@sunkite, @dewleaf) ✅
- Recording → Supabase Storage → Gemini transcription + classification ✅
- Offline queue via Room DB + WorkManager (HiltWorker) ✅
- Bubble chart Topics screen with real data, radial packing, 3 tabs ✅
- My Thoughts (timeline) with weekly reflection activity chart ✅
- Topic Deep Dive with AI summary (Gemini narrative + Indian TTS) ✅
- Attributed transcript view with 2-letter initials synced to audio ✅
- Color aura recording UI (green → orange → red) ✅
- Session persistence across launches (SettingsSessionManager) ✅
- Shimmer loading states on all screens ✅
- New and My Topics list views on Topics tab ✅

### What the PRD + designs still require
- Streaming transcription (word-by-word, < 2s) — deferred Phase 2
- pgvector topic deduplication — deferred Phase 2
- H3 area indexing — deferred Phase 2
- Consent gate (ambiguous classification) — deferred Phase 2
- Resonance mechanism (milestone animations) — deferred Phase 2
- Localized recording prompts — deferred Phase 2
- Google Sign-In — Phase 3
- Push notifications — Phase 3
- AI mood detection + weekly AI reflection — Phase 3
- Content moderation pipeline — Phase 2
- Map exploration screen (F8) — Phase 3

---

## Navigation

**Implemented:**
```
Bottom Nav: [My Thoughts] [Topics]
Floating mic FAB (My Thoughts tab only)

My Thoughts → Personal journal + weekly reflection activity chart + voice history
Topics       → Bubble view (Trending tab) + List view (New tab) + List view (My Topics tab)
               → tap → Topic Detail (AI summary + attributed transcript + community voices)
Recording    → Full-screen modal with aura + waveform
```

---

## Phase 1 — Alpha (Weeks 1–10)
**Goal:** Recording + personal feed working end-to-end. Internal testing only.

### 1A. Backend — Schema & Pipeline

**1. H3 area system** ⏳ Deferred Phase 2
- Add `h3_index` column to `topics` and `voices` tables
- Replace `get_popular_topics` with H3-scoped RPCs
- Currently using PostGIS bounding-box queries (works fine for alpha)

**2. pgvector for deduplication** ⏳ Deferred Phase 2
- Enable pgvector; add `embedding vector(1536)` to `topics` + `voices`
- Currently using Gemini fuzzy matching (~70–80% accuracy, acceptable for testing)

**3. Voices table schema** ✅ DONE
```sql
ALTER TABLE voices ADD COLUMN classification TEXT;
ALTER TABLE voices ADD COLUMN username TEXT;  -- stored at insert time from auth metadata (migration applied)
-- topic_id, transcript already exist
```

**4. Topics table schema** ✅ DONE (via RPCs)
- `voice_count` computed via COUNT JOIN in `get_popular_topics` and `get_topic_by_id` RPCs
- No stored counter column — reflects reality automatically

**5. Streaming transcription** ⏳ Deferred Phase 2
- Current: batch transcription via `transcribe-audio` edge function (10–20s latency, acceptable)

**6. Voice summary generation** ✅ DONE
- `generate-topic-summary` edge function deployed
- Uses `EdgeRuntime.waitUntil()` — HTTP 202 immediately, Gemini + TTS in background
- Triggered by `transcribe-audio` after each community voice is linked
- Android subscribes via Realtime on `topic_summaries`; spinner until ready
- Segments stored as JSONB with `attributed_to: [username]` (not UUID)
- Timing estimated at 150 WPM; attributed transcript synced by `AudioPlayer.currentPositionMs`
- Username stored directly on `voices` row (avoids join complexity with auth schema)

**7. Contributor count + resonance** ⏳ Deferred Phase 2
- DB trigger + milestone events table — not yet built

---

### 1B. App — Core Infrastructure

**8. Navigation restructure** ✅ DONE
- 2-tab bottom nav: My Thoughts + Topics
- Floating mic FAB only on My Thoughts tab
- `topic_deep_dive/{topicId}` route wired
- `record?topicId={topicId}&topicTitle={topicTitle}` route with URL-encoded title

**9. H3 in the app** ⏳ Deferred Phase 2
- Currently using Geocoder reverse-geocoding for area name display
- PostGIS queries used for topic scoping (good enough for alpha)

**10. Room DB offline queue** ✅ DONE
- `PendingVoiceUpload` entity: `id, filePath, topicId, lat, lng, status`
- `UploadWorker` (HiltWorker + WorkManager): processes queue when CONNECTED
- Status: PENDING → UPLOADING → PROCESSING → COMPLETE / FAILED
- `HiveApplication` implements `Configuration.Provider` for HiltWorkerFactory
- WorkManager auto-initializer disabled in `AndroidManifest.xml`

**11. Username system** ✅ DONE
- 2-syllable handles from 40×40 word pools (@sunkite, @dewleaf, @stardrift)
- Stored in Supabase auth metadata + `PreferenceManager`
- Auto-login if last login within 30 days
- Username written to `voices.username` column at insert time

**12. Session persistence** ✅ DONE
- `SettingsSessionManager(Settings())` configured on `Auth` plugin in `SupabaseModule`
- Session saved to SharedPreferences via `multiplatform-settings-no-arg` (transitive dep)
- `isUserLoggedIn()` calls `sessionStatus.first { it !is Initializing }` before checking session
- Logged-in users skip splash and auth screens entirely on subsequent launches
- Plain cream background shown during ~100ms auth check (no splash flash)

---

### 1C. Recording Screen ✅ DONE

**13. RecordingScreen**
- Full-screen modal with screen-edge aura glow (4 gradient strips via `drawBehind`)
- Real amplitude waveform from `MediaRecorder.maxAmplitude` sampled every 50ms (30 bars)
- Aura: 0–99s green → 100–109s orange → 110s+ red (`animateColorAsState` 1500ms tween)
- Auto-stop at 120s
- Discard + Finish buttons; SavedCard auto-dismisses after 1.5s
- Topic title shown as pill when recording into a specific topic
- "Add your voice" from topic skips classification entirely (passes `topic_id` through full chain)

**14. RecorderViewModel**
- `AuraColor` enum + StateFlow derived from `recordingSeconds`
- `_amplitudeBars: List<Float>(30)` sampled every 50ms, normalized 0–1
- Location injected via `LocationRepository.getLastLocation()` → passes `lat`/`lng` to edge function
- `saveToQueue()`: inserts `PendingVoiceUpload` to Room → enqueues WorkManager job
- `isSaving` / `isSaved` StateFlows (replaces older `isUploading`/`uploadResult`)

---

### 1D. My Thoughts Screen ✅ DONE

**15. TimelineScreen**
- Header: real area name from Geocoder + `.statusBarsPadding()`
- Weekly Reflection card: 7-day activity bar chart, peak day highlighted, sequential playlist play
- Voice History: `VoiceNoteCard` list with play/pause, waveform bars, transcript excerpt
- Community chip → navigates to `topic_deep_dive/{topicId}`
- Shimmer loading state while `isLoading = true`
- Empty state only shown after first data emission (distinguishes loading from empty)

**16. TimelineViewModel**
- `isLoading = MutableStateFlow(true)` — set false on first `getMyVoiceNotes()` emission
- `isWeeklyReflectionPlaying` derived from `audioPlayer.playingUrl`
- `toggleWeeklyReflection()` queues last 5 clips as playlist

---

### 1E. Topics Screen ✅ DONE

**17. LocalHiveScreen**
- Tab 0 (Trending): bubble view, radial packing by `voiceCount`, pastel colors, category icons
- Tab 1 (New): scrollable list sorted by `createdAt` DESC, relative timestamps ("2d ago")
- Tab 2 (My Topics): scrollable list filtered to user's contributed topics, "Contributed" badge
- Bubble canvas: `BoxWithConstraints` for viewport size → correct center-scroll on open
- Auto-scrolls to center of canvas (not corner) using `(canvasWidth - viewportWidth) / 2`
- Horizontal + vertical scroll; no topic limit (all topics shown)
- Shimmer: BubbleShimmer for tab 0, TopicListShimmer (6 card skeletons) for tabs 1 & 2
- Mini-player bar when audio playing

**18. LocalHiveViewModel**
- Tab data via `loadTopicsForTab(index)`:
  - Tab 0: `getPopularTopics()` RPC (voice_count DESC)
  - Tab 1: same RPC, client-side sort by `createdAt` DESC
  - Tab 2: RPC result filtered to topic IDs from user's voices
- `isLoading` reset to true on tab switch, false on first emission

---

### 1F. Topic Detail Screen ✅ DONE

**19. TopicDeepDiveScreen**
- AI summary card with play button → ExoPlayer streams TTS audio
- Auto-triggers `generate-topic-summary` when no summary exists; shows spinner
- Attributed transcript: 2-letter initials per segment (CamelCase extraction: SilentFox → SF)
- Community voices list: initials avatar, transcript, waveform, play button
- "Add your voice" → RecorderScreen with `topicId` + `topicTitle` pre-set; skips classification
- Shimmer while `isLoading = true`
- Refresh icon to manually re-trigger summary

**20. TopicDeepDiveViewModel**
- `loadForTopic(id)` guarded with `loadedTopicId` check (fixes Hilt ViewModel reuse / "always Bellandur" bug)
- Realtime subscription on `topic_summaries` for live summary delivery
- `summaryGenerating` guards duplicate trigger calls

---

### 1G. Auth & Session UX ✅ DONE

- Splash screen on first install only (`hasSeenSplash` preference gate)
- Logged-in users: plain cream screen for ~100ms auth check → home with shimmer (no splash)
- `isLoggedIn` checked before `hasSeenSplash` in `startDest` logic
- Permission dialog (RECORD_AUDIO + LOCATION) shown once on first install
- Location settings check **removed** from startup (was causing freeze by firing two system dialogs simultaneously); permissions work fine without forcing GPS mode on

---

### 1H. Audio Playback ✅ DONE

- `AudioPlayer` singleton (ExoPlayer/Media3):
  - `play(url)` — single clip
  - `playPlaylist(urls, id)` — sequential playlist with sentinel `playingUrl` for state tracking
  - `currentPositionMs: StateFlow<Long>` polled every 100ms for attributed transcript sync
  - Auto-stop + release on error and `STATE_ENDED`

---

### 1I. UX Polish ✅ DONE

- **Shimmer loading states** on all screens: Timeline (header + reflection card + 3 voice card skeletons), Topics (bubble ghost circles + list card skeletons), Topic Detail (full content shimmer)
- **Settings icon removed** from all screen headers (no functionality yet)
- **Initials from usernames**: `initialsFrom()` extracts uppercase letters from CamelCase (SilentFox → SF, ElectricStar → ES)
- **Username stored on voices row** at insert time — avoids PostgREST join issues with auth schema
- **Topics center-scroll fix**: uses `BoxWithConstraints` to get real viewport size for correct offset calculation
- **New & My Topics list views** with `TopicListCard`, relative timestamps, Contributed badge

---

## Phase 2 — Closed Beta (Weeks 11–14)
**Goal:** 50 invited users in Koramangala. Full community loop working.

### 2A. Backend

**21. Content moderation edge function** ⏳
- `moderate-content`: LLM classifier for hate speech, PII, spam
- Flagged voices: set `status = 'moderation_flagged'`, block from community feed

**22. Resonance & milestones** ⏳
- DB trigger on voice INSERT → increment `topics.contributor_count`
- Milestone table + RPC for unread milestones per user

**23. Streaming transcription** ⏳
- Replace batch `transcribe-audio` with SSE or Supabase Realtime partial-transcript push

**24. pgvector deduplication** ⏳
- Enable pgvector; `find_similar_topic(embedding, h3_index, threshold)` RPC
- Replace Gemini fuzzy matching with embedding similarity

**25. H3 area system** ⏳
- `h3_index` on topics + voices; H3-scoped RPCs
- `H3Utils` in Android app; `PreferenceManager` caches user's H3 index

**26. Localized prompts** ⏳
- `prompts` table seeded with Bangalore-specific prompts per H3 index
- `get_prompt_for_area(h3_index)` RPC; RecorderViewModel fetches on open

### 2B. App

**27. Consent gate UI** ⏳
- Bottom sheet after AMBIGUOUS/COMMUNITY classification: "Add to Hive? Yes / Keep Private"

**28. Resonance UI** ⏳
- Post-recording overlay: "142 people in your area are talking about this"
- Milestone animation: confetti/pulse at 10, 50, 100, 500 voices

**29. Moderation feedback UI** ⏳
- Flagged clips in My Thoughts: subtle indicator + bottom sheet explanation

---

## Phase 3 — Public Beta (Weeks 15–18)
**Goal:** Open launch in 3 Bangalore neighbourhoods.

**30. Google Sign-In** ⏳ (Credential Manager API + Supabase GoTrue Google OAuth)

**31. Push notifications** ⏳ (FCM: trending, resonance milestones, weekly reflection, re-engagement)

**32. AI Journaling** ⏳
- `generate-weekly-reflection` scheduled edge function
- Mood detection in `process-voice-clip`
- Mood chip filter in My Thoughts; weekly AI narrative card

**33. Map Exploration (F8)** ⏳
- Full-screen Google Map; tap area → fetch topics by H3 → bubble overlay

---

## Phase 4 — V1 Stable (Weeks 19–24)

**34. Performance** ⏳ — paginate voices, debounce summary generation, cache H3 lookups
**35. Analytics** ⏳ — PostHog/Mixpanel; key funnel events
**36. Moderation dashboard** ⏳ — lightweight web app, queue view, approve/reject
**37. Referral system** ⏳ — "Invite a neighbour" share sheet after resonance moment

---

## Risk Flags

| Risk | Mitigation |
|---|---|
| Streaming transcription latency | Supabase Realtime partial-transcript push; 5s polling fallback |
| pgvector deduplication accuracy | Tune threshold (0.82) against real clips in alpha; manual merge in moderation |
| Bubble physics performance | `infiniteTransition` for idle only; tap is instant navigation |
| TTS cost | Cache aggressively; regenerate only on 5+ new clips or 10min debounce |
| H3 library on Android | `h3-java` is JVM-compatible; fallback to server-side computation |
| Classification false positives | Consent gate before publishing; ambiguous defaults to personal |
| Auth join complexity | Username stored directly on `voices` row — no cross-schema join needed |

---

## Known Outstanding SQL

None — all required migrations have been applied.
