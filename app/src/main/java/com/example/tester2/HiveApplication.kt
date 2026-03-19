package com.example.tester2

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.tester2.service.HiveFirebaseMessagingService
import com.example.tester2.utils.PreferenceManager
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class HiveApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var preferenceManager: PreferenceManager

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
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
}
