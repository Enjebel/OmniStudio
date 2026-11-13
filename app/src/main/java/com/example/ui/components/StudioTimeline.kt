package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TimelineClip
import com.example.model.TimelineTrackType
import com.example.ui.theme.NeonGradient
import com.example.ui.theme.NeonPink
import com.example.ui.theme.OmniTheme
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import java.util.Locale

@Composable
fun StudioTimeline(
    clips: List<TimelineClip>,
    selectedClipId: String? = null,
    playheadSec: Float,
    totalDurationSec: Float,
    isPlaying: Boolean,
    onTogglePlay: () -> Unit,
    onSeekPlayhead: (Float) -> Unit,
    onSelectClip: (String) -> Unit = {},
    onSplitClip: () -> Unit = {},
    onTrimClip: () -> Unit = {},
    onUpdateClip: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OmniTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .border(1.dp, StudioBorder)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Top Transport & Cut Controls Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(
                            if (isPlaying) {
                                Brush.radialGradient(listOf(NeonPink, Color(0xFFC026D3)))
                            } else {
                                NeonGradient
                            }
                        )
                        .clickable { onTogglePlay() }
                        .testTag("play_pause_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                val currentFormatted = String.format(Locale.US, "%02d:%04.1f", (playheadSec / 60).toInt(), playheadSec % 60)
                val totalFormatted = String.format(Locale.US, "%02d:%02.0f", (totalDurationSec / 60).toInt(), totalDurationSec % 60)

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = currentFormatted,
                        color = palette.accentCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = " / $totalFormatted",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // CapCut-style Split & Trim Action Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioSurfaceElevated)
                        .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                        .clickable { onSplitClip() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("timeline_split_btn"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = "Split", tint = palette.accentCyan, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Split", color = palette.accentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioSurfaceElevated)
                        .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                        .clickable { onTrimClip() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("timeline_trim_btn"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Trim", tint = palette.accentPurple, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Trim -0.5s", color = palette.accentPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Hybrid Multi-Track Scrubber
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(98.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(StudioSurfaceElevated)
                .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
        ) {
            val timelineWidth = maxWidth
            val widthPx = constraints.maxWidth.toFloat()

            val rulerBg = palette.timelineRulerBg
            val majorTick = palette.accentCyan.copy(alpha = 0.85f)
            val minorTick = palette.timelineRulerTick

            Column(modifier = Modifier.fillMaxSize()) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(18.dp)
                        .background(rulerBg)
                        .pointerInput(totalDurationSec) {
                            detectDragGestures { change, _ ->
                                change.consume()
                                val seekRatio = (change.position.x / widthPx).coerceIn(0f, 1f)
                                onSeekPlayhead(seekRatio * totalDurationSec)
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val numSeconds = totalDurationSec.toInt()
                    for (sec in 0..numSeconds) {
                        val x = (sec / totalDurationSec) * w
                        val isMajor = sec % 1 == 0
                        drawLine(
                            color = if (isMajor) majorTick else minorTick,
                            start = Offset(x, if (isMajor) 0f else h * 0.5f),
                            end = Offset(x, h),
                            strokeWidth = if (isMajor) 1.5f else 1f
                        )
                    }
                }

                val tracks = listOf(
                    TimelineTrackType.PRODUCT,
                    TimelineTrackType.TEXT,
                    TimelineTrackType.AUDIO,
                    TimelineTrackType.EFFECT
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 3.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    for (track in tracks) {
                        val trackClips = clips.filter { it.trackType == track }
                        TrackRow(
                            track = track,
                            clips = trackClips,
                            selectedClipId = selectedClipId,
                            totalDurationSec = totalDurationSec,
                            onSelectClip = onSelectClip,
                            onUpdateClip = onUpdateClip
                        )
                    }
                }
            }

            // Glowing Playhead
            val playheadRatio = (playheadSec / totalDurationSec).coerceIn(0f, 1f)
            val playheadX = (timelineWidth * playheadRatio)

            Box(
                modifier = Modifier
                    .offset(x = playheadX - 6.dp)
                    .width(12.dp)
                    .fillMaxHeight()
                    .pointerInput(totalDurationSec) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val seekRatio = (change.position.x / widthPx).coerceIn(0f, 1f)
                            onSeekPlayhead(seekRatio * totalDurationSec)
                        }
                    }
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(NeonPink)
                        .border(1.dp, Color.White, CircleShape)
                        .align(Alignment.TopCenter)
                )
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(NeonPink)
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

@Composable
fun TrackRow(
    track: TimelineTrackType,
    clips: List<TimelineClip>,
    selectedClipId: String?,
    totalDurationSec: Float,
    onSelectClip: (String) -> Unit,
    onUpdateClip: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(16.dp)
            .padding(horizontal = 4.dp)
    ) {
        val totalWidth = maxWidth

        val isDark = OmniTheme.colors.isDark
        val selectedBorderColor = TextPrimary

        clips.forEach { clip ->
            val startRatio = (clip.startTimeSec / totalDurationSec).coerceIn(0f, 0.95f)
            val durRatio = (clip.durationSec / totalDurationSec).coerceIn(0.08f, 1f - startRatio)
            val clipWidth = totalWidth * durRatio
            val clipOffset = totalWidth * startRatio
            val isSelected = clip.id == selectedClipId
            val bgAlpha = if (isDark) {
                if (isSelected) 0.58f else 0.36f
            } else {
                if (isSelected) 0.95f else 0.82f
            }

            Row(
                modifier = Modifier
                    .offset(x = clipOffset)
                    .width(clipWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(track.color.copy(alpha = bgAlpha))
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) selectedBorderColor else track.color,
                        shape = RoundedCornerShape(4.dp)
                    )
                    .clickable { onSelectClip(clip.id) }
                    .padding(horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (clip.isAiGenerated) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(8.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                    }
                    Text(
                        text = clip.name,
                        color = Color.White,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Text(
                    text = String.format(Locale.US, "%.1fs", clip.durationSec),
                    color = Color.White.copy(alpha = 0.88f),
                    fontSize = 7.sp
                )
            }
        }
    }
}
