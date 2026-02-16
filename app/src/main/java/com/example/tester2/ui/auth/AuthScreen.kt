package com.example.tester2.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
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

        // Logo
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(HiveYellow.copy(alpha = 0.8f), CircleShape)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "Hive Logo",
                tint = Color(0xFF854D0E), // Brown/Dark Yellow
                modifier = Modifier.size(48.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Hive",
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Light,
                fontSize = 56.sp
            ),
            color = HiveDarkGray
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "CONNECT. THINK. FEEL.",
            style = MaterialTheme.typography.labelSmall.copy(
                letterSpacing = 4.sp,
                fontWeight = FontWeight.Bold
            ),
            color = HiveMediumGray
        )

        // User ID Card
        val canAutoLogin by viewModel.canAutoLogin.collectAsState()
        
        if (canAutoLogin) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.attemptAutoLogin() },
                shape = RoundedCornerShape(50),
                color = HiveWhite.copy(alpha = 0.9f),
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp, 
                    color = HiveGreen
                )
            ) {
                Row(
                    modifier = Modifier
                        .padding(8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(HiveGreen.copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "User Icon",
                            tint = HiveGreen
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "WELCOME BACK",
                            style = MaterialTheme.typography.labelSmall,
                            color = HiveGreen
                        )
                        Text(
                            text = generatedId,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = HiveDarkGray
                        )
                        Text(
                            text = "Tap to Login",
                            style = MaterialTheme.typography.labelSmall,
                            color = HiveGreen
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "ANONYMOUS IDENTITIES",
            style = MaterialTheme.typography.labelSmall,
            color = HiveMediumGray.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // Create Account Button
        Button(
            onClick = viewModel::createAnonymousAccount,
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Create Account",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        tint = HiveYellow,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Sign In Button
        OutlinedButton(
            onClick = viewModel::toggleLoginView,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, HiveLightGray),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = HiveNavy,
                containerColor = HiveWhite
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
