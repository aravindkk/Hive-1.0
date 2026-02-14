package com.example.tester2.ui.recorder

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tester2.ui.theme.HiveGreen
import com.example.tester2.ui.theme.HiveRed
import com.example.tester2.ui.theme.HiveWhite

@Composable
fun RecorderScreen(
    onRecordingSaved: () -> Unit,
    viewModel: RecorderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        hasPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    if (!hasPermission) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text("Allow Microphone Access")
            }
        }
    } else {
        RecorderContent(
            viewModel = viewModel,
            onRecordingSaved = onRecordingSaved
        )
    }
}

@Composable
fun RecorderContent(
    viewModel: RecorderViewModel,
    onRecordingSaved: () -> Unit
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val recordedFile by viewModel.recordedFile.collectAsState()

    val isUploading by viewModel.isUploading.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()

    /* 
    // Temporarily disabled navigation to show upload status
    LaunchedEffect(recordedFile) {
        if (recordedFile != null) {
            onRecordingSaved()
        }
    } 
    */

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HiveWhite),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isRecording) {
                Text(
                    text = "Recording...",
                    style = MaterialTheme.typography.headlineMedium,
                    color = HiveRed
                )
                Spacer(modifier = Modifier.height(32.dp))
            } else if (isUploading) {
                CircularProgressIndicator(color = HiveGreen)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Uploading to Cloud...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = HiveGreen
                )
                Spacer(modifier = Modifier.height(32.dp))
            } else if (recordedFile != null && uploadError == null) {
                 Text(
                    text = "Saved & Uploaded!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = HiveGreen
                )
                 Spacer(modifier = Modifier.height(32.dp))
            }

            if (uploadError != null) {
                 Text(
                    text = "Upload Failed: $uploadError",
                    style = MaterialTheme.typography.bodyMedium,
                    color = HiveRed
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            // Record Button
            IconButton(
                onClick = {
                    if (isRecording) {
                        viewModel.stopRecording()
                    } else {
                        viewModel.startRecording()
                    }
                },
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) HiveRed else HiveGreen)
            ) {
                Icon(
                    imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (isRecording) "Stop Recording" else "Start Recording",
                    tint = Color.White,
                    modifier = Modifier.size(64.dp)
                )
            }
        }
    }
}
