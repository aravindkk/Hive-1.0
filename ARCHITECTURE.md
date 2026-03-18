# Hive — Architecture Document

## Overview

Hive is a location-based voice-note social platform for Android. Users record short audio clips tied to geographic topics, discoverable on a map and by popularity. The backend is fully managed via Supabase (PostgreSQL + PostGIS), with Google Gemini for async transcription.

---

## Stack Summary

| Layer | Technology |
|---|---|
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose (single-activity) |
| DI | Hilt |
| Networking | Ktor 3 (CIO engine) + Kotlinx Serialization |
| Backend | Supabase (Postgrest, Auth, Storage, Realtime, Functions) |
| Database | PostgreSQL + PostGIS (geospatial) |
| Maps | Google Maps Compose + Fused Location Provider |
| Audio | Android MediaRecorder / MediaPlayer |
| Transcription | Supabase Edge Function → Google Gemini 2.5 Flash |
| Image Loading | Coil 3 |

---

## Project Structure

```
Tester2/
├── app/src/main/java/com/example/tester2/
│   ├── HiveApplication.kt          # @HiltAndroidApp entry point
│   ├── MainActivity.kt             # Single activity host
│   ├── ui/
│   │   ├── HiveApp.kt              # NavHost + bottom nav
│   │   ├── auth/                   # Auth screen + ViewModel
│   │   ├── hive/                   # Map screen + ViewModel
│   │   ├── recorder/               # Recorder screen + ViewModel
│   │   ├── timeline/               # Timeline screen + ViewModel
│   │   ├── localhive/              # Bubble chart screen + ViewModel
│   │   └── theme/                  # Color, Type, Theme
│   ├── data/
│   │   ├── model/                  # Topic, VoiceNote
│   │   ├── repository/             # Repository interfaces + implementations
│   │   └── recorder/               # AudioRecorder interface + implementation
│   ├── di/                         # Hilt modules
│   └── utils/                      # AudioPlayer, PreferenceManager
├── supabase/
│   ├── migrations/                 # Schema, RPC functions, RLS policies
│   ├── seeds/                      # Bangalore test data
│   └── functions/
│       └── transcribe-audio/       # Deno edge function (Gemini transcription)
```

---

## Architecture Pattern: MVVM

```
┌─────────────────────────────────┐
│         UI (Composables)        │  ← collectAsState(), events
├─────────────────────────────────┤
│        ViewModels (Hilt)        │  ← StateFlow, coroutines
├─────────────────────────────────┤
│      Repositories (interfaces)  │  ← Flow<T>, suspend functions
├───────────┬─────────────────────┤
│  Supabase │  Android Services   │  ← Postgrest, Auth, Storage, Realtime / Location, MediaRecorder
└───────────┴─────────────────────┘
```

---

## Navigation

Single-activity, Navigation Compose:

```
splash
  └─→ auth (LandingScreen / LoginForm)
        └─→ home (bottom nav)
              ├── feed          → TimelineScreen
              ├── local_hive    → LocalHiveScreen
              ├── hive          → HiveScreen (map)
              └── record        → RecorderScreen (optional topic_id param)
                    └─→ topic_deep_dive/{topicId} → TopicDeepDiveScreen
```

---

## UI Layer

### Screens & ViewModels

#### AuthScreen / AuthViewModel
- Anonymous account creation with auto-generated `AdjectiveAnimal` usernames
- Email/password sign-in and sign-up toggle
- 30-day auto-login via stored credentials in `SharedPreferences`
- Fake email format: `{generatedId}@hive.anonymous`

#### HiveScreen / HiveViewModel
- Google Map with topic markers and radius circles
- `onCameraIdle` fires RPC `get_topics_in_bounds` for current viewport
- Topic tap → bottom sheet with voice notes + playback
- Location updates from `FusedLocationProviderClient`

#### RecorderScreen / RecorderViewModel
- Circular record button, M4A output to app cache dir
- On stop: upload to Storage → insert voice row → invoke transcription edge function
- Optional `topic_id` param to link recording to a topic

#### TimelineScreen / TimelineViewModel
- User's own voice notes with Realtime subscription
- Transcript shown inline once populated by edge function

#### LocalHiveScreen / LocalHiveViewModel
- Bubble chart of popular topics, sized by `voice_count`
- Falls back to hardcoded demo topics if backend returns empty

### Design System

**Brand colors:** HiveGreen (`#4ADE80`), HiveYellow (`#FEF08A`), HivePink (`#FBCFE8`), HiveBlue (`#BFDBFE`), HiveRed (`#F87171`)

Supports light/dark `ColorScheme` and Android 12+ dynamic colors.

---

## Data Layer

### Models

```kotlin
data class Topic(
    val id: String,
    val title: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Int,
    val active: Boolean = true,
    val voiceCount: Long = 0
)

data class VoiceNote(
    val id: String,
    val userId: String,
    val storagePath: String,
    val transcript: String? = null,
    val topicId: String? = null,
    val createdAt: String
)
```

### Repositories

| Repository | Key Operations |
|---|---|
| `AuthRepository` | `signUp`, `signIn`, `signOut`, `isUserLoggedIn`, `getCurrentUserId/Username` |
| `TopicRepository` | `getTopicsInBounds`, `getPopularTopics`, `getNearbyTopics`, `createTopic` |
| `VoiceRepository` | `createVoiceNote`, `getMyVoiceNotes`, `getVoiceNotesForTopic`, `transcribeAudio`, `getAudioUrl` |
| `StorageRepository` | `uploadAudio` |
| `LocationRepository` | `getLocationUpdates(intervalMs)` → `Flow<Location>` |

All repositories are injected as singletons. All return `Flow<T>` or `suspend` functions.

---

## Dependency Injection (Hilt)

| Module | Provides |
|---|---|
| `SupabaseModule` | `SupabaseClient` (singleton, with all plugins) |
| `RepositoryModule` | All repository bindings |
| `AudioModule` | `AndroidAudioRecorder` |
| `LocationModule` | `FusedLocationProviderClient`, `LocationRepository` |
| `StorageModule` | `StorageRepository` |
| `TopicModule` | `TopicRepository` |
| `VoiceModule` | `VoiceRepository` |

`AudioPlayer` and `PreferenceManager` are also Hilt singletons.

---

## Backend

### Database Schema

**`topics` table**
```sql
id          uuid PK
title       text
location    geography(point)   -- PostGIS
radius      int  (default 500)
active      boolean
created_at  timestamptz
```
GIST index on `location` for spatial query performance.

**`voices` table**
```sql
id            uuid PK
user_id       uuid FK → auth.users.id
storage_path  text
transcript    text (nullable, async populated)
topic_id      uuid FK → topics.id (nullable)
created_at    timestamptz
```

**Storage bucket:** `audio-notes` — public read, authenticated write.

### RPC Functions

| Function | Description |
|---|---|
| `create_topic(title, lat, long, radius)` | Inserts topic with ST_Point |
| `get_nearby_topics(lat, long, radius_meters)` | ST_DWithin distance filter |
| `get_topics_in_bounds(min_lat, min_long, max_lat, max_long)` | ST_MakeEnvelope bounding box |
| `get_popular_topics()` | LEFT JOIN voices, ORDER BY voice_count DESC |

### Row-Level Security

- `topics`: public SELECT, authenticated INSERT
- `voices`: public SELECT (playback), authenticated INSERT/UPDATE
- `storage.objects`: public read, per-user write

### Seed Data

20 Bangalore-area topics (HSR Layout, Koramangala, Indiranagar, Jayanagar) for local testing.

---

## Edge Functions

### `transcribe-audio` (TypeScript / Deno)

**Trigger:** Called by `RecorderViewModel` after voice note upload.

**Flow:**
```
RecorderViewModel.transcribeAudio(storagePath)
  → supabase.functions.invoke("transcribe-audio", { storage_path })
  → Download file from Storage
  → Base64-encode
  → Gemini 2.5 Flash API (prompt: "Transcribe the following audio")
  → UPDATE voices SET transcript = ... WHERE storage_path = ...
  → Return transcript
```

**Env vars required:** `SUPABASE_URL`, `SUPABASE_SERVICE_ROLE_KEY`, `GEMINI_API_KEY`

---

## Key Data Flows

### Record → Transcribe → Display

```
User taps Record
  → AudioRecorder.start() writes M4A to cache
  → User taps Stop
  → StorageRepository.uploadAudio() → Supabase Storage
  → VoiceRepository.createVoiceNote() → INSERT voices row
  → VoiceRepository.transcribeAudio() → invoke edge function
      → Gemini processes audio
      → UPDATE voices.transcript
      → Realtime change event fires
  → TimelineViewModel receives update via postgresChangeFlow()
  → TimelineScreen re-renders with transcript
```

### Map Discovery

```
User pans/zooms map
  → HiveScreen.onCameraIdle(LatLngBounds)
  → HiveViewModel.onCameraIdle(bounds)
  → TopicRepository.getTopicsInBounds()
      → RPC get_topics_in_bounds (PostGIS envelope query)
  → _topics StateFlow updated
  → Map markers + radius circles re-rendered
  → User taps marker
  → VoiceRepository.getVoiceNotesForTopic(topicId)
      → Realtime subscription
  → TopicDetailSheet shows voices + play buttons
```

---

## Audio

### Recording
- `AndroidAudioRecorder`: wraps `MediaRecorder`, MPEG-4/AAC, writes to `context.cacheDir`
- File naming: `HIV_REC_yyyyMMdd_HHmmss.m4a`

### Playback
- `AudioPlayer` singleton wraps `MediaPlayer`
- `playingUrl: StateFlow<String?>` tracks currently playing URL
- `prepareAsync()` for network URLs; auto-starts on `onPrepared`

---

## Location

- `FusedLocationProviderClient`, `HIGH_ACCURACY` priority
- `getLocationUpdates(intervalMs)` returns `callbackFlow<Location>`
- Initial: `lastKnownLocation`, then streaming updates every 10s (default)
- Permissions: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION`

---

## Permissions

| Permission | Used For |
|---|---|
| `INTERNET` | All network calls |
| `RECORD_AUDIO` | Voice recording |
| `ACCESS_FINE_LOCATION` | Map centering, topic discovery |
| `ACCESS_COARSE_LOCATION` | Fallback location |

Runtime permissions handled via Accompanist `rememberMultiplePermissionsState()`.

---

## Local Configuration

```properties
# local.properties
SUPABASE_URL=https://<project>.supabase.co
SUPABASE_ANON_KEY=<anon-key>
MAPS_API_KEY=<google-maps-key>
```

Secrets are injected at build time via `BuildConfig` (Supabase) and the Secrets Gradle Plugin (Maps key → `AndroidManifest.xml` metadata).

---

## Build Config

| Setting | Value |
|---|---|
| compileSdk | 35 |
| minSdk | 26 |
| Kotlin | 2.1.10 |
| JVM target | 17 |
| Compose compiler | enabled |
