# Push Notifications — Implementation Plan

**Scope:** Trending, Weekly Reflection, Re-engagement. No resonance milestones.

---

## Prerequisites (Manual Steps)

1. Create a Firebase project at console.firebase.google.com
2. Add an Android app with package `com.example.tester2`
3. Download `google-services.json` → place at `app/google-services.json`
4. Get the legacy FCM server key from Firebase Console → Cloud Messaging → Server key
5. Store it in Supabase: `supabase secrets set FCM_SERVER_KEY=<your_key>`

---

## Part 1 — Gradle Changes

### `gradle/libs.versions.toml`

Add to `[versions]`:
```toml
firebase-bom = "33.7.0"
```

Add to `[libraries]`:
```toml
firebase-bom      = { group = "com.google.firebase", name = "firebase-bom",           version.ref = "firebase-bom" }
firebase-messaging = { group = "com.google.firebase", name = "firebase-messaging-ktx" }
```

Add to `[plugins]`:
```toml
google-services = { id = "com.google.gms.google-services", version = "4.4.2" }
```

### Root `build.gradle.kts`

Add to the root `plugins {}` block:
```kotlin
alias(libs.plugins.google-services) apply false
```

### `app/build.gradle.kts`

Add to `plugins {}`:
```kotlin
alias(libs.plugins.google-services)
```

Add to `dependencies {}`:
```kotlin
implementation(platform(libs.firebase.bom))
implementation(libs.firebase.messaging)
```

---

## Part 2 — Database Migration

### `supabase/migrations/20260318_fcm_tokens.sql`

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

The `UNIQUE(user_id, token)` constraint enables upsert-on-conflict to refresh `updated_at` on each app launch — this becomes the re-engagement signal (stale `updated_at` = user hasn't opened app).

---

## Part 3 — Android Changes

### 3a. `utils/PreferenceManager.kt` — add FCM token storage

```kotlin
companion object {
    // add alongside existing keys:
    private const val KEY_FCM_TOKEN = "fcm_token"
}

fun saveFcmToken(token: String) = prefs.edit().putString(KEY_FCM_TOKEN, token).apply()
fun getFcmToken(): String? = prefs.getString(KEY_FCM_TOKEN, null)
```

### 3b. New file: `service/HiveFirebaseMessagingService.kt`

```kotlin
package com.example.tester2.service

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.tester2.MainActivity
import com.example.tester2.R
import com.example.tester2.utils.PreferenceManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import javax.inject.Inject

@AndroidEntryPoint
class HiveFirebaseMessagingService : FirebaseMessagingService() {

    @Inject lateinit var preferenceManager: PreferenceManager
    @Inject lateinit var supabase: SupabaseClient

    companion object {
        const val CHANNEL_ID = "hive_notifications"
    }

    override fun onNewToken(token: String) {
        preferenceManager.saveFcmToken(token)
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            upsertToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        val title = data["title"] ?: return
        val body  = data["body"]  ?: return
        showNotification(title, body, data["deep_link"])
    }

    private suspend fun upsertToken(token: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        try {
            supabase.from("fcm_tokens").upsert(
                mapOf("user_id" to userId, "token" to token, "updated_at" to Clock.System.now().toString()),
                onConflict = "user_id,token"
            )
        } catch (e: Exception) {
            Log.e("HiveFCM", "Token upsert failed", e)
        }
    }

    private fun showNotification(title: String, body: String, deepLink: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            deepLink?.let { putExtra("deep_link", it) }
        }
        val pi = PendingIntent.getActivity(
            this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // create 24dp monochrome vector drawable
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }
}
```

**Note:** Create `res/drawable/ic_notification.xml` — a simple 24dp monochrome vector (e.g. hexagon or mic outline).

### 3c. `HiveApplication.kt` — add notification channel + FCM token fetch

```kotlin
import android.app.NotificationChannel
import android.app.NotificationManager
import com.example.tester2.service.HiveFirebaseMessagingService
import com.google.firebase.messaging.FirebaseMessaging
import javax.inject.Inject

@HiltAndroidApp
class HiveApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var preferenceManager: PreferenceManager  // add this

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        prefetchFcmToken()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            HiveFirebaseMessagingService.CHANNEL_ID,
            "Hive Notifications",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = "Community activity and neighbourhood updates" }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun prefetchFcmToken() {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            preferenceManager.saveFcmToken(token)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()
}
```

### 3d. `AuthViewModel.kt` — upsert token after login

Inside `performAuth()`, add inside the `result.onSuccess { }` block after the username sync:

```kotlin
// FCM token registration
val fcmToken = preferenceManager.getFcmToken()
if (fcmToken != null) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            supabase.from("fcm_tokens").upsert(
                mapOf(
                    "user_id" to supabase.auth.currentUserOrNull()?.id,
                    "token"   to fcmToken,
                    "updated_at" to kotlinx.datetime.Clock.System.now().toString()
                ),
                onConflict = "user_id,token"
            )
        } catch (e: Exception) {
            Log.e("AuthViewModel", "FCM token upsert failed", e)
        }
    }
}
```

`AuthViewModel` already has `preferenceManager`. Also inject `SupabaseClient` directly:
```kotlin
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val preferenceManager: PreferenceManager,
    private val supabase: SupabaseClient   // add this
) : ViewModel()
```

### 3e. `AndroidManifest.xml` — three additions

**1. Permission (before `<application>`):**
```xml
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

**2. Service (inside `<application>`):**
```xml
<service
    android:name=".service.HiveFirebaseMessagingService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.firebase.MESSAGING_EVENT" />
    </intent-filter>
</service>
```

**3. Deep-link intent filter on `<activity android:name=".MainActivity">`:**
```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="hive" android:host="topic" />
    <data android:scheme="hive" android:host="feed" />
    <data android:scheme="hive" android:host="timeline" />
</intent-filter>
```

### 3f. `MainActivity.kt` — pass deep link to HiveApp

```kotlin
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HiveApp(initialDeepLink = intent.getStringExtra("deep_link"))
        }
    }
}
```

### 3g. `HiveApp.kt` — handle deep link on startup

Add parameter to `HiveApp` and `MainScreen`:
```kotlin
@Composable
fun HiveApp(initialDeepLink: String? = null) { ... }

@Composable
fun MainScreen(initialDeepLink: String? = null) {
    val navController = rememberNavController()
    // After NavHost is set up:
    LaunchedEffect(initialDeepLink) {
        if (initialDeepLink == null) return@LaunchedEffect
        when {
            initialDeepLink.startsWith("hive://topic/") -> {
                val topicId = initialDeepLink.removePrefix("hive://topic/")
                navController.navigate("topic_deep_dive/$topicId")
            }
            initialDeepLink == "hive://timeline" -> navController.navigate("feed")
            initialDeepLink == "hive://feed"     -> navController.navigate("local_hive")
        }
    }
}
```

### 3h. `HiveApp.kt` — request POST_NOTIFICATIONS permission

In `MainScreen`, update the permissions list:
```kotlin
val permissions = buildList {
    add(android.Manifest.permission.RECORD_AUDIO)
    add(android.Manifest.permission.ACCESS_FINE_LOCATION)
    add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        add(android.Manifest.permission.POST_NOTIFICATIONS)
    }
}
val multiplePermissionsState = rememberMultiplePermissionsState(permissions)
```

---

## Part 4 — Supabase Edge Functions

All three functions use a pure `data` payload (no `notification` block) so display is controlled by the Android service in all app states.

**Shared FCM send helper** — inline in each function (or extract to a `_shared/fcm.ts` module):

```typescript
async function sendFcmToUser(
  supabase: SupabaseClient,
  userId: string,
  payload: Record<string, string>
) {
  const { data: tokens } = await supabase
    .from("fcm_tokens")
    .select("token")
    .eq("user_id", userId);
  if (!tokens?.length) return;

  const fcmKey = Deno.env.get("FCM_SERVER_KEY");
  await Promise.allSettled(
    tokens.map((row) =>
      fetch("https://fcm.googleapis.com/fcm/send", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "Authorization": `key=${fcmKey}`,
        },
        body: JSON.stringify({ to: row.token, data: payload, priority: "high" }),
      })
    )
  );
}
```

### 4a. `supabase/functions/send-trending-notification/index.ts`

**Triggered from `transcribe-audio`** (fire-and-forget after voice is linked to a topic).

**Integration in `transcribe-audio/index.ts`** — after fetching `voice_count`, add:
```typescript
const TRENDING_THRESHOLDS = [5, 10, 20];
if (matchedTopicId && TRENDING_THRESHOLDS.includes(voiceCount)) {
  fetch(`${supabaseUrl}/functions/v1/send-trending-notification`, {
    method: "POST",
    headers: { "Content-Type": "application/json", "Authorization": `Bearer ${serviceKey}` },
    body: JSON.stringify({ topic_id: matchedTopicId, voice_count: voiceCount }),
  }).catch(() => {});
}
```

**Edge function:**
```typescript
serve(async (req) => {
  const { topic_id, voice_count } = await req.json();
  const supabase = createClient(SUPABASE_URL, SERVICE_KEY);

  const { data: topic } = await supabase.from("topics").select("title").eq("id", topic_id).single();
  if (!topic) return new Response("not found", { status: 404 });

  // Notify only users who have contributed to this topic
  const { data: voices } = await supabase.from("voices").select("user_id").eq("topic_id", topic_id);
  const userIds = [...new Set(voices?.map((v) => v.user_id) ?? [])];

  const payload = {
    type: "trending",
    title: "Neighbourhood buzz 🔥",
    body: `${voice_count} people are talking about "${topic.title}"`,
    topic_id,
    deep_link: `hive://topic/${topic_id}`,
  };

  for (const userId of userIds) await sendFcmToUser(supabase, userId, payload);
  return new Response("ok");
});
```

### 4b. `supabase/functions/send-weekly-reflection/index.ts`

**Triggered by cron** — Sunday 9:00 AM IST (3:30 UTC).

**Cron schedule migration `supabase/migrations/20260318_schedule_notifications.sql`:**
```sql
-- Weekly reflection: Sunday 09:00 IST = Sunday 03:30 UTC
SELECT cron.schedule(
  'weekly-reflection',
  '30 3 * * 0',
  $$ SELECT net.http_post(
       url := current_setting('app.supabase_url') || '/functions/v1/send-weekly-reflection',
       headers := jsonb_build_object('Authorization', 'Bearer ' || current_setting('app.service_role_key')),
       body := '{}'::jsonb
     ); $$
);

-- Re-engagement: Wednesday 10:00 AM IST = Wednesday 04:30 UTC
SELECT cron.schedule(
  'reengagement-wednesday',
  '30 4 * * 3',
  $$ SELECT net.http_post(
       url := current_setting('app.supabase_url') || '/functions/v1/send-reengagement-notification',
       headers := jsonb_build_object('Authorization', 'Bearer ' || current_setting('app.service_role_key')),
       body := '{}'::jsonb
     ); $$
);
```

**Edge function:**
```typescript
serve(async (req) => {
  const supabase = createClient(SUPABASE_URL, SERVICE_KEY);
  const { data: tokenRows } = await supabase.from("fcm_tokens").select("user_id");
  const userIds = [...new Set(tokenRows?.map((r) => r.user_id) ?? [])];

  const payload = {
    type: "weekly_reflection",
    title: "Your week in Hive",
    body: "Your week in review is ready — see what your neighbourhood was talking about.",
    deep_link: "hive://timeline",
  };

  for (const userId of userIds) await sendFcmToUser(supabase, userId, payload);
  return new Response("ok");
});
```

### 4c. `supabase/functions/send-reengagement-notification/index.ts`

**Re-engagement signal:** `updated_at` on `fcm_tokens` is refreshed on every app launch (via `onNewToken` or login upsert). Users with `updated_at < now() - 3 days` are inactive.

```typescript
serve(async (req) => {
  const supabase = createClient(SUPABASE_URL, SERVICE_KEY);
  const threeDaysAgo = new Date(Date.now() - 3 * 24 * 60 * 60 * 1000).toISOString();

  const { data: staleTokens } = await supabase
    .from("fcm_tokens")
    .select("user_id")
    .lt("updated_at", threeDaysAgo);
  const userIds = [...new Set(staleTokens?.map((r) => r.user_id) ?? [])];
  if (!userIds.length) return new Response("no inactive users");

  // Count new voices in last 7 days (platform-wide for MVP)
  const { count } = await supabase
    .from("voices")
    .select("id", { count: "exact", head: true })
    .gte("created_at", new Date(Date.now() - 7 * 24 * 60 * 60 * 1000).toISOString());

  if (!count) return new Response("no new voices");

  const payload = {
    type: "re_engagement",
    title: "Catch up on your neighbourhood",
    body: `Your neighbourhood has ${count} new voice${count === 1 ? "" : "s"} this week.`,
    deep_link: "hive://feed",
  };

  for (const userId of userIds) await sendFcmToUser(supabase, userId, payload);
  return new Response("ok");
});
```

---

## Implementation Order

1. Firebase Console setup + `google-services.json` ← **blocks everything else**
2. `FCM_SERVER_KEY` secret in Supabase
3. `libs.versions.toml` + `build.gradle.kts` + root `build.gradle.kts`
4. `fcm_tokens` migration → deploy
5. `PreferenceManager` — add `saveFcmToken` / `getFcmToken`
6. `HiveFirebaseMessagingService` + `ic_notification` drawable
7. `AndroidManifest.xml` — permission + service + intent-filter
8. `HiveApplication` — channel creation + token prefetch
9. `AuthViewModel` — inject `SupabaseClient` + upsert token on login
10. `MainActivity` + `HiveApp` — deep link plumbing
11. `send-trending-notification` edge function + `transcribe-audio` hook
12. `send-weekly-reflection` edge function + cron migration
13. `send-reengagement-notification` edge function

---

## Key Decisions

| Decision | Reason |
|---|---|
| Pure `data` payload, no `notification` block | Full display control in foreground, background, and killed states |
| `UNIQUE(user_id, token)` + upsert on every launch | `updated_at` becomes a reliable last-active proxy; no separate tracking table needed |
| Trending notifies only topic contributors | Avoids spam; feels personal ("this topic you spoke into is blowing up") |
| Legacy FCM endpoint (`/fcm/send` + server key) | No OAuth2 service account dance needed in Deno edge functions |
| Cron via `pg_cron` / `net.http_post` | Native Supabase scheduling; no external cron service needed |
