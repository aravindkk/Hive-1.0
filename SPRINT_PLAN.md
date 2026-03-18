# Sprint Plan — Core Loop
> **Status as of 2026-03-18**: All 7 tasks ✅ DONE. Core loop is live end-to-end.

**Goal:** Record a voice note → see it transcribed & classified → linked to a real topic → browse topics with real voice counts → tap a topic and see real community voices.

Latency is fine. Streaming, aura UI, physics bubbles, push notifications — all deferred.

---

## The Loop We're Building

```
Record → Upload → Transcribe + Classify → Auto-link to topic
                                                    ↓
Topics tab (real counts) ← ← ← ← ← ← ← Topic detail (real voices)
```

---

## What Already Works (Don't Touch)

- Recording, file creation, upload to Supabase Storage
- `voices` DB row creation
- Transcription edge function fires and updates `voices.transcript`
- `LocalHiveScreen` bubbles render (with dummy fallback)
- `TopicDeepDiveScreen` route exists in nav graph
- `AudioPlayer`, `VoiceRepository`, `StorageRepository`

---

## Task 1 — DB Migration ✅ DONE

Add one column to `voices`:

```sql
ALTER TABLE voices ADD COLUMN IF NOT EXISTS classification TEXT DEFAULT 'pending';
-- values: 'pending' | 'personal' | 'community'
```

No other schema changes needed. `voices.topic_id` already exists for the link.

---

## Task 2 — Update Edge Function: classify + auto-link ✅ DONE

**File:** `supabase/functions/transcribe-audio/index.ts`

Change the input signature to also accept `user_lat` and `user_lng` (optional, for future). The core change: after transcription, make a second Gemini call to classify and match a topic.

**New flow:**
1. Transcribe audio (same as now)
2. Fetch the top 10 active topics from `topics` table (by `voice_count` or `created_at`)
3. Send transcript + topic list to Gemini:
   > "Given this transcript, does the speaker seem to be talking about a specific neighborhood topic from this list? If yes, return the matching topic's ID. If the content is personal (emotions, work, relationships, private thoughts), return 'personal'. Return JSON: `{ classification: 'community'|'personal', topic_id: '<uuid>'|null }`"
4. Update `voices` row: set `transcript`, `classification`, and `topic_id` (if matched)
5. If `topic_id` matched: increment `topics.voice_count` by 1 (raw SQL update or RPC)
6. Return `{ transcript, classification, topic_id, topic_title }` in the response

**Why this is enough:** No embeddings, no deduplication, no new infrastructure. Gemini does the matching with a simple prompt. Accuracy will be ~70–80% which is fine for internal testing.

---

## Task 3 — VoiceRepository: return classification result ✅ DONE

**File:** `data/repository/VoiceRepository.kt` + `VoiceRepositoryImpl.kt`

Change `transcribeAudio` signature from `Result<Unit>` to `Result<TranscriptionResult>`:

```kotlin
data class TranscriptionResult(
    val transcript: String,
    val classification: String,   // "personal" | "community" | "pending"
    val topicId: String?,
    val topicTitle: String?
)

suspend fun transcribeAudio(storagePath: String): Result<TranscriptionResult>
```

In the impl, parse the full JSON response from the edge function instead of discarding it.

---

## Task 4 — RecorderViewModel: expose result ✅ DONE

**File:** `ui/recorder/RecorderViewModel.kt`

Add two new state fields:

```kotlin
data class UploadResult(
    val classification: String,
    val topicId: String?,
    val topicTitle: String?,
    val voiceCount: Long
)

private val _uploadResult = MutableStateFlow<UploadResult?>(null)
val uploadResult = _uploadResult.asStateFlow()
```

After `transcribeAudio` completes, populate `_uploadResult`. If classification is `community` and `topicId` is non-null, also fetch the current `voice_count` for that topic (one quick `select` on `topics` table).

---

## Task 5 — RecorderScreen: show post-record feedback ✅ DONE

**File:** `ui/recorder/RecorderScreen.kt`

After `isUploading` goes false and `uploadResult` is set, show a result card instead of the record button:

- **Community:** "🎙 Added to [Traffic] — 83 people talking about this." + "Go to topic →" button that navigates to `topic_detail/{topicId}`
- **Personal:** "Saved to your journal." + "Done" button that pops back

This replaces the current "uploading" spinner with meaningful feedback. The latency (10–20s for transcription + classification) is acceptable for now — show a "Processing your thought..." state while waiting.

---

## Task 6 — TopicDeepDiveScreen: real voices ✅ DONE

**File:** `ui/hive/TopicDeepDiveScreen.kt`

Currently shows hardcoded mock data. Replace with:

1. Create a new `TopicDeepDiveViewModel` (or add to `LocalHiveViewModel`):
   - Accepts `topicId` as a parameter
   - Calls `topicRepository.getTopicById(topicId)` → exposes topic title + voice_count
   - Calls `voiceRepository.getVoiceNotesForTopic(topicId)` → exposes live voice list

2. Update the screen:
   - Topic title and voice count from real data (not `when(topicId)` lookup)
   - Replace the mock transcript section with a `LazyColumn` of voice cards:
     - Timestamp (formatted from `createdAt`)
     - Transcript text (or "Transcribing..." if null)
     - Play button using existing `AudioPlayer` + `getAudioUrl(storagePath)`
   - Keep the "Tap to Speak" button wired to recorder with `topicId` pre-set

3. Add `getTopicById(topicId: String): Flow<Topic?>` to `TopicRepository` + impl (simple `select().eq("id", topicId)` query).

---

## Task 7 — LocalHiveScreen: real data + navigation ✅ DONE

**File:** `ui/hive/LocalHiveScreen.kt` + `LocalHiveViewModel.kt`

Two changes:

1. **Remove dummy fallback** in `LocalHiveViewModel.fetchTopics()`. If there are no topics, show an empty state ("No topics yet. Be the first to speak.") instead of fake data.

2. **Wire tap navigation**: the `onClick` in `LocalHiveScreen` already receives a `Topic` — make sure the caller in `HiveApp.kt` navigates to `topic_detail/{topic.id}`. Check the nav graph; if the route is already there, it just needs the correct `topic.id` passed (not the hardcoded dummy UUID).

---

## What We're Explicitly Skipping

| Feature | Why |
|---|---|
| Streaming transcription | Batch is fine, latency is acceptable for testing |
| Color aura recording UI | Current UI works; visual polish comes later |
| Navigation restructure (2 tabs) | 4 tabs still navigable; restructure in next sprint |
| Topic deduplication (pgvector) | Gemini fuzzy matching is good enough for now |
| AI voice summaries / TTS | Not needed to feel the core loop |
| Push notifications | Post-alpha |
| H3 area indexing | PostGIS queries still work |
| Mood detection, weekly reflection | Personal feed enhancement, post-alpha |
| Room DB offline queue | Direct upload is fine for testing |

---

## Build Order

| Step | Task | Status |
|---|---|---|
| 1 | DB migration (add `classification` column) | ✅ DONE |
| 2 | Edge function: classify + auto-link | ✅ DONE |
| 3 | `VoiceRepository`: update return type | ✅ DONE |
| 4 | `RecorderViewModel`: expose `UploadResult` | ✅ DONE |
| 5 | `RecorderScreen`: post-record feedback UI | ✅ DONE |
| 6 | `TopicDeepDiveScreen` + `TopicDeepDiveViewModel`: real data | ✅ DONE |
| 7 | `LocalHiveScreen`: remove dummy fallback, fix nav | ✅ DONE |
