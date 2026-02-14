package com.example.tester2.ui.hive

import android.Manifest
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tester2.data.model.Topic
import com.example.tester2.ui.theme.HiveGreen
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HiveScreen(
    viewModel: HiveViewModel = hiltViewModel(),
    onContributeClick: (String) -> Unit
) {
    val locationPermissions = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    val location by viewModel.location.collectAsState()
    val topics by viewModel.topics.collectAsState()
    val selectedTopic by viewModel.selectedTopic.collectAsState()
    val topicVoices by viewModel.topicVoices.collectAsState()

    val sheetState = rememberModalBottomSheetState()
    
    // Camera State
    val cameraPositionState = rememberCameraPositionState()
    
    LaunchedEffect(key1 = locationPermissions.allPermissionsGranted) {
        if (locationPermissions.allPermissionsGranted) {
            viewModel.startLocationUpdates()
        } else {
            locationPermissions.launchMultiplePermissionRequest()
        }
    }

    LaunchedEffect(location) {
        location?.let {
            // Only move camera if not already moving or first load
            // For MVP, just center on user
            if (!cameraPositionState.isMoving) {
                cameraPositionState.animate(
                    update = CameraUpdateFactory.newLatLngZoom(
                        LatLng(it.latitude, it.longitude),
                        15f
                    )
                )
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (!locationPermissions.allPermissionsGranted) {
             Text(
                "Need location permission to see the Hive",
                modifier = Modifier.align(Alignment.Center)
            )
        } else if (location == null) {
            Text(
                "Waiting for location...",
                modifier = Modifier.align(Alignment.Center)
            )
        } else {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                properties = MapProperties(isMyLocationEnabled = true),
                uiSettings = MapUiSettings(myLocationButtonEnabled = true)
            ) {
                topics.forEach { topic ->
                    Marker(
                        state = MarkerState(position = LatLng(topic.latitude, topic.longitude)),
                        title = topic.title,
                        snippet = "${topic.radius}m radius",
                        onClick = {
                            viewModel.selectTopic(topic)
                            false // Allow default behavior (info window) + our custom sheet
                        }
                    )
                    
                    // Optional: Draw circle for radius
                    Circle(
                        center = LatLng(topic.latitude, topic.longitude),
                        radius = topic.radius.toDouble(),
                        strokeColor = HiveGreen,
                        fillColor = HiveGreen.copy(alpha = 0.2f)
                    )
                }
            }
            
            // Debug Button
            Button(
                onClick = { viewModel.createDummyTopic() },
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            ) {
                Text("Create Test Topic Here")
            }
        }
        
        if (selectedTopic != null) {
            ModalBottomSheet(
                onDismissRequest = { viewModel.dismissTopic() },
                sheetState = sheetState
            ) {
                TopicDetailSheet(
                    topic = selectedTopic!!,
                    voiceNotes = topicVoices,
                    onContributeClick = { topicId ->
                        onContributeClick(topicId)
                        viewModel.dismissTopic()
                    }
                )
            }
        }
    }
}
