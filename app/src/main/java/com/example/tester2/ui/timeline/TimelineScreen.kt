package com.example.tester2.ui.timeline

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tester2.data.model.VoiceNote
import com.example.tester2.ui.theme.HiveGreen
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val CreamBg = Color(0xFFF9F9F4)
private val CardWhite = Color(0xFFFFFFFF)
private val TextDark = Color(0xFF1C1C1C)
private val TextGray = Color(0xFF9E9E9E)

@Composable
fun TimelineScreen(
    onTopicClick: (String) -> Unit = {},
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val voiceNotes by viewModel.voiceNotes.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val playingUrl by viewModel.playingUrl.collectAsState()
    val areaName by viewModel.areaName.collectAsState()
    val isWeeklyReflectionPlaying by viewModel.isWeeklyReflectionPlaying.collectAsState()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CreamBg)
            .statusBarsPadding()
    ) {
        // Location header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = HiveGreen,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(4.dp))
            if (isLoading) {
                ShimmerBox(width = 100.dp, height = 14.dp, shape = RoundedCornerShape(4.dp))
            } else {
                Text(
                    text = areaName,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold
                    ),
                    color = TextDark
                )
            }
            Spacer(Modifier.weight(1f))
        }

        when {
            isLoading -> TimelineShimmer()
            voiceNotes.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No thoughts yet.\nStart recording!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextGray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
            else -> LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Weekly Reflection",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextDark,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                item {
                    WeeklyReflectionCard(
                        notes = voiceNotes,
                        isPlaying = isWeeklyReflectionPlaying,
                        onPlayClick = { viewModel.toggleWeeklyReflection() }
                    )
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Voice History",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold
                            ),
                            color = TextDark
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${voiceNotes.size} Recordings",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextGray
                        )
                    }
                }

                items(voiceNotes) { note ->
                    VoiceNoteCard(
                        note = note,
                        isPlaying = playingUrl == viewModel.getAudioUrl(note),
                        onPlayClick = { viewModel.toggleAudio(note) },
                        onTopicClick = onTopicClick
                    )
                }
            }
        }
    }
}

@Composable
private fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing)),
        label = "shimmer_translate"
    )
    return Brush.linearGradient(
        colors = listOf(Color(0xFFE8E8E8), Color(0xFFF5F5F5), Color(0xFFE8E8E8)),
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 0f)
    )
}

@Composable
private fun ShimmerBox(
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(6.dp)
) {
    Box(
        modifier = Modifier
            .width(width)
            .height(height)
            .clip(shape)
            .background(shimmerBrush())
    )
}

@Composable
private fun TimelineShimmer() {
    val brush = shimmerBrush()
    LazyColumn(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Weekly Reflection heading shimmer
        item {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(180.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(brush)
            )
        }

        // Weekly Reflection card shimmer
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = CardWhite,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Box(Modifier.width(120.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                            Spacer(Modifier.height(8.dp))
                            Box(Modifier.fillMaxWidth(0.75f).height(16.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                        }
                        Box(Modifier.size(40.dp).clip(RoundedCornerShape(50)).background(brush))
                    }
                    Spacer(Modifier.height(20.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(0.4f, 0.7f, 0.5f, 1f, 0.6f, 0.3f, 0.8f).forEach { frac ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height((frac * 56).dp)
                                    .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                    .background(brush)
                            )
                        }
                    }
                }
            }
        }

        // Voice History header shimmer
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.width(140.dp).height(20.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                Spacer(Modifier.weight(1f))
                Box(Modifier.width(80.dp).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
            }
        }

        // 3 VoiceNote card skeletons
        items(3) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = CardWhite,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(100.dp).height(10.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                        Spacer(Modifier.weight(1f))
                        Box(Modifier.size(20.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Spacer(Modifier.height(4.dp))
                    Box(Modifier.fillMaxWidth(0.8f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(brush))
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(32.dp).clip(CircleShape).background(brush))
                        Spacer(Modifier.width(10.dp))
                        Row(
                            modifier = Modifier.weight(1f).height(28.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            repeat(24) {
                                val h = (0.2f + (it % 5) * 0.15f)
                                Box(
                                    modifier = Modifier
                                        .width(3.dp)
                                        .height((h * 24).dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .background(brush)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WeeklyReflectionCard(
    notes: List<VoiceNote>,
    isPlaying: Boolean,
    onPlayClick: () -> Unit
) {
    // Compute per-day-of-week counts for the last 7 days
    val dayLabels = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    val dayCounts = remember(notes) {
        val counts = IntArray(7)
        notes.forEach { note ->
            try {
                val dow = ZonedDateTime.parse(note.createdAt).dayOfWeek.value - 1 // 0=Mon
                counts[dow]++
            } catch (_: Exception) {}
        }
        counts.toList()
    }
    val maxCount = dayCounts.max().coerceAtLeast(1)
    val peakDay = dayCounts.indexOfFirst { it == dayCounts.max() }
    val peakDayName = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
        .getOrElse(peakDay) { "today" }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CardWhite,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "ACTIVITY OVERVIEW",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.5.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextGray
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Your voice energy was high on $peakDayName.",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                        color = TextDark
                    )
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(HiveGreen),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onPlayClick, modifier = Modifier.size(40.dp)) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Bar chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dayCounts.forEachIndexed { i, count ->
                    val fraction = count.toFloat() / maxCount
                    val barHeight = (fraction * 56.dp.value).coerceAtLeast(6f)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight.dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(if (i == peakDay) HiveGreen else Color(0xFFE8E8E8))
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            // Day labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                dayLabels.forEach { label ->
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = TextGray,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun VoiceNoteCard(
    note: VoiceNote,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onTopicClick: (String) -> Unit
) {
    val waveformHeights = remember(note.id) {
        val seed = note.id.hashCode()
        val rng = java.util.Random(seed.toLong())
        (0 until 24).map { 0.2f + rng.nextFloat() * 0.8f }
    }

    val derivedTitle = note.topicTitle
        ?: note.transcript?.take(60)?.let { if (it.length == 60) "$it…" else it }
        ?: "Voice note"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = CardWhite,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Timestamp + three-dot menu row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatNoteTimestamp(note.createdAt),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = TextGray
                )
                Spacer(Modifier.weight(1f))
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.height(6.dp))

            // Bold title
            Text(
                text = derivedTitle,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = TextDark
            )

            Spacer(Modifier.height(4.dp))

            // Transcript quote
            when {
                note.transcript != null -> Text(
                    text = "\"${note.transcript}\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark,
                    maxLines = 3
                )
                else -> Text(
                    "Transcribing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.LightGray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(Modifier.height(12.dp))

            // Play button + waveform row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play / Pause button
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) HiveGreen else Color(0xFFEEEEEE))
                        .clickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isPlaying) Color.White else TextGray,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                // Waveform bars
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    waveformHeights.forEach { h ->
                        val barH = (h * 24).dp
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height(barH)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isPlaying) HiveGreen else Color(0xFFD0D0D0))
                        )
                    }
                }
            }

            // Topic chip — shown when clip was shared to a community topic
            if (note.classification == "community" && note.topicTitle != null) {
                Spacer(Modifier.height(10.dp))
                val chipModifier = if (note.topicId != null) {
                    Modifier.clickable { onTopicClick(note.topicId) }
                } else {
                    Modifier
                }
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = HiveGreen.copy(alpha = 0.1f),
                    modifier = chipModifier
                ) {
                    Text(
                        "#${note.topicTitle}",
                        style = MaterialTheme.typography.labelMedium.copy(color = HiveGreen),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

private fun formatNoteTimestamp(createdAt: String): String {
    return try {
        val zdt = ZonedDateTime.parse(createdAt)
        val now = ZonedDateTime.now()
        val minutesAgo = java.time.Duration.between(zdt, now).toMinutes()
        val timeStr = zdt.format(DateTimeFormatter.ofPattern("h:mm a"))
        when {
            minutesAgo < 1440 -> "TODAY • $timeStr"
            minutesAgo < 2880 -> "YESTERDAY • $timeStr"
            else -> zdt.format(DateTimeFormatter.ofPattern("MMM d")).uppercase() + " • $timeStr"
        }
    } catch (e: Exception) {
        createdAt.take(10)
    }
}

