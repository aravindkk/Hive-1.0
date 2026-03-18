package com.example.tester2.ui.hive

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tester2.ui.VoiceDetailSheet
import com.example.tester2.data.model.SummarySegment
import com.example.tester2.data.model.TopicSummary
import com.example.tester2.data.model.VoiceNote
import com.example.tester2.ui.theme.HiveGreen
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val IST = ZoneId.of("Asia/Kolkata")

private val DeepDiveBg = Color(0xFFF9F9F4)
private val TextDark = Color(0xFF1C1C1C)
private val TextGray = Color(0xFF9E9E9E)

@Composable
fun TopicDeepDiveScreen(
    topicId: String,
    onBackClick: () -> Unit,
    onSpeakClick: (topicId: String, topicTitle: String) -> Unit,
    viewModel: TopicDeepDiveViewModel = hiltViewModel()
) {
    // Fix: drive data loading from the composable parameter, not SavedStateHandle
    LaunchedEffect(topicId) {
        viewModel.loadForTopic(topicId)
    }

    val topic by viewModel.topic.collectAsState()
    val voices by viewModel.voices.collectAsState()
    val summary by viewModel.summary.collectAsState()
    val summaryGenerating by viewModel.summaryGenerating.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val playingUrl by viewModel.playingUrl.collectAsState()
    val currentPositionMs by viewModel.currentPositionMs.collectAsState()
    var selectedVoice by remember { mutableStateOf<com.example.tester2.data.model.VoiceNote?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepDiveBg)
    ) {
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = onBackClick) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextDark)
            }
            if (isLoading) {
                ShimmerBox(modifier = Modifier.width(160.dp).height(20.dp).clip(RoundedCornerShape(6.dp)))
            } else {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic?.title ?: "",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextDark,
                        maxLines = 1
                    )
                    val count = topic?.voiceCount ?: voices.size.toLong()
                    Text(
                        text = "$count ${if (count == 1L) "voice" else "voices"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextGray
                    )
                }
            }
        }

        if (isLoading) {
            TopicDeepDiveShimmer()
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // AI Summary section — auto-trigger when voices exist but no summary yet
                item {
                    val hasVoices = voices.isNotEmpty()
                    if (summary == null && hasVoices && !summaryGenerating) {
                        LaunchedEffect(topicId) {
                            viewModel.triggerSummaryGeneration(topicId)
                        }
                    }
                    AiSummarySection(
                        summary = summary,
                        isGenerating = summaryGenerating,
                        hasVoices = hasVoices,
                        playingUrl = playingUrl,
                        currentPositionMs = currentPositionMs,
                        onPlayToggle = { url -> viewModel.toggleAudio(url) },
                        onRetry = { viewModel.triggerSummaryGeneration(topicId) },
                        getAudioUrl = { path -> viewModel.getSummaryAudioUrl(path) }
                    )
                }

                // Community voices header
                if (voices.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Community Voices",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = TextDark
                            )
                            Spacer(Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = Color(0xFFF0F0F0)
                            ) {
                                Text(
                                    "Newest",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    items(voices) { voice ->
                        val audioUrl = viewModel.getAudioUrl(voice.storagePath)
                        CommunityVoiceCard(
                            voice = voice,
                            isPlaying = playingUrl == audioUrl,
                            onPlayToggle = { viewModel.toggleAudio(audioUrl) },
                            onCardClick = { selectedVoice = voice }
                        )
                    }
                } else {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No voices yet. Be the first to speak.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextGray
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
        }

        Button(
            onClick = { onSpeakClick(topicId, topic?.title ?: "") },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(containerColor = HiveGreen)
        ) {
            Text(
                "Add your voice",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = TextDark
            )
        }
    }

    selectedVoice?.let { voice ->
        val audioUrl = viewModel.getAudioUrl(voice.storagePath)
        VoiceDetailSheet(
            voice = voice,
            isPlaying = playingUrl == audioUrl,
            onPlayClick = { viewModel.toggleAudio(audioUrl) },
            onDismiss = {
                viewModel.stopAudio()
                selectedVoice = null
            }
        )
    }
}

@Composable
private fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color(0xFFE8E8E8),
        Color(0xFFF5F5F5),
        Color(0xFFE8E8E8),
    )
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 0f)
    )
}

@Composable
private fun ShimmerBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.background(shimmerBrush()))
}

@Composable
private fun TopicDeepDiveShimmer() {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // AI summary card shimmer
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    ShimmerBox(modifier = Modifier.width(80.dp).height(12.dp).clip(RoundedCornerShape(4.dp)))
                    Spacer(Modifier.height(4.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.85f).height(14.dp).clip(RoundedCornerShape(4.dp)))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.7f).height(14.dp).clip(RoundedCornerShape(4.dp)))
                    Spacer(Modifier.height(4.dp))
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(4.dp)))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f).height(14.dp).clip(RoundedCornerShape(4.dp)))
                }
            }
        }

        // Section header shimmer
        item {
            ShimmerBox(modifier = Modifier.width(140.dp).height(16.dp).clip(RoundedCornerShape(4.dp)))
        }

        // Voice card shimmers
        items(3) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ShimmerBox(modifier = Modifier.size(36.dp).clip(CircleShape))
                        Spacer(Modifier.width(10.dp))
                        ShimmerBox(modifier = Modifier.width(80.dp).height(12.dp).clip(RoundedCornerShape(4.dp)))
                    }
                    ShimmerBox(modifier = Modifier.fillMaxWidth().height(13.dp).clip(RoundedCornerShape(4.dp)))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.75f).height(13.dp).clip(RoundedCornerShape(4.dp)))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.9f).height(13.dp).clip(RoundedCornerShape(4.dp)))
                    ShimmerBox(modifier = Modifier.fillMaxWidth(0.5f).height(24.dp).clip(RoundedCornerShape(8.dp)))
                }
            }
        }
    }
}

@Composable
private fun AiSummarySection(
    summary: TopicSummary?,
    isGenerating: Boolean,
    hasVoices: Boolean,
    playingUrl: String?,
    currentPositionMs: Long,
    onPlayToggle: (String) -> Unit,
    onRetry: () -> Unit,
    getAudioUrl: (String) -> String
) {
    val summaryAudioUrl = summary?.audioPath?.let { getAudioUrl(it) }
    val isSummaryPlaying = summaryAudioUrl != null && playingUrl == summaryAudioUrl

    val activeSegmentIndex = if (isSummaryPlaying && summary != null) {
        summary.segments.indexOfLast { it.startMs <= currentPositionMs }.coerceAtLeast(0)
    } else -1

    val segmentListState = rememberLazyListState()
    val screenHeightDp = LocalConfiguration.current.screenHeightDp

    // Auto-scroll to active segment while playing
    LaunchedEffect(activeSegmentIndex) {
        if (isSummaryPlaying && activeSegmentIndex > 0) {
            segmentListState.animateScrollToItem(activeSegmentIndex)
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "AI SUMMARY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = HiveGreen,
                    modifier = Modifier.weight(1f)
                )
                if (summary != null) {
                    IconButton(
                        onClick = onRetry,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Regenerate",
                            tint = TextGray,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            when {
                // Waiting for auto-generated summary or no voices yet
                summary == null -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (isGenerating || hasVoices) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = HiveGreen,
                                strokeWidth = 2.dp
                            )
                            Text(
                                "Generating AI summary…",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray
                            )
                        } else {
                            Text(
                                "Summary will appear once voices are added.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray
                            )
                        }
                    }
                }

                else -> {
                    // Play button row — only shown when audio is available
                    if (summaryAudioUrl != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(HiveGreen),
                                contentAlignment = Alignment.Center
                            ) {
                                IconButton(onClick = { onPlayToggle(summaryAudioUrl) }) {
                                    Icon(
                                        imageVector = if (isSummaryPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Play Summary",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = TextDark
                                )
                                val durationText = if (summary.durationSeconds > 0) {
                                    val mins = (summary.durationSeconds / 60).toInt()
                                    val secs = (summary.durationSeconds % 60).toInt()
                                    "%d:%02d".format(mins, secs)
                                } else "~${summary.segments.size * 5}s"
                                Text(
                                    durationText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextGray
                                )
                            }
                        }
                    } else {
                        // Summary text exists but no audio — waiting on TTS or generation still running
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = HiveGreen,
                                strokeWidth = 2.dp
                            )
                            Text(
                                "Generating audio…",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextGray
                            )
                        }
                    }

                    if (summary.segments.isNotEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        LazyColumn(
                            state = segmentListState,
                            modifier = Modifier.heightIn(max = (screenHeightDp * 0.3f).dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            itemsIndexed(summary.segments) { index, segment ->
                                LyricsSegmentRow(
                                    segment = segment,
                                    isActive = index == activeSegmentIndex,
                                    isPlaying = isSummaryPlaying
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
private fun LyricsSegmentRow(
    segment: SummarySegment,
    isActive: Boolean,
    isPlaying: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = segment.text,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                    lineHeight = 22.sp
                ),
                color = when {
                    isActive -> TextDark
                    isPlaying -> TextGray.copy(alpha = 0.5f)
                    else -> TextGray
                }
            )

            // Attribution bubbles
            if (segment.attributedTo.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                    segment.attributedTo.take(3).forEachIndexed { i, username ->
                        val avatarColor = remember(username) {
                            val colors = listOf(
                                Color(0xFFBBDEFB), Color(0xFFC8E6C9), Color(0xFFFFCCBC),
                                Color(0xFFF8BBD0), Color(0xFFE1BEE7), Color(0xFFFFF9C4)
                            )
                            colors[Math.abs(username.hashCode()) % colors.size]
                        }
                        Box(
                            modifier = Modifier
                                .size(20.dp)
                                .clip(CircleShape)
                                .background(avatarColor)
                                .border(1.dp, Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initialsFrom(username),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp),
                                color = Color(0xFF555555)
                            )
                        }
                    }
                }
            }
        }

        if (isActive) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(HiveGreen)
                    .align(Alignment.CenterVertically)
            )
        }
    }
}

@Composable
private fun CommunityVoiceCard(
    voice: VoiceNote,
    isPlaying: Boolean,
    onPlayToggle: () -> Unit,
    onCardClick: () -> Unit = {}
) {
    val waveformHeights = remember(voice.id) {
        val seed = voice.id.hashCode()
        val rng = java.util.Random(seed.toLong())
        (0 until 20).map { 0.2f + rng.nextFloat() * 0.8f }
    }

    val displayName = voice.username ?: voice.userId
    val avatarColor = remember(displayName) {
        val colors = listOf(
            Color(0xFFBBDEFB), Color(0xFFC8E6C9), Color(0xFFFFCCBC),
            Color(0xFFF8BBD0), Color(0xFFE1BEE7), Color(0xFFFFF9C4)
        )
        colors[Math.abs(displayName.hashCode()) % colors.size]
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(avatarColor),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initialsFrom(displayName),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF555555)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = formatTimestamp(voice.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = TextGray
                )
            }

            Spacer(Modifier.height(10.dp))

            voice.transcript?.let { transcript ->
                Text(
                    text = "\"$transcript\"",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextDark,
                    maxLines = 4
                )
                Spacer(Modifier.height(10.dp))
            } ?: Text(
                "Transcribing...",
                style = MaterialTheme.typography.bodySmall,
                color = Color.LightGray,
                fontStyle = FontStyle.Italic
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onPlayToggle,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(HiveGreen.copy(alpha = 0.12f))
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = HiveGreen,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    waveformHeights.forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((h * 20).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isPlaying) HiveGreen else Color(0xFFD8D8D8))
                        )
                    }
                }
            }
        }
    }
}

// "SilentFox" → "SF", "ElectricStar" → "ES", fallback to first 2 chars
private fun initialsFrom(name: String): String {
    val uppers = name.filter { it.isUpperCase() }
    return if (uppers.length >= 2) uppers.take(2) else name.take(2).uppercase()
}

private fun formatTimestamp(createdAt: String): String {
    return try {
        val zdt = ZonedDateTime.parse(createdAt).withZoneSameInstant(IST)
        val now = ZonedDateTime.now(IST)
        val minutesAgo = java.time.Duration.between(zdt, now).toMinutes()
        when {
            minutesAgo < 1 -> "just now"
            minutesAgo < 60 -> "${minutesAgo}m ago"
            minutesAgo < 1440 -> "${minutesAgo / 60}h ago"
            else -> zdt.format(DateTimeFormatter.ofPattern("MMM d"))
        }
    } catch (e: Exception) {
        createdAt.take(10)
    }
}
