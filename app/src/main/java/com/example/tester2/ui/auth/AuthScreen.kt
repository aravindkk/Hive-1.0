package com.example.tester2.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tester2.ui.theme.*

@Composable
fun AuthScreen(
    onLoginSuccess: () -> Unit,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val showLoginFields by viewModel.showLoginFields.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            onLoginSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(HiveWhite, HiveCream),
                    radius = 1500f,
                    center = androidx.compose.ui.geometry.Offset(x = 500f, y = 500f)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        if (showLoginFields) {
            LoginForm(viewModel)
        } else {
            LandingScreen(viewModel)
        }
    }
}

@Composable
fun LandingScreen(viewModel: AuthViewModel) {
    val generatedId by viewModel.generatedId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Logo — green circle with honeycomb icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(HiveGreen.copy(alpha = 0.25f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Hive Logo",
                tint = HiveGreen,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Hive",
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Normal,
                fontSize = 52.sp
            ),
            color = HiveDarkGray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Connect. Think. Feel.",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Normal),
            color = HiveMediumGray
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Digital identity card
        val canAutoLogin by viewModel.canAutoLogin.collectAsState()

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = HiveWhite.copy(alpha = 0.9f),
            border = androidx.compose.foundation.BorderStroke(1.dp, HiveLightGray)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "YOUR DIGITAL IDENTITY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = HiveMediumGray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "@$generatedId",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ),
                        color = HiveDarkGray,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = viewModel::onRefreshId,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Regenerate",
                            tint = HiveMediumGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Hive protects your privacy. You'll be as an anonymous entity.",
                    style = MaterialTheme.typography.bodySmall,
                    color = HiveMediumGray
                )
            }
        }

        if (canAutoLogin) {
            Spacer(modifier = Modifier.height(12.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.attemptAutoLogin() },
                shape = RoundedCornerShape(50),
                color = HiveGreen.copy(alpha = 0.1f),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, HiveGreen)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Welcome back, @$generatedId",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = HiveGreen,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Tap to log in →",
                        style = MaterialTheme.typography.labelSmall,
                        color = HiveGreen
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Create Account Button
        Button(
            onClick = viewModel::createAnonymousAccount,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HiveGreen,
                contentColor = HiveDarkGray
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = HiveDarkGray, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sign In Button
        OutlinedButton(
            onClick = viewModel::toggleLoginView,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HiveLightGray),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = HiveDarkGray,
                containerColor = HiveWhite.copy(alpha = 0.7f)
            )
        ) {
            Text(
                text = "Sign In",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium)
            )
        }

        Spacer(modifier = Modifier.weight(1f))
        
        // Footer
        Row(
            modifier = Modifier.padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "PRIVACY",
                style = MaterialTheme.typography.labelSmall,
                color = HiveMediumGray
            )
            Text(
                text = "•",
                style = MaterialTheme.typography.labelSmall,
                color = HiveMediumGray
            )
            Text(
                text = "TERMS",
                style = MaterialTheme.typography.labelSmall,
                color = HiveMediumGray
            )
        }
    }
}

@Composable
fun LoginForm(viewModel: AuthViewModel) {
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var passwordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        
        // Back Button
        Row(modifier = Modifier.fillMaxWidth()) {
            IconButton(onClick = viewModel::toggleLoginView) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Welcome Back",
            style = MaterialTheme.typography.headlineLarge,
            color = HiveDarkGray
        )
        
        Spacer(modifier = Modifier.height(32.dp))

        // Error Message
        errorMessage?.let {
            Text(
                text = it,
                color = HiveRed,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        // Inputs
        OutlinedTextField(
            value = email,
            onValueChange = viewModel::onEmailChange,
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HiveNavy,
                unfocusedBorderColor = HiveLightGray,
                focusedLabelColor = HiveNavy
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = viewModel::onPasswordChange,
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HiveNavy,
                unfocusedBorderColor = HiveLightGray,
                focusedLabelColor = HiveNavy
            ),
            trailingIcon = {
                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(imageVector = image, contentDescription = "Toggle password visibility")
                }
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Sign In Button
        Button(
            onClick = viewModel::authenticate,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = HiveNavy,
                contentColor = HiveWhite
            ),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = HiveWhite, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}
