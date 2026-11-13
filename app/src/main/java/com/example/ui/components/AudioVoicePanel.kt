package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AudioTrackOption
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun AudioVoicePanel(
    selectedTrack: AudioTrackOption,
    onTrackSelected: (AudioTrackOption) -> Unit,
    isPlayingMusic: Boolean,
    onToggleMusic: () -> Unit,
    bgmVolume: Float,
    onVolumeChanged: (Float) -> Unit,
    voicePitch: Float,
    onPitchChanged: (Float) -> Unit,
    voiceSpeed: Float,
    onSpeedChanged: (Float) -> Unit,
    onTestVoice: () -> Unit,
    onStopVoice: () -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .background(StudioSurface)
            .verticalScrollbar(lazyListState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Section: Background Music Tracks
        item {
            Text(
                text = "AUDIO STUDIO & SYNTHESIZER",
                color = NeonCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Real-time rhythmic chord progressions and synth beats",
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        // BGM Volume Slider
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioSurfaceElevated)
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Master BGM Volume", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(String.format(Locale.US, "%.0f%%", bgmVolume * 100), color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = bgmVolume,
                    onValueChange = onVolumeChanged,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                )
            }
        }

        // Track items
        items(AudioTrackOption.entries) { track ->
            val isSelected = track == selectedTrack
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isSelected) StudioSurfaceElevated else StudioSurface)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) NeonCyan else StudioBorder,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .clickable { onTrackSelected(track) }
                    .padding(10.dp)
                    .testTag("audio_track_${track.name}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) NeonCyan else StudioBorder),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSelected && isPlayingMusic) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = "Play Track",
                        tint = if (isSelected) Color.White else TextPrimary,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable {
                                if (isSelected) onToggleMusic() else onTrackSelected(track)
                            }
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(track.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(track.genre, color = NeonPurple, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        Text("•", color = TextMuted, fontSize = 10.sp)
                        Text("${track.bpm} BPM", color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
        }

        // Section: AI Voiceover SpeechSynthesis
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioSurfaceElevated)
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = "Voiceover", tint = NeonPink, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("AI Voiceover Engine (Android TTS)", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Voice Pitch
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Voice Pitch", color = TextSecondary, fontSize = 11.sp)
                    Text(String.format(Locale.US, "%.1fx", voicePitch), color = NeonPink, fontSize = 11.sp)
                }
                Slider(
                    value = voicePitch,
                    onValueChange = onPitchChanged,
                    valueRange = 0.5f..1.8f,
                    colors = SliderDefaults.colors(thumbColor = NeonPink, activeTrackColor = NeonPink)
                )

                // Voice Speed
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Speech Rate", color = TextSecondary, fontSize = 11.sp)
                    Text(String.format(Locale.US, "%.1fx", voiceSpeed), color = NeonPink, fontSize = 11.sp)
                }
                Slider(
                    value = voiceSpeed,
                    onValueChange = onSpeedChanged,
                    valueRange = 0.6f..1.6f,
                    colors = SliderDefaults.colors(thumbColor = NeonPink, activeTrackColor = NeonPink)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onTestVoice,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("test_voice_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPink)
                    ) {
                        Text("Preview Voiceover", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                    Button(
                        onClick = onStopVoice,
                        colors = ButtonDefaults.buttonColors(containerColor = StudioBorder)
                    ) {
                        Text("Stop", color = TextPrimary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
