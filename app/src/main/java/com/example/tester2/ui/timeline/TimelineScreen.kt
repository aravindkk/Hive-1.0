package com.example.tester2.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.tester2.data.model.VoiceNote
import com.example.tester2.ui.theme.HiveGreen
import com.example.tester2.ui.theme.HiveWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TimelineScreen(
    viewModel: TimelineViewModel = hiltViewModel()
) {
    val voiceNotes by viewModel.voiceNotes.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(HiveWhite)
    ) {
        if (voiceNotes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No thoughts yet. Start recording!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        text = "My Thoughts",
                        style = MaterialTheme.typography.displayMedium,
                        color = HiveGreen,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                }
                items(voiceNotes) { note ->
                    VoiceNoteItem(note)
                }
            }
        }
    }
}

@Composable
fun VoiceNoteItem(note: VoiceNote) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { /* TODO: Playback */ },
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(HiveGreen.copy(alpha = 0.1f))
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play",
                    tint = HiveGreen
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = formatDate(note.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (note.transcript != null) {
                    Text(
                        text = note.transcript,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2
                    )
                } else {
                    Text(
                        text = "Processing...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.LightGray,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }
            }
        }
    }
}

private fun formatDate(dateString: String): String {
    // Basic parsing, assuming ISO or similar. Supabase returns ISO 8601
    // Ideally use Instant or stricter parser.
    return try {
        // Z format for ISO 8601
        // Simplified for now
        dateString.take(10) + " " + dateString.substring(11, 16)
    } catch (e: Exception) {
        dateString
    }
}
