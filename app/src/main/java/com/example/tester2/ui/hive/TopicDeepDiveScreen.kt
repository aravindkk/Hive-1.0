package com.example.tester2.ui.hive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tester2.data.model.Topic
import com.example.tester2.ui.theme.HiveGreen
import com.example.tester2.ui.theme.HiveLightGray
import com.example.tester2.ui.theme.HiveWhite
import kotlinx.coroutines.delay

@Composable
fun TopicDeepDiveScreen(
    topicId: String?,
    onBackClick: () -> Unit,
    viewModel: LocalHiveViewModel = hiltViewModel() // Reusing for simplicity or create specific VM
) {
    // In a real app, fetch topic details by ID. For now, mocking data based on ID.
    val topicTitle = when(topicId) {
        "d1" -> "Traffic"
        "d2" -> "Late Night Food"
        "d3" -> "Lost Dog"
        else -> "Topic Details"
    }

    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    // Mock playing simulation
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val startTime = System.currentTimeMillis()
            val duration = 10000L // 10 seconds mock
            while (isPlaying && progress < 1f) {
                val elapsed = System.currentTimeMillis() - startTime
                progress = (elapsed / duration.toFloat()).coerceIn(0f, 1f)
                delay(100)
                if (progress >= 1f) isPlaying = false
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(HiveWhite)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "LIVE TOPIC",
                style = MaterialTheme.typography.labelMedium,
                color = HiveGreen,
                modifier = Modifier
                    .background(HiveGreen.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1f))
            // Context Menu Icon...
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Title
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "KORAMANGALA",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = topicTitle,
                style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "83 voices contributed today", // Mock
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Player Circle
        Box(
            modifier = Modifier
                .size(240.dp)
                .align(Alignment.CenterHorizontally),
            contentAlignment = Alignment.Center
        ) {
            // Outer Ring
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = HiveLightGray,
                strokeWidth = 2.dp,
            )
            
            // Progress Ring
            CircularProgressIndicator(
                 progress = { progress },
                 modifier = Modifier.fillMaxSize(),
                 color = HiveGreen,
                 strokeWidth = 6.dp,
            )

            // Play Button
            Button(
                onClick = { 
                    isPlaying = !isPlaying 
                    if (!isPlaying) progress = 0f // Reset on stop for demo
                },
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = HiveGreen.copy(alpha = 0.1f)),
                modifier = Modifier.size(120.dp),
                contentPadding = PaddingValues(0.dp)
            ) {
                 Icon(
                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = "Play AI Summary",
                    tint = HiveGreen,
                    modifier = Modifier.size(64.dp)
                 )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Transcript
        Text(
            text = "AI SUMMARY TRANSCRIPT",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "\"Overall, the community is frustrated with the bottleneck near the Sony World signal. Most voices report a 20-minute delay due to ongoing metro construction...\"",
            style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
            color = Color.Black
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "\"Several users suggested taking the inner ring road alternate route, though it's getting crowded too...\"",
             style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
            color = Color.Gray
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Footer CTA
        Button(
            onClick = { /* TODO: Open Recorder */ },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HiveGreen)
        ) {
             Text("Tap to Speak")
        }
    }
}
