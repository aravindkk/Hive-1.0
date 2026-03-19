# Push Notifications — Implementation Plan

**Scope:** Trending, Weekly Reflection, Re-engagement. No resonance milestones.

**Status:** Android + edge function code complete. Pending: DB migration, function deploy, cron setup.

---

## Prerequisites

| Step | Status |
|---|---|
| Create Firebase project + add Android app (`com.example.tester2`) | ✅ DONE |
| `google-services.json` → `app/google-services.json` | ✅ DONE |
| Firebase Console → Project Settings → Service Accounts → Generate New Private Key | ✅ DONE |
| `supabase secrets set --env-file .env.secrets` with `FIREBASE_SERVICE_ACCOUNT=<json>` | ✅ DONE |
| Run `fcm_tokens` migration in Supabase SQL Editor | ⏳ TODO |
| Deploy 3 edge functions | ⏳ TODO |
| Set up cron triggers (see Part 5) | ⏳ TODO |

**Note:** The Legacy FCM API (server key) was shut down June 20, 2024. We use the **HTTP v1 API** with OAuth2 service account authentication. The edge functions generate a short-lived access token by signing a JWT (RS256) with the service account private key via Web Crypto API — no extra Deno libraries needed.

---

## Part 1 — Gradle ✅ DONE

`gradle/libs.versions.toml` — added `firebase-bom = "33.7.0"`, `firebase-messaging-ktx` library, `google-services` plugin.

`build.gradle.kts` (root) — `alias(libs.plugins.google.services) apply false`

`app/build.gradle.kts` — `alias(libs.plugins.google.services)` plugin + Firebase BOM + messaging dependency.

---

## Part 2 — Database Migration ⏳ TODO

Run `supabase/migrations/20260319_fcm_tokens.sql` in the Supabase SQL Editor:

```sql
CREATE TABLE public.fcm_tokens (
  id         uuid        NOT NULL DEFAULT gen_random_uuid(),
  user_id    uuid        NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
  token      text        NOT NULL,
  updated_at timestamptz NOT NULL DEFAULT now(),
  CONSTRAINT fcm_tokens_pkey PRIMARY KEY (id),
  CONSTRAINT fcm_tokens_user_token_unique UNIQUE (user_id, token)
);

CREATE INDEX fcm_tokens_user_id_idx ON public.fcm_tokens(user_id);

ALTER TABLE public.fcm_tokens ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Users manage own tokens"
  ON public.fcm_tokens FOR ALL TO authenticated
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());
```

The `UNIQUE(user_id, token)` constraint enables upsert-on-conflict to refresh `updated_at` on each app launch — stale `updated_at` (> 3 days) is the re-engagement signal.

---

## Part 3 — Android ✅ DONE

### Files changed

- **`utils/PreferenceManager.kt`** — added `saveFcmToken` / `getFcmToken`
- **`service/HiveFirebaseMessagingService.kt`** — new file; handles token registration (`onNewToken`) and data-only notification display (`onMessageReceived`)
- **`res/drawable/ic_notification.xml`** — monochrome bell vector for the notification small icon
- **`HiveApplication.kt`** — creates notification channel on startup; prefetches FCM token via `FirebaseMessaging.getInstance().token`
- **`ui/auth/AuthViewModel.kt`** — injects `SupabaseClient`; upserts FCM token into `fcm_tokens` after successful login
- **`AndroidManifest.xml`** — `POST_NOTIFICATIONS` permission; `HiveFirebaseMessagingService` service declaration; deep-link intent-filter (`hive://topic`, `hive://feed`, `hive://timeline`)
- **`MainActivity.kt`** — passes `intent.getStringExtra("deep_link")` to `HiveApp`
- **`ui/HiveApp.kt`** — `initialDeepLink` param threaded to `MainScreen`; `LaunchedEffect` routes deep links to nav destinations; `POST_NOTIFICATIONS` added to runtime permission request (Android 13+)

### supabase-kt v3.x upsert API note

`upsert` in v3.x takes a `List<T>` where T is `@Serializable`, and conflict options are set via a trailing builder lambda:

```kotlin
@Serializable
private data class FcmTokenRow(
    @SerialName("user_id") val userId: String,
    val token: String,
    @SerialName("updated_at") val updatedAt: String
)

supabase.from("fcm_tokens").upsert(
    listOf(FcmTokenRow(userId, token, Clock.System.now().toString()))
) { onConflict = "user_id,token" }
```

---

## Part 4 — Supabase Edge Functions ⏳ TODO (deploy)

All three functions are written and in `supabase/functions/`. Deploy with:

```bash
supabase functions deploy send-trending-notification
supabase functions deploy send-weekly-reflection
supabase functions deploy send-reengagement-notification
```

### FCM HTTP v1 send pattern (used in all three functions)

Each function calls `getFcmAccessToken()` once per invocation to exchange a signed JWT for an OAuth2 token, then calls the FCM v1 endpoint per device token:

```
POST https://fcm.googleapis.com/v1/projects/{project_id}/messages:send
Authorization: Bearer <access_token>

{ "message": { "token": "<device_token>", "data": { ... }, "android": { "priority": "high" } } }
```

### 4a. `send-trending-notification`

Triggered fire-and-forget from `transcribe-audio` when `voice_count` hits 5, 10, or 20.
Notifies only users who have contributed to that topic (not all users).
Deep link: `hive://topic/{topic_id}`

### 4b. `send-weekly-reflection`

Broadcasts to all users with an FCM token.
Copy: "Your week in review is ready — see what your neighbourhood was talking about."
Deep link: `hive://timeline`

### 4c. `send-reengagement-notification`

Targets users whose `fcm_tokens.updated_at < now() - 3 days` (haven't opened the app in 3+ days).
Only fires if there are new voices in the last 7 days platform-wide.
Copy: "Your neighbourhood has N new voices this week."
Deep link: `hive://feed`

### Hook in `transcribe-audio` ✅ DONE

After the `voice_count` fetch, before the summary trigger:

```typescript
const TRENDING_THRESHOLDS = [5, 10, 20];
if (TRENDING_THRESHOLDS.includes(voiceCount)) {
  fetch(`${supabaseUrl}/functions/v1/send-trending-notification`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "Authorization": `Bearer ${serviceKey}` },
    body: JSON.stringify({ topic_id: matchedTopicId, voice_count: voiceCount }),
  }).catch(() => {});
}
```

---

## Part 5 — Cron Scheduling ⏳ TODO

`pg_cron` is not available on the Supabase free plan. Use **cron-job.org** (free) instead:

1. Get your edge function base URL from Dashboard → Edge Functions (e.g. `https://<ref>.supabase.co/functions/v1/`)
2. Get your `service_role` key from Dashboard → Settings → API
3. Create two jobs on [cron-job.org](https://cron-job.org):

| Function | Schedule | Method | Header |
|---|---|---|---|
| `send-weekly-reflection` | `30 3 * * 0` (Sun 9AM IST) | POST | `Authorization: Bearer <service_role_key>` |
| `send-reengagement-notification` | `30 4 * * 3` (Wed 10AM IST) | POST | `Authorization: Bearer <service_role_key>` |

The migration `supabase/migrations/20260319_schedule_notifications.sql` contains a `pg_cron` version for reference if you upgrade to a paid Supabase plan later.

---

## Key Decisions

| Decision | Reason |
|---|---|
| Pure `data` payload, no `notification` block | Full display control in foreground, background, and killed states |
| `UNIQUE(user_id, token)` + upsert on every launch | `updated_at` becomes a reliable last-active proxy; no separate tracking table needed |
| Trending notifies only topic contributors | Avoids spam; feels personal ("this topic you spoke into is blowing up") |
| FCM HTTP v1 API + service account OAuth2 | Legacy FCM API shut down June 2024; v1 uses short-lived access tokens from a service account JWT (RS256) via Web Crypto API — no extra libraries needed in Deno |
| cron-job.org instead of pg_cron | pg_cron not available on Supabase free plan; cron-job.org is free and zero-maintenance |
