package com.example.tester2.ui.hive

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.tester2.data.model.Topic
import com.example.tester2.data.model.VoiceNote
import com.example.tester2.ui.theme.HiveGreen
import com.example.tester2.ui.timeline.VoiceNoteItem

@Composable
fun TopicDetailSheet(
    topic: Topic,
    voiceNotes: List<VoiceNote>,
    playingUrl: String?,
    onContributeClick: (String) -> Unit,
    onPlayClick: (VoiceNote) -> Unit,
    getAudioUrl: (VoiceNote) -> String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.White)
    ) {
        Text(
            text = topic.title,
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Text(
            text = "${voiceNotes.size} contributions",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        Button(
            onClick = { onContributeClick(topic.id) },
            colors = ButtonDefaults.buttonColors(containerColor = HiveGreen),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Text("Contribute to this Topic")
        }
        
        if (voiceNotes.isEmpty()) {
            Text("No voices yet. Be the first!", modifier = Modifier.padding(vertical = 32.dp))
        } else {
            LazyColumn {
                items(voiceNotes) { voiceNote ->
                    val isPlaying = playingUrl == getAudioUrl(voiceNote)
                    VoiceNoteItem(
                        note = voiceNote,
                        isPlaying = isPlaying,
                        onPlayClick = { onPlayClick(voiceNote) }
                    )
                }
            }
        }
    }
}
