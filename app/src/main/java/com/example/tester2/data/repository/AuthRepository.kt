package com.example.tester2.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import kotlin.random.Random

interface AuthRepository {
    suspend fun signUp(email: String, password: String, username: String? = null): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut()
    suspend fun isUserLoggedIn(): Boolean
    suspend fun getCurrentUserId(): String?
    suspend fun getCurrentUsername(): String?
}

class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : AuthRepository {

    override suspend fun signUp(email: String, password: String, username: String?): Result<Unit> {
        return try {
            val finalUsername = username ?: generateUniqueUsername()
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = JsonObject(
                    mapOf("username" to JsonPrimitive(finalUsername))
                )
            }
            // Note: A database trigger is expected to handle the creation of the public.users row
            // using the metadata provided here.
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signIn(email: String, password: String): Result<Unit> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signOut() {
        supabase.auth.signOut()
    }

    override suspend fun isUserLoggedIn(): Boolean {
        // Wait for Auth to finish loading the persisted session from SharedPreferences
        supabase.auth.sessionStatus.first { it !is SessionStatus.Initializing }
        return supabase.auth.currentSessionOrNull() != null
    }

    override suspend fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    override suspend fun getCurrentUsername(): String? {
        val metadata = supabase.auth.currentUserOrNull()?.userMetadata
        return metadata?.get("username")?.toString()?.removeSurrounding("\"")
    }

    private suspend fun generateUniqueUsername(): String {
        var username = ""
        var isUnique = false
        var attempts = 0
        
        while (!isUnique && attempts < 5) {
            username = generateRandomUsername()
            // Check uniqueness in public.users table
            try {
                // Using count to check for existence
                val count = supabase.from("users").select(columns = Columns.list("username")) {
                    filter {
                        eq("username", username)
                    }
                }.countOrNull() ?: 0
                
                if (count == 0L) {
                    isUnique = true
                }
            } catch (e: Exception) {
                // Optimistic fallthrough if check fails
                isUnique = true
            }
            attempts++
        }
        
        if (!isUnique) {
            username = "${generateRandomUsername()}${Random.nextInt(1000, 9999)}"
        }
        return username
    }

    private fun generateRandomUsername(): String {
        val adjectives = listOf(
            "Neon", "Solar", "Cyber", "Lunar", "Sonic", "Rapid", "Cosmic", "Hyper", "Mega", "Ultra",
            "Misty", "Frozen", "Golden", "Silver", "Iron", "Electric", "Dark", "Bright", "Swift", "Brave",
            "Calm", "Wild", "Crimson", "Azure", "Emerald", "Violet", "Mystic", "Arcane", "Prime", "Elite"
        )
        val nouns = listOf(
            "Tiger", "Eagle", "Wolf", "Bear", "Lion", "Hawk", "Falcon", "Shark", "Dragon", "Phoenix",
            "Viper", "Cobra", "Raven", "Owl", "Fox", "Panther", "Panda", "Koala", "Otter", "Seal",
            "Star", "Moon", "Sun", "Comet", "Nebula", "Galaxy", "Orbit", "Pulse", "Wave", "Storm"
        )
        return "${adjectives.random()}${nouns.random()}"
    }

    companion object {
        private val Adjectives = listOf("Brave", "Calm", "Swift", "Happy", "Jolly", "Bright", "Misty", "Silent", "Wild", "Green", "Blue", "Red", "Golden", "Silver", "Hidden", "Lost", "Found", "Neon", "Cyber", "Ancient")
        private val Nouns = listOf("Maple", "River", "Lion", "Star", "Wolf", "Bear", "Eagle", "Hawk", "Fox", "Panda", "Tiger", "Moon", "Sun", "Sky", "Ocean", "Forest", "Mountain", "Valley", "Desert", "City")
    }
}
