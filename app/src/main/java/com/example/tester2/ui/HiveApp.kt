package com.example.tester2.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tester2.ui.auth.AuthScreen
import com.example.tester2.ui.hive.HiveScreen
import com.example.tester2.ui.recorder.RecorderScreen
import com.example.tester2.ui.theme.HiveGreen
import com.example.tester2.ui.theme.HiveTheme
import com.example.tester2.ui.timeline.TimelineScreen

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
                MainScreen(
                    onRecordClick = { topicId -> 
                        val route = if (topicId != null) "record?topicId=$topicId" else "record"
                        navController.navigate(route)
                    }
                )
            }
            
            composable(
                route = "record?topicId={topicId}",
                arguments = listOf(
                    androidx.navigation.navArgument("topicId") {
                        nullable = true
                        defaultValue = null
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) { backStackEntry ->
                RecorderScreen(
                    topicId = backStackEntry.arguments?.getString("topicId"),
                    onRecordingSaved = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(onRecordClick: (String?) -> Unit) {
    val navController = rememberNavController()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hive User") }, // Placeholder for now
                actions = {
                    IconButton(onClick = { /* TODO: Settings */ }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                NavigationBarItem(
                    icon = { Icon(Icons.Default.List, contentDescription = "Feed") },
                    label = { Text("Feed") },
                    selected = currentDestination?.route == "feed",
                    onClick = { navController.navigate("feed") { launchSingleTop = true } }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = "Hive") },
                    label = { Text("Hive") },
                    selected = currentDestination?.route == "hive",
                    onClick = { navController.navigate("hive") { launchSingleTop = true } }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onRecordClick(null) },
                containerColor = HiveGreen
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Record",
                    tint = Color.White
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController, 
            startDestination = "feed", 
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("feed") {
                TimelineScreen()
            }
            composable("hive") {
                HiveScreen(
                    onContributeClick = { topicId -> onRecordClick(topicId) }
                )
            }
        }
    }
}
