package com.example.tester2.ui.recorder

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tester2.ui.theme.HiveGreen

private val HiveCreamBg = Color(0xFFF9F9F4)
private val AuraGreen = Color(0xFF22C55E)
private val AuraOrange = Color(0xFFF97316)
private val AuraRed = Color(0xFFEF4444)

@Composable
fun RecorderScreen(
    topicId: String? = null,
    topicTitle: String? = null,
    onRecordingSaved: () -> Unit,
    onNavigateToTopic: (String) -> Unit = {},
    viewModel: RecorderViewModel = hiltViewModel()
) {
    LaunchedEffect(topicId) { viewModel.setTopicId(topicId) }

    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { hasPermission = it }
    LaunchedEffect(Unit) { if (!hasPermission) permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }

    if (!hasPermission) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }) {
                Text("Allow Microphone Access")
            }
        }
    } else {
        RecorderContent(
            topicId = topicId,
            topicTitle = topicTitle,
            viewModel = viewModel,
            onRecordingSaved = onRecordingSaved,
            onNavigateToTopic = onNavigateToTopic
        )
    }
}

@Composable
fun RecorderContent(
    topicId: String?,
    topicTitle: String?,
    viewModel: RecorderViewModel,
    onRecordingSaved: () -> Unit,
    onNavigateToTopic: (String) -> Unit
) {
    val isRecording by viewModel.isRecording.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val isSaved by viewModel.isSaved.collectAsState()
    val uploadError by viewModel.uploadError.collectAsState()
    val amplitudeBars by viewModel.amplitudeBars.collectAsState()
    val auraColor by viewModel.auraColor.collectAsState()
    val areaName by viewModel.areaName.collectAsState()
    val recordingSeconds by viewModel.recordingSeconds.collectAsState()

    // Animate the aura color smoothly between states
    val targetAuraColor = when (auraColor) {
        AuraColor.GREEN -> AuraGreen
        AuraColor.ORANGE -> AuraOrange
        AuraColor.RED -> AuraRed
    }
    val animatedAura by animateColorAsState(
        targetValue = if (isRecording) targetAuraColor else Color.Transparent,
        animationSpec = tween(durationMillis = 1500),
        label = "aura"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HiveCreamBg)
            .drawBehind {
                // Screen-edge glow: four gradient strips
                if (animatedAura != Color.Transparent) {
                    val glowWidth = 48.dp.toPx()
                    val color = animatedAura.copy(alpha = 0.35f)
                    val transparent = color.copy(alpha = 0f)
                    // Left edge
                    drawRect(brush = Brush.horizontalGradient(listOf(color, transparent), endX = glowWidth))
                    // Right edge
                    drawRect(brush = Brush.horizontalGradient(listOf(transparent, color), startX = size.width - glowWidth))
                    // Top edge
                    drawRect(brush = Brush.verticalGradient(listOf(color, transparent), endY = glowWidth))
                    // Bottom edge
                    drawRect(brush = Brush.verticalGradient(listOf(transparent, color), startY = size.height - glowWidth))
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Location header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = HiveGreen, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(
                    text = areaName,
                    style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
                    color = Color(0xFF1C1C1C)
                )
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF9E9E9E), modifier = Modifier.size(20.dp))
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    isSaved -> SavedCard(topicTitle = topicTitle, onDone = onRecordingSaved)
                    isSaving -> SavingState()
                    uploadError != null -> ErrorState(error = uploadError!!, onRetry = onRecordingSaved)
                    isRecording -> RecordingActiveState(
                        topicTitle = topicTitle,
                        amplitudeBars = amplitudeBars,
                        auraColor = animatedAura,
                        recordingSeconds = recordingSeconds,
                        onDiscard = { viewModel.discardRecording() },
                        onFinish = { viewModel.stopRecording() }
                    )
                    else -> RecordingIdleState(topicTitle = topicTitle, onStartRecord = { viewModel.startRecording() })
                }
            }
        }
    }
}

@Composable
private fun RecordingIdleState(topicTitle: String?, onStartRecord: () -> Unit) {
    val freeFormPrompts = remember {
        listOf(
            "What's one thing in\nyour area that secretly\ndrives you crazy?",
            "What's happening in\nyour neighborhood today?",
            "What's on your mind\nabout your community?",
            "Share a thought with\nthe Hive!"
        )
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.padding(horizontal = 32.dp)
    ) {
        if (topicTitle != null) {
            Surface(shape = RoundedCornerShape(20.dp), color = HiveGreen.copy(alpha = 0.15f)) {
                Text(
                    topicTitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = HiveGreen,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "What's your take?",
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, lineHeight = 36.sp),
                color = Color(0xFF1C1C1C),
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                text = remember { freeFormPrompts.random() },
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Medium, lineHeight = 36.sp),
                color = Color(0xFF1C1C1C),
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.height(48.dp))

        IconButton(
            onClick = onStartRecord,
            modifier = Modifier.size(80.dp).clip(CircleShape).background(HiveGreen)
        ) {
            Icon(Icons.Default.Mic, contentDescription = "Start Recording", tint = Color.White, modifier = Modifier.size(40.dp))
        }
    }
}

@Composable
private fun RecordingActiveState(
    topicTitle: String?,
    amplitudeBars: List<Float>,
    auraColor: Color,
    recordingSeconds: Int,
    onDiscard: () -> Unit,
    onFinish: () -> Unit
) {
    val mins = recordingSeconds / 60
    val secs = recordingSeconds % 60
    val timerText = "%d:%02d".format(mins, secs)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
    ) {
        if (topicTitle != null) {
            Surface(shape = RoundedCornerShape(20.dp), color = HiveGreen.copy(alpha = 0.15f)) {
                Text(
                    topicTitle,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = HiveGreen,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        } else {
            Spacer(Modifier.height(40.dp))
        }

        Spacer(Modifier.height(24.dp))

        // Dot + timer + Recording label
        val dotColor = if (auraColor == Color.Transparent) HiveGreen else auraColor
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(dotColor))
            Text(
                text = timerText,
                style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FontFamily.Monospace),
                color = Color(0xFF1C1C1C)
            )
            Text(
                "Recording",
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF9E9E9E)
            )
        }

        Spacer(Modifier.height(32.dp))

        // Real amplitude waveform
        val barColor = if (auraColor == Color.Transparent) Color(0xFF4ADE80) else auraColor
        Canvas(modifier = Modifier.fillMaxWidth().height(56.dp)) {
            val barCount = amplitudeBars.size
            val barWidth = size.width / (barCount * 2f)
            val gap = barWidth
            amplitudeBars.forEachIndexed { i, heightFraction ->
                val barH = (heightFraction * size.height * 0.85f).coerceAtLeast(6f)
                val x = i * (barWidth + gap) + barWidth / 2f
                val top = (size.height - barH) / 2f
                drawLine(
                    color = barColor,
                    start = Offset(x, top),
                    end = Offset(x, top + barH),
                    strokeWidth = barWidth
                )
            }
        }

        Spacer(Modifier.height(40.dp))

        IconButton(
            onClick = {},
            modifier = Modifier.size(80.dp).clip(CircleShape).background(HiveGreen)
        ) {
            Icon(Icons.Default.Mic, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
        }

        Spacer(Modifier.height(48.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            OutlinedButton(
                onClick = onDiscard,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF9E9E9E)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
            ) {
                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Discard")
            }
            Button(
                onClick = onFinish,
                modifier = Modifier.weight(1f).height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = HiveGreen)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Finish")
            }
        }
    }
}

@Composable
private fun SavingState() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = HiveGreen)
        Spacer(Modifier.height(16.dp))
        Text("Saving your thought...", style = MaterialTheme.typography.bodyLarge, color = HiveGreen)
    }
}

@Composable
private fun SavedCard(topicTitle: String?, onDone: () -> Unit) {
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        onDone()
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(HiveGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
        }
        Spacer(Modifier.height(20.dp))
        if (topicTitle != null) {
            Text(
                "Added to the Hive!",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1C1C1C)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your voice has been added to \"$topicTitle\".",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        } else {
            Text(
                "Saved!",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1C1C1C)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Your thought is saved and will be processed shortly.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun ErrorState(error: String, onRetry: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
        Text("Something went wrong", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(error, style = MaterialTheme.typography.bodySmall, color = Color(0xFFF87171), textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRetry) { Text("Close") }
    }
}
