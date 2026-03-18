package com.example.tester2.ui.hive

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tester2.data.model.Topic
import kotlin.math.cos
import kotlin.math.sin

// Pastel bubble colors matching the design
private val BubbleTeal   = Color(0xFFD1F2DF)
private val BubbleBlue   = Color(0xFFE2F1F8)
private val BubbleWhite  = Color(0xFFFFFFFF)
private val BubbleCoral  = Color(0xFFFDE2DE)
private val BubbleYellow = Color(0xFFFDF0A6)

// Text colors for contrast
private val TextTeal = Color(0xFF166534)
private val TextBlue = Color(0xFF0C4A6E)
private val TextCoral = Color(0xFF7F1D1D)
private val TextYellow = Color(0xFF713F12)
private val TextDark = Color(0xFF1A1A1A)

private val tabLabels = listOf("Trending", "New", "My Topics")

// Data class to hold computed bubble layout
private data class BubbleLayout(
    val topic: Topic,
    val sizeDp: Dp,
    val offsetX: Dp,
    val offsetY: Dp,
    val colorIndex: Int
)

@Composable
fun LocalHiveScreen(
    viewModel: LocalHiveViewModel = hiltViewModel(),
    onTopicClick: (Topic) -> Unit
) {
    val topics by viewModel.popularTopics.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val playingUrl by viewModel.playingUrl.collectAsState()
    val playingTopicTitle by viewModel.playingTopicTitle.collectAsState()
    val areaName by viewModel.areaName.collectAsState()

    // Calculate dynamic bubble layout based on screen density to avoid overlaps
    val density = LocalDensity.current
    val bubbleLayouts = remember(topics, density) {
        computeBubbleLayouts(topics, 6)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF9F9F4)) // off-white/cream background from design
            .statusBarsPadding()
    ) {
        // ── Header ──────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                tint = Color(0xFF166534),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = areaName,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif
                ),
                color = Color(0xFF1A1A1A)
            )
            Spacer(Modifier.weight(1f))
            Icon(
                Icons.Default.Settings,
                contentDescription = "Settings",
                tint = Color(0xFF666666),
                modifier = Modifier.size(26.dp)
            )
        }

        // ── Tabs ─────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(30.dp))
                    .background(Color.White)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                tabLabels.forEachIndexed { index, label ->
                    val selected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(if (selected) Color(0xFF9DF3C4) else Color.Transparent)
                            .clickable { viewModel.setTab(index) }
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (selected) Color(0xFF166534) else Color(0xFF666666)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // ── Bubble cluster area ───────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (topics.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No topics yet.\nRecord a thought to start one.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else {
                // Background Concentric Circles
                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {
                    val centerOffset = Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        color = Color(0xFFE5E5E0).copy(alpha = 0.4f),
                        radius = 180.dp.toPx(),
                        center = centerOffset,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFE5E5E0).copy(alpha = 0.4f),
                        radius = 260.dp.toPx(),
                        center = centerOffset,
                        style = Stroke(width = 1.dp.toPx())
                    )
                    drawCircle(
                        color = Color(0xFFE5E5E0).copy(alpha = 0.4f),
                        radius = 340.dp.toPx(),
                        center = centerOffset,
                        style = Stroke(width = 1.dp.toPx())
                    )
                }

                // Dislay Dynamic Bubbles
                bubbleLayouts.forEach { layout ->
                    Bubble(
                        topic = layout.topic,
                        size = layout.sizeDp,
                        colorIndex = layout.colorIndex,
                        modifier = Modifier.align(Alignment.Center),
                        offsetX = layout.offsetX,
                        offsetY = layout.offsetY,
                        onClick = {
                            viewModel.onTopicTapped(layout.topic)
                            onTopicClick(layout.topic)
                        }
                    )
                }
            }

            // ── Mini-player ───────────────────────────────────────
            if (playingUrl != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .shadow(8.dp, RoundedCornerShape(30.dp), spotColor = Color.Black.copy(alpha = 0.05f))
                            .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(30.dp))
                            .clip(RoundedCornerShape(30.dp))
                            .background(Color.White)
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(modifier = Modifier.size(20.dp)) {
                            // Simple equalizer drawing
                            val barWidth = 3.dp.toPx()
                            val spacing = 3.dp.toPx()
                            val color = Color(0xFF0F766E)
                            drawLine(color, Offset(0f, size.height * 0.3f), Offset(0f, size.height * 0.7f), barWidth, StrokeCap.Round)
                            drawLine(color, Offset(barWidth + spacing, size.height * 0.1f), Offset(barWidth + spacing, size.height * 0.9f), barWidth, StrokeCap.Round)
                            drawLine(color, Offset((barWidth + spacing) * 2, size.height * 0.4f), Offset((barWidth + spacing) * 2, size.height * 0.6f), barWidth, StrokeCap.Round)
                        }
                        
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Listening to ",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = playingTopicTitle ?: "…",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF0F766E)
                        )
                        Spacer(Modifier.width(24.dp))
                        
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF3F4F6))
                                .clickable { viewModel.stopAudio() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Stop",
                                tint = Color(0xFF4B5563),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Compute sizes based on voice count, then run circular packing algorithm
private fun computeBubbleLayouts(topics: List<Topic>, maxCount: Int): List<BubbleLayout> {
    if (topics.isEmpty()) return emptyList()

    val displayTopics = topics.sortedByDescending { it.voiceCount }.take(maxCount)
    
    // 1) Determine sizing dynamically based on relative voiceCount
    val maxVoiceCount = displayTopics.maxOf { it.voiceCount }.coerceAtLeast(1L).toFloat()
    
    val minSizeDp = 100f
    val maxSizeDp = 180f
    
    // Helper to compute size for a given count
    fun sizeFor(count: Long): Float {
        val ratio = count.toFloat() / maxVoiceCount
        // Use a slight curve so small items aren't completely tiny, but differences are noticeable
        val curvedRatio = Math.pow(ratio.toDouble(), 0.6).toFloat()
        return minSizeDp + (maxSizeDp - minSizeDp) * curvedRatio
    }

    // 2) Pack visually radially outward so they don't overlap
    // List of placed circles: Pair(Offset(x,y), radius)
    val placed = mutableListOf<Pair<Offset, Float>>()
    val layouts = mutableListOf<BubbleLayout>()
    
    val paddingPx = 8f // Extra buffer between bubbles
    
    displayTopics.forEachIndexed { index, topic ->
        val bubbleSizeDp = sizeFor(topic.voiceCount)
        val radius = bubbleSizeDp / 2f
        
        var placedOffset = Offset.Zero
        
        if (index == 0) {
            // First (largest) bubble goes directly in center
            placedOffset = Offset.Zero
            placed.add(Pair(placedOffset, radius))
        } else {
            // Radial search for a valid spot
            var found = false
            var currentDistance = 10f // Start slightly offset to search outward
            var angleOffset = index * 45f // Stagger starting angle for visual distribution
            var angleStep = 30f // How much we step angle while searching
            
            while (!found) {
                var angle = angleOffset
                var attemptsInRing = 0
                val maxAttemptsInRing = (360f / angleStep).toInt()
                
                while (attemptsInRing < maxAttemptsInRing && !found) {
                    val rad = Math.toRadians(angle.toDouble())
                    val testX = currentDistance * cos(rad).toFloat()
                    val testY = currentDistance * sin(rad).toFloat()
                    val testOffset = Offset(testX, testY)
                    
                    // Check overlap against all placed
                    var overlaps = false
                    for (p in placed) {
                        val existingOffset = p.first
                        val existingRadius = p.second
                        
                        val dx = testOffset.x - existingOffset.x
                        val dy = testOffset.y - existingOffset.y
                        val distSq = dx * dx + dy * dy
                        
                        val minAllowedDist = radius + existingRadius + paddingPx
                        if (distSq < minAllowedDist * minAllowedDist) {
                            overlaps = true
                            break
                        }
                    }
                    
                    if (!overlaps) {
                        placedOffset = testOffset
                        found = true
                    } else {
                        angle += angleStep
                        attemptsInRing++
                    }
                }
                
                // If we didn't find a spot in this ring, increase distance and try again
                if (!found) {
                    currentDistance += 10f
                }
            }
            
            placed.add(Pair(placedOffset, radius))
        }
        
        layouts.add(
            BubbleLayout(
                topic = topic,
                sizeDp = bubbleSizeDp.dp,
                offsetX = placedOffset.x.dp, // Simplification: calculating via pseudo-Dp coordinate space
                offsetY = placedOffset.y.dp,
                colorIndex = index
            )
        )
    }
    
    return layouts
}

@Composable
private fun Bubble(
    topic: Topic,
    size: Dp,
    colorIndex: Int,
    modifier: Modifier = Modifier,
    offsetX: Dp,
    offsetY: Dp,
    onClick: () -> Unit
) {
    val bubbleColors = listOf(BubbleTeal, BubbleBlue, BubbleWhite, BubbleCoral, BubbleYellow)
    val textColors = listOf(TextTeal, TextBlue, TextDark, TextCoral, TextYellow)
    
    val safeIndex = colorIndex % bubbleColors.size
    val color = bubbleColors[safeIndex]
    val textColor = textColors[safeIndex]
    val iconColor = textColor

    Box(
        modifier = modifier
            .offset(x = offsetX, y = offsetY)
            .size(size)
            .shadow(
                elevation = if (color == BubbleWhite) 8.dp else 0.dp, 
                shape = CircleShape, 
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(CircleShape)
            .background(color.copy(alpha = 0.9f))
            .border(
                width = 1.dp,
                color = if (color == BubbleWhite) Color(0xFFD1D5DB) else Color.Transparent,
                shape = CircleShape
            )
            .clickable { onClick() }
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = categoryIcon(topic.title),
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(if (size < 110.dp) 18.dp else 26.dp)
            )
            Text(
                text = topic.title,
                style = if (size < 110.dp) MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium) else MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, fontFamily = FontFamily.Serif),
                textAlign = TextAlign.Center,
                color = textColor,
                lineHeight = if (size < 110.dp) 14.sp else 20.sp,
                maxLines = 2
            )
            if (size > 110.dp) {
                Text(
                    text = voiceCountLabel(topic.voiceCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

private fun categoryIcon(title: String): ImageVector {
    val t = title.lowercase()
    return when {
        t.contains("water") || t.contains("pipe") || t.contains("supply") -> Icons.Default.WaterDrop
        t.contains("traffic") || t.contains("road") || t.contains("commute") || t.contains("signal") -> Icons.Default.Traffic
        t.contains("food") || t.contains("cafe") || t.contains("restaurant") || t.contains("dine") || t.contains("eat") -> Icons.Default.LocalCafe
        t.contains("safe") || t.contains("crime") || t.contains("security") || t.contains("alert") -> Icons.Default.Shield
        t.contains("park") || t.contains("garden") || t.contains("nature") || t.contains("green") -> Icons.Default.Park
        t.contains("construct") || t.contains("noise") || t.contains("build") -> Icons.Default.Construction
        t.contains("power") || t.contains("electric") || t.contains("light") -> Icons.Default.ElectricBolt
        else -> Icons.Default.Forum
    }
}

private fun voiceCountLabel(count: Long): String = when {
    count == 0L -> "Alert active" // Based on Safety bubble
    count < 10L -> "$count local"
    count < 100L -> "$count voices"
    count < 1000L -> "$count scouting" // Based on New Cafe
    else -> "${count / 100}k joined" // Based on Water Supply
}
