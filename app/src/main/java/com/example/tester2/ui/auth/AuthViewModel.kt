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
    private val repository: AuthRepository
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

    init {
        checkLoginStatus()
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            _isLoggedIn.value = repository.isUserLoggedIn()
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
        
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = if (_isSignUp.value) {
                repository.signUp(_email.value, _password.value)
            } else {
                repository.signIn(_email.value, _password.value)
            }
            
            result.onSuccess {
                _isLoggedIn.value = true
            }.onFailure {
                _errorMessage.value = it.message ?: "Authentication failed"
            }
            _isLoading.value = false
        }
    }
}
