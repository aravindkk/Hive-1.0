package com.example.tester2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import com.example.tester2.ui.theme.HiveTheme
import com.example.tester2.ui.auth.AuthScreen

@Composable
fun HiveApp() {
    HiveTheme {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "auth") {
            composable("auth") {
                AuthScreen(
                    onLoginSuccess = {
                        navController.navigate("home") {
                            popUpTo("auth") { inclusive = true }
                        }
                    }
                )
            }
            composable("home") {
                androidx.compose.material3.Scaffold(
                    floatingActionButton = {
                        androidx.compose.material3.FloatingActionButton(
                            onClick = { navController.navigate("record") },
                            containerColor = com.example.tester2.ui.theme.HiveGreen
                        ) {
                            androidx.compose.material3.Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Mic,
                                contentDescription = "Record",
                                tint = androidx.compose.ui.graphics.Color.White
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        com.example.tester2.ui.timeline.TimelineScreen()
                    }
                }
            }
            composable("record") {
                com.example.tester2.ui.recorder.RecorderScreen(
                    onRecordingSaved = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}
