package com.example.tester2.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tester2.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val preferenceManager: com.example.tester2.utils.PreferenceManager
) : ViewModel() {

    private val _email = MutableStateFlow("")
    val email = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password = _password.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage = _errorMessage.asStateFlow()
    
    private val _isSignUp = MutableStateFlow(false) // Toggle between Sign In / Sign Up
    val isSignUp = _isSignUp.asStateFlow()
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn = _isLoggedIn.asStateFlow()

    private val _username = MutableStateFlow<String?>(null)
    val username = _username.asStateFlow()

    private val _generatedId = MutableStateFlow("User")
    val generatedId = _generatedId.asStateFlow()
    
    // Toggle to show email/password fields for existing users
    private val _showLoginFields = MutableStateFlow(false)
    val showLoginFields = _showLoginFields.asStateFlow()
    
    private val _canAutoLogin = MutableStateFlow(false)
    val canAutoLogin = _canAutoLogin.asStateFlow()

    init {
        checkLoginStatus()
        loadStoredId()
    }
    
    private fun loadStoredId() {
        val storedId = preferenceManager.getLastGeneratedId()
        if (storedId != null) {
            _generatedId.value = storedId
            // Check if last login was within 30 days
            val lastLogin = preferenceManager.getLastLoginTimestamp()
            val thirtyDaysMillis = 30L * 24 * 60 * 60 * 1000
            if (System.currentTimeMillis() - lastLogin < thirtyDaysMillis) {
                _canAutoLogin.value = true
            }
        } else {
             generateNewId()
        }
    }
    
    private fun generateNewId() {
        // Simple generator for demo/anonymous flow
        val adjectives = listOf("Blue", "Red", "Green", "Electric", "Silent", "Happy", "Crimson", "Neon")
        val animals = listOf("Fox", "Bear", "Wolf", "Tiger", "Eagle", "Panda", "Shark", "Hawk")
        val newId = "${adjectives.random()}${animals.random()}${ (10..99).random() }"
        _generatedId.value = newId
        // preferenceManager.saveLastGeneratedId(newId) // Do not save candidate ID until logged in
        _canAutoLogin.value = false // New ID means new user, no auto-login yet
    }
    
    fun onRefreshId() {
        generateNewId()
    }
    
    fun toggleLoginView() {
        _showLoginFields.value = !_showLoginFields.value
        _errorMessage.value = null
        // If switching to login view, we are not in "Sign Up" mode (conceptually), 
        // but for the repository, "signIn" is distinct.
        // We'll let the UI fields drive the standard authenticate().
        _isSignUp.value = false 
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoggedIn.value = repository.isUserLoggedIn()
            if (_isLoggedIn.value) {
                _username.value = repository.getCurrentUsername()
            }
        }
    }

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }
    
    fun toggleMode() {
        _isSignUp.value = !_isSignUp.value
        _errorMessage.value = null
    }

    fun authenticate() {
        if (_email.value.isBlank() || _password.value.isBlank()) {
            _errorMessage.value = "Please fill in all fields"
            return
        }
        performAuth(_email.value, _password.value)
    }
    
    fun createAnonymousAccount() {
        val id = _generatedId.value
        val email = "${id.lowercase()}@hive.anonymous" // Fake email for auth
        val password = "hive_password_${id}" // Auto-generated password
        
        // This is conceptually a Sign Up
        _isSignUp.value = true 
        performAuth(email, password, username = id)
    }
    
    fun attemptAutoLogin() {
        if (_canAutoLogin.value) {
            val id = _generatedId.value
            val email = "${id.lowercase()}@hive.anonymous"
            val password = "hive_password_${id}"
            
            _isSignUp.value = false // Sign In
            performAuth(email, password)
        }
    }
    
    private fun performAuth(email: String, pass: String, username: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            
            // Note: If reusing standard signUp, it might try to insert into 'profiles' table if triggers are set up.
            // Ensure the repository handles this or the backend works with this pattern.
            // For now, we assume standard Supabase Auth works.
            
            val result = if (_isSignUp.value) {
                repository.signUp(email, pass, username)
            } else {
                repository.signIn(email, pass)
            }
            
            result.onSuccess {
                _isLoggedIn.value = true
                preferenceManager.saveLastLoginTimestamp(System.currentTimeMillis())
                
                // CRITICAL FIX: Sync local ID with actual remote username
                // This handles cases where local prefs are out of sync with the account (e.g. ElectricStar vs BlueFox63)
                val currentUsername = repository.getCurrentUsername()
                if (currentUsername != null) {
                    _username.value = currentUsername
                    _generatedId.value = currentUsername
                    if (email.endsWith("@hive.anonymous")) {
                        preferenceManager.saveLastGeneratedId(currentUsername)
                    }
                } else {
                    // Fallback for new signups if getCurrentUsername isn't immediately available (though it should be)
                     if (username != null && email.endsWith("@hive.anonymous")) {
                        preferenceManager.saveLastGeneratedId(username)
                    }
                }
            }.onFailure {
                _errorMessage.value = it.message ?: "Authentication failed"
            }
            _isLoading.value = false
        }
    }
}
