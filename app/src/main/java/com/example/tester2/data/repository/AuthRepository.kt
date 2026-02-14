package com.example.tester2.data.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import javax.inject.Inject
import kotlin.random.Random

interface AuthRepository {
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun signIn(email: String, password: String): Result<Unit>
    suspend fun signOut()
    suspend fun isUserLoggedIn(): Boolean
    suspend fun getCurrentUserId(): String?
}

class AuthRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : AuthRepository {

    override suspend fun signUp(email: String, password: String): Result<Unit> {
        return try {
            val username = generateUniqueUsername()
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = password
                data = JsonObject(
                    mapOf("username" to JsonPrimitive(username))
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
        return supabase.auth.currentSessionOrNull() != null
    }

    override suspend fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
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
        val adjective = Adjectives.random()
        val noun = Nouns.random()
        val number = Random.nextInt(10, 99)
        return "$adjective$noun$number"
    }

    companion object {
        private val Adjectives = listOf("Brave", "Calm", "Swift", "Happy", "Jolly", "Bright", "Misty", "Silent", "Wild", "Green", "Blue", "Red", "Golden", "Silver", "Hidden", "Lost", "Found", "Neon", "Cyber", "Ancient")
        private val Nouns = listOf("Maple", "River", "Lion", "Star", "Wolf", "Bear", "Eagle", "Hawk", "Fox", "Panda", "Tiger", "Moon", "Sun", "Sky", "Ocean", "Forest", "Mountain", "Valley", "Desert", "City")
    }
}
