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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject

@Serializable
private data class FcmTokenRow(
    @SerialName("user_id") val userId: String,
    val token: String,
    @SerialName("updated_at") val updatedAt: String
)

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
                listOf(FcmTokenRow(userId, token, Clock.System.now().toString()))
            ) { onConflict = "user_id,token" }
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
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .build()
        NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
    }
}
