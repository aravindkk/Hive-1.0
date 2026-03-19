package com.example.tester2.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BubbleChart
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.tester2.R
import com.example.tester2.ui.auth.AuthScreen
import com.example.tester2.ui.auth.AuthViewModel
import com.example.tester2.ui.hive.LocalHiveScreen
import com.example.tester2.ui.hive.TopicDeepDiveScreen
import com.example.tester2.ui.recorder.RecorderScreen
import com.example.tester2.ui.theme.HiveGreen
import com.example.tester2.ui.theme.HiveTheme
import com.example.tester2.ui.timeline.TimelineScreen
import com.google.accompanist.permissions.isGranted
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

private val NavActiveColor = Color(0xFF166534)
private val NavInactiveColor = Color(0xFF9E9E9E)
private val ScreenBg = Color(0xFFF9F9F4)

@Composable
fun HiveApp(initialDeepLink: String? = null) {
    HiveTheme {
        val authViewModel: AuthViewModel = hiltViewModel()
        val isLoggedIn by authViewModel.isLoggedIn.collectAsState()
        val isAuthCheckComplete by authViewModel.isAuthCheckComplete.collectAsState()

        // Wait for auth check — show plain background so no splash flashes for returning users
        if (!isAuthCheckComplete) {
            Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF9F9F4)))
            return@HiveTheme
        }

        val navController = rememberNavController()
        val startDest = when {
            isLoggedIn -> "home"
            !authViewModel.hasSeenSplash -> "splash"
            else -> "auth"
        }

        NavHost(navController = navController, startDestination = startDest) {
            composable("splash") {
                SplashScreen(onComplete = {
                    authViewModel.markSplashSeen()
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
                MainScreen(initialDeepLink = initialDeepLink)
            }
        }
    }
}

@Composable
private fun SplashContent() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(id = R.drawable.splash_background),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
fun SplashScreen(onComplete: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(2000)
        onComplete()
    }
    SplashContent()
}

@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(initialDeepLink: String? = null) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Hide nav chrome on full-screen destinations
    val showNav = currentRoute in listOf("feed", "local_hive")

    val permissions = buildList {
        add(android.Manifest.permission.RECORD_AUDIO)
        add(android.Manifest.permission.ACCESS_FINE_LOCATION)
        add(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }
    val multiplePermissionsState = com.google.accompanist.permissions.rememberMultiplePermissionsState(permissions)

    LaunchedEffect(Unit) {
        multiplePermissionsState.launchMultiplePermissionRequest()
    }

    val context = LocalContext.current
    val locationSettingsLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { /* user responded — location updates will start automatically */ }

    val locationPermissionsGranted = multiplePermissionsState.permissions
        .filter { it.permission == android.Manifest.permission.ACCESS_FINE_LOCATION ||
                  it.permission == android.Manifest.permission.ACCESS_COARSE_LOCATION }
        .any { it.status.isGranted }

    LaunchedEffect(locationPermissionsGranted) {
        if (!locationPermissionsGranted) return@LaunchedEffect
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10_000L).build()
        val settingsRequest = LocationSettingsRequest.Builder().addLocationRequest(request).build()
        LocationServices.getSettingsClient(context)
            .checkLocationSettings(settingsRequest)
            .addOnFailureListener { exception ->
                if (exception is ResolvableApiException) {
                    try {
                        locationSettingsLauncher.launch(
                            IntentSenderRequest.Builder(exception.resolution).build()
                        )
                    } catch (e: Exception) { /* ignore */ }
                }
            }
    }

    LaunchedEffect(initialDeepLink) {
        if (initialDeepLink == null) return@LaunchedEffect
        when {
            initialDeepLink.startsWith("hive://topic/") -> {
                val topicId = initialDeepLink.removePrefix("hive://topic/")
                navController.navigate("topic_deep_dive/$topicId")
            }
            initialDeepLink == "hive://timeline" -> navController.navigate("feed")
            initialDeepLink == "hive://feed"     -> navController.navigate("local_hive")
        }
    }

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val navBarTotalHeight = 64.dp + bottomInset

    Box(modifier = Modifier.fillMaxSize()) {
        // Main content — padded at bottom to clear nav bar + system nav
        NavHost(
            navController = navController,
            startDestination = "feed",
            modifier = Modifier
                .fillMaxSize()
                .then(if (showNav) Modifier.padding(bottom = navBarTotalHeight) else Modifier)
        ) {
            composable("feed") {
                TimelineScreen(
                    onTopicClick = { topicId -> navController.navigate("topic_deep_dive/$topicId") }
                )
            }
            composable(
                route = "record?topicId={topicId}&topicTitle={topicTitle}",
                arguments = listOf(
                    androidx.navigation.navArgument("topicId") {
                        nullable = true
                        defaultValue = null
                        type = androidx.navigation.NavType.StringType
                    },
                    androidx.navigation.navArgument("topicTitle") {
                        nullable = true
                        defaultValue = null
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) { backStackEntry ->
                RecorderScreen(
                    topicId = backStackEntry.arguments?.getString("topicId"),
                    topicTitle = backStackEntry.arguments?.getString("topicTitle"),
                    onRecordingSaved = { navController.popBackStack() },
                    onNavigateToTopic = { topicId ->
                        navController.navigate("topic_deep_dive/$topicId") {
                            popUpTo("record?topicId={topicId}") { inclusive = true }
                        }
                    }
                )
            }
            composable("local_hive") {
                LocalHiveScreen(
                    onTopicClick = { topic -> navController.navigate("topic_deep_dive/${topic.id}") }
                )
            }
            composable(
                route = "topic_deep_dive/{topicId}",
                arguments = listOf(
                    androidx.navigation.navArgument("topicId") {
                        type = androidx.navigation.NavType.StringType
                    }
                )
            ) { backStackEntry ->
                val topicId = backStackEntry.arguments?.getString("topicId") ?: return@composable
                TopicDeepDiveScreen(
                    topicId = topicId,
                    onBackClick = { navController.popBackStack() },
                    onSpeakClick = { id, title ->
                        navController.navigate("record?topicId=$id&topicTitle=${Uri.encode(title)}")
                    }
                )
            }
        }

        // Custom bottom nav — only on main tabs
        if (showNav) {
            // Column: nav items row + spacer behind system nav bar
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(ScreenBg)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 48.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NavItem(
                        icon = Icons.Default.Mic,
                        label = "My Thoughts",
                        selected = currentRoute == "feed",
                        onClick = {
                            navController.navigate("feed") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                    NavItem(
                        icon = Icons.Default.BubbleChart,
                        label = "Topics",
                        selected = currentRoute == "local_hive",
                        onClick = {
                            navController.navigate("local_hive") {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
                // Fills the space behind the system navigation bar
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsBottomHeight(WindowInsets.navigationBars)
                )
            }

            // FAB — only on My Thoughts tab
            if (currentRoute == "feed") Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 20.dp, bottom = navBarTotalHeight + 8.dp)
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(HiveGreen)
                    .clickable { navController.navigate("record") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Record",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val color = if (selected) NavActiveColor else NavInactiveColor
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
        modifier = Modifier
            .clickable(
                indication = null,
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}
