package com.example.tester2.utils

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PreferenceManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("hive_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_LAST_GENERATED_ID = "last_generated_id"
        private const val KEY_LAST_LOGIN_TIMESTAMP = "last_login_timestamp"
    }

    fun saveLastGeneratedId(id: String) {
        prefs.edit().putString(KEY_LAST_GENERATED_ID, id).apply()
    }

    fun getLastGeneratedId(): String? {
        return prefs.getString(KEY_LAST_GENERATED_ID, null)
    }

    fun saveLastLoginTimestamp(timestamp: Long) {
        prefs.edit().putLong(KEY_LAST_LOGIN_TIMESTAMP, timestamp).apply()
    }

    fun getLastLoginTimestamp(): Long {
        return prefs.getLong(KEY_LAST_LOGIN_TIMESTAMP, 0L)
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
