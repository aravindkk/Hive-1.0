# Milestones & Resonance — Implementation Plan

**Scope:** Phase 2 features (items 22, 27, 28 from IMPLEMENTATION_PLAN.md)
**Reference:** PRD v1.1 §6.2 (Consent Gate), §7 (Resonance Mechanism), Design screens

---

## What These Features Are

**Resonance** is the "wow" moment immediately after recording: the user sees that real neighbors are already talking about the same thing.
> *"142 people in your area are talking about this too."*

**Milestones** are community momentum markers — special animated moments at contributor thresholds (10 / 50 / 100 / 500).
> *"You're the 100th voice on this topic. Your neighbourhood is listening."*

**Consent Gate** gives the user final say before their clip enters the community feed, triggered when the AI classifies a clip as AMBIGUOUS.
> *"This sounds like something your neighbors might relate to. Add it to the Hive?"*

---

## Current State

- `transcribe-audio` already returns `{ transcript, classification, topic_id, topic_title, voice_count }` to the Android client
- `TranscriptionResult.voiceCount` is already a field on the model — it's just not displayed anywhere
- `SavedCard` shows "Added to the Hive!" but ignores `voiceCount` entirely
- AMBIGUOUS classification currently silently saves to personal — no consent gate shown
- No milestone table or trigger exists in the DB

---

## Part 1 — Backend Changes

### 1a. Add `is_milestone` to `transcribe-audio` response

`voice_count` is already computed in `transcribe-audio` after the voice is linked to a topic. Add a threshold check there and include `is_milestone` in the JSON response.

**In `supabase/functions/transcribe-audio/index.ts`**, after computing `voiceCount`:

```typescript
const MILESTONE_THRESHOLDS = [10, 50, 100, 500];
const isMilestone = MILESTONE_THRESHOLDS.includes(voiceCount);

// In the return payload:
return new Response(JSON.stringify({
  transcript,
  classification,
  topic_id: matchedTopicId,
  topic_title: matchedTopicTitle,
  voice_count: voiceCount,
  is_milestone: isMilestone,          // <-- new field
}), { headers: { "Content-Type": "application/json", ...corsHeaders } });
```

No new DB table needed for Phase 2. The client receives the signal directly. Per-user milestone deduplication (so you don't re-animate the same milestone across devices) is deferred to Phase 3 when push notifications are built.

### 1b. Milestone history table (Phase 3 prep — skip for now)

When push notifications are added, a `milestone_events` table will be needed to avoid re-notifying users about the same threshold. Schema stub for reference:

```sql
-- Deferred to Phase 3
CREATE TABLE public.milestone_events (
  id         uuid        DEFAULT gen_random_uuid() PRIMARY KEY,
  topic_id   uuid        NOT NULL REFERENCES public.topics(id) ON DELETE CASCADE,
  threshold  int         NOT NULL,  -- 10, 50, 100, 500
  reached_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (topic_id, threshold)
);
```

---

## Part 2 — Android: `TranscriptionResult` Model

**File:** `app/src/main/java/com/example/tester2/data/model/TranscriptionResult.kt`

Add `isMilestone`:

```kotlin
@Serializable
data class TranscriptionResult(
    val transcript: String = "",
    val classification: String = "personal",
    @SerialName("topic_id") val topicId: String? = null,
    @SerialName("topic_title") val topicTitle: String? = null,
    @SerialName("voice_count") val voiceCount: Long = 0,
    @SerialName("is_milestone") val isMilestone: Boolean = false,   // <-- new
)
```

---

## Part 3 — Android: Resonance `SavedCard` Redesign

**File:** `app/src/main/java/com/example/tester2/ui/recorder/RecorderScreen.kt`

### 3a. Resonance line (always shown for community clips)

When `resolvedTopicTitle != null` (clip went to community), show the contributor count below the topic name:

```
Added to the Hive!
Your voice has been added to "Koramangala Traffic & Commute".
                                                           ← gap
          142 people are talking about this
```

Display logic:
- `voiceCount > 1` → `"$voiceCount people are talking about this"`
- `voiceCount == 1` → `"You're the first voice on this topic"` (no integer plural issue)

The count comes directly from `transcriptionResult.voiceCount`. No extra network call needed.

### 3b. Milestone overlay

When `transcriptionResult.isMilestone == true`, show a milestone moment on top of the normal SavedCard:

**Layout:** Full `Box` with the normal card content underneath and a milestone banner on top:

```
┌─────────────────────────────────┐
│  ✓ (pulsing green circle)       │
│                                 │
│  Added to the Hive!             │
│  "Koramangala Traffic & Commute"│
│                                 │
│ ┌─────────────────────────────┐ │  ← milestone banner (animated in)
│ │ 🎉  100th voice             │ │
│ │ Your neighbourhood is       │ │
│ │ listening.                  │ │
│ └─────────────────────────────┘ │
│                                 │
│  142 people are talking about   │
│  this                           │
└─────────────────────────────────┘
```

**Animation spec:**
- Banner slides up from bottom with `animateFloatAsState` (300ms spring) on `isMilestone`
- Check circle pulses once using `infiniteTransition` → `animateFloat` scale 1f→1.15f→1f over 600ms, then stops after 2 cycles
- Auto-dismiss timer extended from 2500ms to **4000ms** when milestone is hit (user needs time to read it)

**Milestone message map:**

```kotlin
fun milestoneMessage(voiceCount: Long): String = when {
    voiceCount >= 500 -> "500 voices. This is a movement."
    voiceCount >= 100 -> "You're the ${voiceCount}th voice on this topic.\nYour neighbourhood is listening."
    voiceCount >= 50  -> "50 people and counting. Your area is buzzing."
    voiceCount >= 10  -> "10 voices on this topic. Something's building here."
    else              -> ""
}
```

### 3c. Auto-dismiss timing

```kotlin
LaunchedEffect(Unit) {
    delay(if (transcriptionResult?.isMilestone == true) 4000L else 2500L)
    onDone()
}
```

---

## Part 4 — Android: Consent Gate

**Trigger:** Classification returns `"ambiguous"` from `transcribe-audio`.

Currently ambiguous clips are silently saved as personal. The PRD requires showing a consent bottom sheet.

### 4a. Flow change in `RecorderViewModel`

After `transcribeAudio()` returns:
- If `classification == "community"` or `classification == "personal"` → behave as today
- If `classification == "ambiguous"` → set `_showConsentGate.value = true` (new StateFlow), hold `isSaved = false`

User taps **"Add to Hive"** → call a new `confirmCommunity()` in the ViewModel which re-calls `transcribeAudio` with a forced `community` flag (or simply inserts the voice directly into the matched/new topic using the topic_id already returned).

User taps **"Keep Private"** → call `discardCommunity()` which marks the voice as personal (update `classification` column on the already-inserted `voices` row, or simply set `isSaved = true` with personal state).

### 4b. `RecorderViewModel` state additions

```kotlin
// New StateFlows
private val _showConsentGate = MutableStateFlow(false)
val showConsentGate: StateFlow<Boolean> = _showConsentGate

// Holds the ambiguous result while waiting for user decision
private var pendingAmbiguousResult: TranscriptionResult? = null

fun confirmCommunity() {
    val result = pendingAmbiguousResult ?: return
    _transcriptionResult.value = result.copy(classification = "community")
    _showConsentGate.value = false
    _isSaved.value = true
}

fun keepPrivate() {
    val result = pendingAmbiguousResult ?: return
    _transcriptionResult.value = result.copy(topicTitle = null, topicId = null)
    _showConsentGate.value = false
    _isSaved.value = true
}
```

**Note on DB state:** The voice row is already inserted by `transcribe-audio` before the response arrives. If user taps "Keep Private" after AMBIGUOUS, the topic link (`voices.topic_id`) must be nulled. Add a `discard-community-voice` edge function or a simple Supabase `.update()` call in `keepPrivate()`:

```kotlin
supabase.from("voices")
    .update(mapOf("topic_id" to null, "classification" to "personal"))
    .eq("id", pendingVoiceId)
```

This requires `voice_id` to also be returned by `transcribe-audio`. Add it to the response payload:

```typescript
// In transcribe-audio return:
voice_id: voiceId,   // the UUID of the inserted voices row
```

And `TranscriptionResult`:
```kotlin
@SerialName("voice_id") val voiceId: String? = null,
```

### 4c. `RecorderScreen` — consent bottom sheet

In `RecorderContent`, observe `showConsentGate` and show a `ModalBottomSheet`:

```kotlin
val showConsentGate by viewModel.showConsentGate.collectAsState()

if (showConsentGate) {
    ModalBottomSheet(onDismissRequest = { viewModel.keepPrivate() }) {
        ConsentGateSheet(
            topicTitle = viewModel.pendingAmbiguousResult?.topicTitle,
            voiceCount = viewModel.pendingAmbiguousResult?.voiceCount ?: 0,
            onAddToHive = { viewModel.confirmCommunity() },
            onKeepPrivate = { viewModel.keepPrivate() }
        )
    }
}
```

**`ConsentGateSheet` layout:**

```
┌──────────────────────────────────────────┐
│ ▬▬▬▬                                     │  ← drag handle
│                                          │
│  This sounds like something your         │
│  neighbors might relate to.              │
│                                          │
│  ┌──────────────────────────────────┐    │
│  │ 🏘 "Koramangala Traffic & Commute"│    │
│  │    142 people already talking    │    │
│  └──────────────────────────────────┘    │
│                                          │
│  [ Add to Hive ]   (filled, teal)        │
│  [ Keep Private ]  (outlined)            │
│                                          │
└──────────────────────────────────────────┘
```

If `topicTitle == null` (would create a new topic):
> "This sounds like it could be the start of a neighbourhood conversation. Share it?"

---

## Part 5 — Resonance in Topic Cards

**File:** `app/src/main/java/com/example/tester2/ui/hive/LocalHiveScreen.kt` (bubble labels) and topic list cards.

The bubble view already shows `voiceCount` as the size driver. Make it explicit in the tooltip or bubble label:

- Bubble label: `"$voiceCount voices"` (already approximate — just ensure the count is legible)
- Topic list cards (`TopicListCard`): show `"$voiceCount people talking"` as a secondary line (already shows relative timestamp; add count alongside it)
- Topic deep dive header: `"$voiceCount voices in this topic"` (already shown via `voice_count` from `get_topic_by_id`)

No new network calls required — `voiceCount` is already in the `Topic` model.

---

## Implementation Order

1. **`transcribe-audio`** — add `is_milestone` + `voice_id` to response payload
2. **`TranscriptionResult`** — add `isMilestone`, `voiceId` fields
3. **`SavedCard`** — add resonance line (`voiceCount`) for community clips
4. **`SavedCard`** — add milestone banner + pulse animation + extended auto-dismiss
5. **`RecorderViewModel`** — add `_showConsentGate`, `pendingAmbiguousResult`, `confirmCommunity()`, `keepPrivate()`
6. **`RecorderScreen`** — add `ConsentGateSheet` `ModalBottomSheet`
7. **Topic list cards** — show `voiceCount` as `"N people talking"` line

---

## Key Decisions

| Decision | Reason |
|---|---|
| `is_milestone` computed server-side, not client-side | Server has the authoritative `voice_count` at insert time; avoids off-by-one from concurrent writes |
| No `milestone_events` table in Phase 2 | Per-user deduplication only matters when push notifications are added; local animation is stateless |
| `voice_id` returned by `transcribe-audio` | Required to null the topic link on "Keep Private" without a second lookup |
| Consent gate only for AMBIGUOUS (not all community) | PRD intent: explicit community classification doesn't need consent; ambiguity is the edge case |
| `keepPrivate()` updates DB immediately | Voice is already inserted; we must clean up `topic_id` to prevent ghost community links |
| Auto-dismiss extended to 4s on milestone | Milestone message is longer and emotionally significant — 2.5s is too fast to read |
