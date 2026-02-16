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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.collectAsState
import com.example.tester2.ui.auth.AuthScreen
import com.example.tester2.ui.auth.AuthViewModel
import com.example.tester2.ui.hive.HiveScreen
import com.example.tester2.ui.recorder.RecorderScreen
import com.example.tester2.ui.theme.HiveGreen
import com.example.tester2.ui.theme.HiveTheme
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import com.example.tester2.ui.timeline.TimelineScreen
import com.example.tester2.ui.hive.LocalHiveScreen
import com.example.tester2.ui.hive.TopicDeepDiveScreen
import androidx.compose.material.icons.filled.BubbleChart
import com.example.tester2.R
import android.app.Activity
import android.content.IntentSender
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import androidx.compose.ui.platform.LocalContext

@Composable
fun HiveApp() {
    HiveTheme {
        val navController = rememberNavController()
        
        NavHost(navController = navController, startDestination = "splash") {
            composable("splash") {
                SplashScreen(onTimeout = {
                    navController.navigate("auth") {
                        popUpTo("splash") { inclusive = true }
                    }
                })
            }
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
                MainScreen()
            }
        }
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onTimeout()
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Image(
            painter = androidx.compose.ui.res.painterResource(id = com.example.tester2.R.drawable.splash_background),
            contentDescription = "Splash Screen",
            modifier = Modifier.fillMaxSize(),
            contentScale = androidx.compose.ui.layout.ContentScale.Crop
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(
    authViewModel: com.example.tester2.ui.auth.AuthViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val navController = rememberNavController()
    val username by authViewModel.username.collectAsState()
    
    // Request permissions on launch
    val multiplePermissionsState = com.google.accompanist.permissions.rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.RECORD_AUDIO,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    
    val context = LocalContext.current
    val locationSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { /* No-op: Check again if needed */ }

    androidx.compose.runtime.LaunchedEffect(Unit) {
        multiplePermissionsState.launchMultiplePermissionRequest()
        
        // Check if GPS is enabled
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(context)
        
        client.checkLocationSettings(builder.build()).addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettingsLauncher.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        modifier = Modifier.clickable {
                            navController.navigate("feed") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(text = username?.let { "$it" } ?: "Hive User")
                    }
                },
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
                    onClick = { 
                        navController.navigate("feed") { 
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        } 
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Map, contentDescription = "Hive") },
                    label = { Text("Hive") },
                    selected = currentDestination?.route == "hive",
                    onClick = { 
                        navController.navigate("hive") { 
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        } 
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.BubbleChart, contentDescription = "Local Hive") },
                    label = { Text("Local Hive") },
                    selected = currentDestination?.route == "local_hive",
                    onClick = { 
                        navController.navigate("local_hive") { 
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        } 
                    }
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("record") },
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
                    onContributeClick = { topicId -> 
                         val route = "record?topicId=$topicId"
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
            composable("local_hive") {
                LocalHiveScreen(
                    onTopicClick = { topic ->
                        navController.navigate("topic_deep_dive/${topic.id}")
                    }
                )
            }
            composable(
                route = "topic_deep_dive/{topicId}",
                arguments = listOf(
                    androidx.navigation.navArgument("topicId") { type = androidx.navigation.NavType.StringType }
                )
            ) { backStackEntry ->
                TopicDeepDiveScreen(
                    topicId = backStackEntry.arguments?.getString("topicId"),
                    onBackClick = { navController.popBackStack() },
                    onSpeakClick = { topicId ->
                        val route = if (topicId != null) "record?topicId=$topicId" else "record"
                        navController.navigate(route)
                    }
                )
            }
        }
    }
}
