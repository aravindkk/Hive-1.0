package com.example.tester2.ui.hive

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tester2.data.model.Topic
import com.example.tester2.ui.theme.HiveBlue
import com.example.tester2.ui.theme.HiveGreen
import com.example.tester2.ui.theme.HivePink
import com.example.tester2.ui.theme.HiveWhite
import com.example.tester2.ui.theme.HiveYellow
import kotlin.math.max
import kotlin.random.Random

@Composable
fun LocalHiveScreen(
    viewModel: LocalHiveViewModel = hiltViewModel(),
    onTopicClick: (Topic) -> Unit
) {
    val topics by viewModel.popularTopics.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HiveWhite.copy(alpha = 0.95f)) // Detailed dot pattern background would be ideal
    ) {
        // Just a simple staggered layout simulation for bubbles
        // In a real app, a physics-based layout or a custom Layout composable would be better.
        
        topics.forEachIndexed { index, topic ->
            Bubble(
                topic = topic,
                modifier = Modifier.align(Alignment.Center), // Simplified positioning logic
                offsetX = (index * 60 - 100).dp, // Dummy spreading
                offsetY = (index * 80 - 150).dp,
                onClick = { onTopicClick(topic) }
            )
        }
        
        // Header
        Text(
            "Trending Topics",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
        )
    }
}

@Composable
fun Bubble(
    topic: Topic,
    modifier: Modifier = Modifier,
    offsetX: Dp,
    offsetY: Dp,
    onClick: () -> Unit
) {
    // Map voice count to size
    val baseSize = 100.dp
    val size = baseSize + (topic.voiceCount * 2).toInt().dp
    val finalSize = if (size > 250.dp) 250.dp else size // Cap max size

    // Color based on some logic or random for demo
    val color = when {
        topic.title.contains("Traffic") -> HiveYellow
        topic.title.contains("Food") -> HiveGreen
        topic.title.contains("Noise") -> HivePink
        else -> HiveWhite
    }
    
    Box(
        modifier = modifier
            .offset(x = offsetX, y = offsetY)
            .size(finalSize)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.8f))
            .clickable { onClick() }
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                color = Color.Black.copy(alpha = 0.8f)
            )
            Text(
                text = "${topic.voiceCount} voices",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Black.copy(alpha = 0.6f)
            )
        }
    }
}
