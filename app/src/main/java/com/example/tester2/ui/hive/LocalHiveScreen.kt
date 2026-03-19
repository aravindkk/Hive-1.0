package com.example.tester2.ui.hive

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.example.tester2.data.model.Topic
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.cos
import kotlin.math.sin

private val IST = ZoneId.of("Asia/Kolkata")

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

// Absolute top-left position within the canvas (in dp)
private data class BubbleLayout(
    val topic: Topic,
    val sizeDp: Float,
    val x: Float,
    val y: Float,
    val colorIndex: Int
)

private data class BubbleCanvasData(
    val layouts: List<BubbleLayout>,
    val canvasWidth: Float,
    val canvasHeight: Float
)

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun LocalHiveScreen(
    viewModel: LocalHiveViewModel = hiltViewModel(),
    onTopicClick: (Topic) -> Unit
) {
    val topics by viewModel.popularTopics.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val playingUrl by viewModel.playingUrl.collectAsState()
    val playingTopicTitle by viewModel.playingTopicTitle.collectAsState()
    val areaName by viewModel.areaName.collectAsState()

    val locationPermissionState = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )
    val locationGranted = locationPermissionState.allPermissionsGranted
    LaunchedEffect(locationGranted) {
        if (locationGranted) viewModel.refreshAreaName()
    }

    val density = LocalDensity.current
    val bubbleCanvas = remember(topics) { computeBubbleLayouts(topics) }

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
            if (areaName == "Your area") {
                HeaderShimmer()
            } else {
                Text(
                    text = areaName,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Serif
                    ),
                    color = Color(0xFF1A1A1A)
                )
            }
            Spacer(Modifier.weight(1f))
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

        // ── Content area ──────────────────────────────────────
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (isLoading) {
                if (selectedTab == 0) BubbleShimmer() else TopicListShimmer()
            } else if (topics.isEmpty()) {
                val emptyMessage = when (selectedTab) {
                    2 -> "You haven't contributed to any topics yet.\nStart recording to join a discussion!"
                    else -> "No topics yet.\nRecord a thought to start one."
                }
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        emptyMessage,
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            } else if (selectedTab == 0) {
                val hScroll = rememberScrollState()
                val vScroll = rememberScrollState()

                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val viewportWidthPx = with(density) { maxWidth.toPx() }.toInt()
                val viewportHeightPx = with(density) { maxHeight.toPx() }.toInt()

                // Scroll so the bubble cluster is centered in the viewport
                LaunchedEffect(bubbleCanvas.canvasWidth, bubbleCanvas.canvasHeight) {
                    val canvasWidthPx = with(density) { bubbleCanvas.canvasWidth.dp.toPx() }.toInt()
                    val canvasHeightPx = with(density) { bubbleCanvas.canvasHeight.dp.toPx() }.toInt()
                    hScroll.scrollTo(((canvasWidthPx - viewportWidthPx) / 2).coerceAtLeast(0))
                    vScroll.scrollTo(((canvasHeightPx - viewportHeightPx) / 2).coerceAtLeast(0))
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(hScroll)
                        .verticalScroll(vScroll)
                ) {
                    Box(
                        modifier = Modifier.size(
                            width = bubbleCanvas.canvasWidth.dp,
                            height = bubbleCanvas.canvasHeight.dp
                        )
                    ) {
                        // Concentric guide circles centered in the canvas
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val center = Offset(size.width / 2, size.height / 2)
                            listOf(180.dp.toPx(), 260.dp.toPx(), 340.dp.toPx()).forEach { r ->
                                drawCircle(
                                    color = Color(0xFFE5E5E0).copy(alpha = 0.4f),
                                    radius = r,
                                    center = center,
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        }

                        bubbleCanvas.layouts.forEach { layout ->
                            Bubble(
                                topic = layout.topic,
                                sizeDp = layout.sizeDp,
                                colorIndex = layout.colorIndex,
                                x = layout.x,
                                y = layout.y,
                                onClick = {
                                    viewModel.onTopicTapped(layout.topic)
                                    onTopicClick(layout.topic)
                                }
                            )
                        }
                    }
                }
                } // BoxWithConstraints
            } else {
                TopicListView(
                    topics = topics,
                    isMyTopics = selectedTab == 2,
                    onTopicClick = onTopicClick
                )
            }

            // ── Mini-player (floats above the scroll area) ────────
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
                            val barWidth = 3.dp.toPx()
                            val spacing = 3.dp.toPx()
                            val color = Color(0xFF0F766E)
                            drawLine(color, Offset(0f, size.height * 0.3f), Offset(0f, size.height * 0.7f), barWidth, StrokeCap.Round)
                            drawLine(color, Offset(barWidth + spacing, size.height * 0.1f), Offset(barWidth + spacing, size.height * 0.9f), barWidth, StrokeCap.Round)
                            drawLine(color, Offset((barWidth + spacing) * 2, size.height * 0.4f), Offset((barWidth + spacing) * 2, size.height * 0.6f), barWidth, StrokeCap.Round)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text("Listening to ", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF1A1A1A))
                        Text(
                            playingTopicTitle ?: "…",
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
                            Icon(Icons.Default.Close, contentDescription = "Stop", tint = Color(0xFF4B5563), modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

// Radial packing — no topic limit, returns absolute canvas coords
private fun computeBubbleLayouts(topics: List<Topic>): BubbleCanvasData {
    if (topics.isEmpty()) return BubbleCanvasData(emptyList(), 400f, 400f)

    val sorted = topics.sortedByDescending { it.voiceCount }
    val maxCount = sorted.maxOf { it.voiceCount }.coerceAtLeast(1L).toFloat()

    fun sizeFor(count: Long): Float {
        val ratio = count.toFloat() / maxCount
        val curved = Math.pow(ratio.toDouble(), 0.6).toFloat()
        return 90f + (160f - 90f) * curved
    }

    // Center-relative placements: (centerOffset, radius)
    val placed = mutableListOf<Pair<Offset, Float>>()
    val centerRelative = mutableListOf<Triple<Offset, Float, Int>>() // offset, size, index

    sorted.forEachIndexed { index, topic ->
        val size = sizeFor(topic.voiceCount)
        val radius = size / 2f
        var placedOffset = Offset.Zero

        if (index == 0) {
            placed.add(Pair(Offset.Zero, radius))
        } else {
            var found = false
            var distance = radius + placed[0].second // start touching the first bubble
            val angleStart = index * 47f

            while (!found) {
                var angle = angleStart
                var attempts = 0
                while (attempts < 24 && !found) {
                    val rad = Math.toRadians(angle.toDouble())
                    val testX = distance * cos(rad).toFloat()
                    val testY = distance * sin(rad).toFloat()
                    val test = Offset(testX, testY)
                    val overlaps = placed.any { (pos, r) ->
                        val dx = test.x - pos.x
                        val dy = test.y - pos.y
                        dx * dx + dy * dy < (radius + r + 6f) * (radius + r + 6f)
                    }
                    if (!overlaps) {
                        placedOffset = test
                        found = true
                    } else {
                        angle += 15f
                        attempts++
                    }
                }
                if (!found) distance += 8f
            }
            placed.add(Pair(placedOffset, radius))
        }
        centerRelative.add(Triple(placedOffset, size, index))
    }

    // Convert center-relative → absolute top-left, with padding
    val pad = 48f
    val minX = centerRelative.minOf { (o, s, _) -> o.x - s / 2f } - pad
    val minY = centerRelative.minOf { (o, s, _) -> o.y - s / 2f } - pad
    val maxX = centerRelative.maxOf { (o, s, _) -> o.x + s / 2f } + pad
    val maxY = centerRelative.maxOf { (o, s, _) -> o.y + s / 2f } + pad

    val layouts = sorted.mapIndexed { index, topic ->
        val (offset, size, _) = centerRelative[index]
        BubbleLayout(
            topic = topic,
            sizeDp = size,
            x = offset.x - size / 2f - minX,
            y = offset.y - size / 2f - minY,
            colorIndex = index
        )
    }

    return BubbleCanvasData(layouts, maxX - minX, maxY - minY)
}

@Composable
private fun Bubble(
    topic: Topic,
    sizeDp: Float,
    colorIndex: Int,
    x: Float,
    y: Float,
    onClick: () -> Unit
) {
    val bubbleColors = listOf(BubbleTeal, BubbleBlue, BubbleWhite, BubbleCoral, BubbleYellow)
    val textColors = listOf(TextTeal, TextBlue, TextDark, TextCoral, TextYellow)
    val safeIndex = colorIndex % bubbleColors.size
    val color = bubbleColors[safeIndex]
    val textColor = textColors[safeIndex]
    val size = sizeDp.dp

    Box(
        modifier = Modifier
            .offset(x = x.dp, y = y.dp)
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
                tint = textColor,
                modifier = Modifier.size(if (sizeDp < 110f) 18.dp else 26.dp)
            )
            Text(
                text = topic.title,
                style = if (sizeDp < 110f)
                    MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium)
                else
                    MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium, fontFamily = FontFamily.Serif),
                textAlign = TextAlign.Center,
                color = textColor,
                lineHeight = if (sizeDp < 110f) 14.sp else 20.sp,
                maxLines = 2
            )
            if (sizeDp > 110f) {
                Text(
                    text = voiceCountLabel(topic.voiceCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = textColor.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun TopicListView(
    topics: List<Topic>,
    isMyTopics: Boolean,
    onTopicClick: (Topic) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(topics, key = { it.id }) { topic ->
            TopicListCard(topic = topic, isMyTopics = isMyTopics, onClick = { onTopicClick(topic) })
        }
    }
}

@Composable
private fun TopicListCard(topic: Topic, isMyTopics: Boolean, onClick: () -> Unit) {
    val icon = categoryIcon(topic.title)
    val iconBg = Color(0xFFD1F2DF)
    val iconTint = Color(0xFF166534)
    val age = remember(topic.createdAt) { relativeTime(topic.createdAt) }

    Surface(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(22.dp))
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = topic.title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFF1A1A1A),
                    maxLines = 2
                )
                Spacer(Modifier.height(4.dp))
                if (isMyTopics) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFD1F2DF)
                    ) {
                        Text(
                            "Contributed",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = Color(0xFF166534),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (topic.voiceCount > 0) {
                        Text(
                            "${topic.voiceCount} ${if (topic.voiceCount == 1L) "person" else "people"} talking",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF666666),
                            maxLines = 1
                        )
                        Text("·", style = MaterialTheme.typography.bodySmall, color = Color(0xFFBBBBBB))
                    }
                    Text(age, style = MaterialTheme.typography.bodySmall, color = Color(0xFF999999), maxLines = 1)
                }
            }

            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun TopicListShimmer() {
    val transition = rememberInfiniteTransition(label = "list_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "shimmer_x"
    )
    val brush = Brush.linearGradient(
        colors = listOf(Color(0xFFE8E8E8), Color(0xFFF5F5F5), Color(0xFFE8E8E8)),
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 0f)
    )
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        userScrollEnabled = false
    ) {
        items(6) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(brush))
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth(0.65f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth(0.4f).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    }
                }
            }
        }
    }
}

private fun relativeTime(createdAt: String): String {
    return try {
        val then = ZonedDateTime.parse(createdAt).withZoneSameInstant(IST)
        val now = ZonedDateTime.now(IST)
        val minutes = ChronoUnit.MINUTES.between(then, now)
        when {
            minutes < 60 -> "${minutes}m ago"
            minutes < 1440 -> "${minutes / 60}h ago"
            minutes < 10080 -> "${minutes / 1440}d ago"
            else -> "${minutes / 10080}w ago"
        }
    } catch (e: Exception) { "" }
}

@Composable
private fun HeaderShimmer() {
    val transition = rememberInfiniteTransition(label = "header_shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "header_translate"
    )
    val brush = Brush.linearGradient(
        colors = listOf(Color(0xFFE0E0E0), Color(0xFFF5F5F5), Color(0xFFE0E0E0)),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(brush)
    )
}

@Composable
private fun BubbleShimmer() {
    val transition = rememberInfiniteTransition(label = "bubble_shimmer")
    val alpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bubble_alpha"
    )

    // Ghost bubbles: (offsetX, offsetY, size) in dp relative to center
    val ghosts = listOf(
        Triple(0f, 0f, 160f),
        Triple(140f, -60f, 110f),
        Triple(-130f, 80f, 120f),
        Triple(60f, 150f, 90f),
        Triple(-80f, -140f, 100f),
        Triple(170f, 100f, 80f),
    )

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        // Concentric guide circles (same as real layout)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            listOf(180.dp.toPx(), 260.dp.toPx(), 340.dp.toPx()).forEach { r ->
                drawCircle(
                    color = Color(0xFFE5E5E0).copy(alpha = 0.4f),
                    radius = r,
                    center = center,
                    style = Stroke(width = 1.dp.toPx())
                )
            }
        }

        ghosts.forEach { (x, y, size) ->
            Box(
                modifier = Modifier
                    .offset(x = x.dp, y = y.dp)
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE0E0E0).copy(alpha = alpha))
            )
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
    count == 0L -> "Be the first"
    count == 1L -> "1 person"
    count < 1000L -> "$count people"
    else -> "${count / 1000}k people"
}
