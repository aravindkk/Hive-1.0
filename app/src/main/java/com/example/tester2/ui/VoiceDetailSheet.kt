package com.example.tester2.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tester2.data.model.VoiceNote
import com.example.tester2.ui.theme.HiveGreen
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private val IST = ZoneId.of("Asia/Kolkata")
private val SheetTextDark = Color(0xFF1C1C1C)
private val SheetTextGray = Color(0xFF9E9E9E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceDetailSheet(
    voice: VoiceNote,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val waveformHeights = remember(voice.id) {
        val rng = java.util.Random(voice.id.hashCode().toLong())
        (0 until 30).map { 0.2f + rng.nextFloat() * 0.8f }
    }
    val displayName = voice.username
    val avatarColor = remember(displayName) {
        val colors = listOf(
            Color(0xFFBBDEFB), Color(0xFFC8E6C9), Color(0xFFFFCCBC),
            Color(0xFFF8BBD0), Color(0xFFE1BEE7), Color(0xFFFFF9C4)
        )
        if (displayName != null) colors[Math.abs(displayName.hashCode()) % colors.size]
        else Color(0xFFE8E8E8)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFFF9F9F4),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header: avatar + username + timestamp
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (displayName != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(avatarColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sheetInitialsFrom(displayName),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF555555)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "@$displayName",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = SheetTextDark
                        )
                        Text(
                            text = sheetFormatTimestamp(voice.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = SheetTextGray
                        )
                    }
                } else {
                    Text(
                        text = sheetFormatTimestamp(voice.createdAt),
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = SheetTextGray
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // Full transcript
            if (voice.transcript != null) {
                Text(
                    text = "\"${voice.transcript}\"",
                    style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 28.sp),
                    color = SheetTextDark
                )
            } else {
                Text(
                    "Transcribing...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SheetTextGray,
                    fontStyle = FontStyle.Italic
                )
            }

            Spacer(Modifier.height(24.dp))

            // Player row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isPlaying) HiveGreen else Color(0xFFEEEEEE))
                        .clickable { onPlayClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = if (isPlaying) Color.White else SheetTextGray,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    waveformHeights.forEach { h ->
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .height((h * 28).dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(if (isPlaying) HiveGreen else Color(0xFFD0D0D0))
                        )
                    }
                }
            }
        }
    }
}

private fun sheetInitialsFrom(name: String): String {
    val uppers = name.filter { it.isUpperCase() }
    return if (uppers.length >= 2) uppers.take(2) else name.take(2).uppercase()
}

private fun sheetFormatTimestamp(createdAt: String): String {
    return try {
        val zdt = ZonedDateTime.parse(createdAt).withZoneSameInstant(IST)
        val now = ZonedDateTime.now(IST)
        val minutesAgo = java.time.Duration.between(zdt, now).toMinutes()
        val timeStr = zdt.format(DateTimeFormatter.ofPattern("h:mm a"))
        when {
            minutesAgo < 1 -> "just now"
            minutesAgo < 60 -> "${minutesAgo}m ago · $timeStr"
            minutesAgo < 1440 -> "${minutesAgo / 60}h ago · $timeStr"
            else -> zdt.format(DateTimeFormatter.ofPattern("MMM d, yyyy")) + " · $timeStr"
        }
    } catch (e: Exception) {
        createdAt.take(10)
    }
}
