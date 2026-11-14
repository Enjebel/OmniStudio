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
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TimelineClip
import com.example.model.TimelineTrackType
import com.example.model.TransitionEffect
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGradient
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OmniTheme
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
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
    onApplyTransitionToClip: (clipId: String?, effect: TransitionEffect, durationSec: Float) -> Unit = { _, _, _ -> },
    onOpenTransitions: () -> Unit = {},
    onUpdateClip: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OmniTheme.colors
    var showToolbarTransitionMenu by remember { mutableStateOf(false) }

    val layerClips = remember(clips) {
        clips.filter { it.trackType == TimelineTrackType.PRODUCT || it.trackType == TimelineTrackType.TEXT }
    }
    val targetMenuClip = remember(layerClips, selectedClipId) {
        layerClips.find { it.id == selectedClipId }
            ?: layerClips.lastOrNull { it.startTimeSec > 0.05f }
            ?: layerClips.firstOrNull()
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .border(1.dp, StudioBorder)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        // Top Transport & Cut / Transition Controls Bar
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

            // CapCut-style Split, Trim & Transition Selection Menu Pills
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioSurfaceElevated)
                        .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                        .clickable { onSplitClip() }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("timeline_split_btn"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ContentCut, contentDescription = "Split", tint = palette.accentCyan, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Split", color = palette.accentCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioSurfaceElevated)
                        .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                        .clickable { onTrimClip() }
                        .padding(horizontal = 7.dp, vertical = 4.dp)
                        .testTag("timeline_trim_btn"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Tune, contentDescription = "Trim", tint = palette.accentPurple, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text("Trim", color = palette.accentPurple, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Box {
                    val activeEff = targetMenuClip?.transitionIn ?: TransitionEffect.NONE
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (activeEff != TransitionEffect.NONE) palette.accentPurple.copy(alpha = 0.16f)
                                else StudioSurfaceElevated
                            )
                            .border(
                                1.dp,
                                if (activeEff != TransitionEffect.NONE) palette.accentPurple
                                else palette.accentPurple.copy(alpha = 0.4f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { showToolbarTransitionMenu = true }
                            .padding(horizontal = 7.dp, vertical = 4.dp)
                            .testTag("timeline_transitions_btn"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.CompareArrows,
                            contentDescription = "Select Transition Effect",
                            tint = palette.accentCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = if (activeEff != TransitionEffect.NONE) activeEff.badge else "Trans",
                            color = TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Icon(
                            Icons.Default.ArrowDropDown,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    TimelineTransitionDropdownMenu(
                        expanded = showToolbarTransitionMenu,
                        targetClip = targetMenuClip,
                        onDismiss = { showToolbarTransitionMenu = false },
                        onSelectTransition = { effect ->
                            showToolbarTransitionMenu = false
                            onApplyTransitionToClip(
                                targetMenuClip?.id,
                                effect,
                                effect.defaultDurationSec
                            )
                        },
                        onOpenFullStudio = {
                            showToolbarTransitionMenu = false
                            onOpenTransitions()
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Hybrid Multi-Track Scrubber with Inter-Clip Transition Indicators
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(114.dp)
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
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    for (track in tracks) {
                        val trackClips = clips.filter { it.trackType == track }.sortedBy { it.startTimeSec }
                        TrackRow(
                            track = track,
                            clips = trackClips,
                            selectedClipId = selectedClipId,
                            totalDurationSec = totalDurationSec,
                            onSelectClip = onSelectClip,
                            onApplyTransitionToClip = onApplyTransitionToClip,
                            onOpenTransitions = onOpenTransitions,
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
    onApplyTransitionToClip: (clipId: String?, effect: TransitionEffect, durationSec: Float) -> Unit = { _, _, _ -> },
    onOpenTransitions: () -> Unit = {},
    onUpdateClip: (String, Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val supportsTransitions = track == TimelineTrackType.PRODUCT || track == TimelineTrackType.TEXT
    var activeJunctionClipId by remember { mutableStateOf<String?>(null) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(20.dp)
            .padding(horizontal = 4.dp)
    ) {
        val totalWidth = maxWidth
        val isDark = OmniTheme.colors.isDark
        val selectedBorderColor = TextPrimary

        // 1. Render clips with visual transition ramp bands
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

            Box(
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
            ) {
                // Visual Shaded Transition Ramp Overlay along incoming clip edge
                if (supportsTransitions && clip.transitionIn != TransitionEffect.NONE && clip.transitionDurationSec > 0.05f) {
                    val rampFraction = (clip.transitionDurationSec / clip.durationSec.coerceAtLeast(0.1f))
                        .coerceIn(0.08f, 0.65f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(rampFraction)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        NeonPurple.copy(alpha = 0.85f),
                                        NeonCyan.copy(alpha = 0.45f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
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
                            text = if (clip.transitionIn != TransitionEffect.NONE) {
                                "⇄ ${clip.transitionIn.badge} • ${clip.name}"
                            } else {
                                clip.name
                            },
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.1fs", clip.durationSec),
                        color = Color.White.copy(alpha = 0.88f),
                        fontSize = 7.sp
                    )
                }
            }
        }

        // 2. Render Interactive Transition Junction Nodes at boundaries between clips (or clips with active transitions)
        if (supportsTransitions) {
            clips.forEachIndexed { index, clip ->
                val prevClip = clips.getOrNull(index - 1)
                val isBoundaryBetweenClips = prevClip != null &&
                    kotlin.math.abs(clip.startTimeSec - (prevClip.startTimeSec + prevClip.durationSec)) <= 0.35f
                val showJunctionNode = isBoundaryBetweenClips ||
                    clip.startTimeSec > 0.05f ||
                    clip.transitionIn != TransitionEffect.NONE

                if (showJunctionNode) {
                    val boundaryRatio = (clip.startTimeSec / totalDurationSec).coerceIn(0f, 0.96f)
                    val hasEffect = clip.transitionIn != TransitionEffect.NONE
                    val nodeSize = 18.dp
                    val nodeOffsetX = (totalWidth * boundaryRatio - nodeSize / 2f)
                        .coerceIn(0.dp, (totalWidth - nodeSize).coerceAtLeast(0.dp))

                    Box(
                        modifier = Modifier
                            .offset(x = nodeOffsetX)
                            .align(Alignment.CenterStart)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(nodeSize)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    if (hasEffect) Brush.linearGradient(listOf(NeonPurple, NeonCyan))
                                    else Brush.linearGradient(listOf(StudioSurface, StudioSurfaceElevated))
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (hasEffect) Color.White else NeonCyan.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(5.dp)
                                )
                                .clickable {
                                    onSelectClip(clip.id)
                                    activeJunctionClipId = clip.id
                                }
                                .testTag("transition_junction_${clip.id}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (hasEffect) timelineTransitionIcon(clip.transitionIn) else Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = if (hasEffect) {
                                    "Transition ${clip.transitionIn.title} on ${clip.name}"
                                } else {
                                    "Add transition before ${clip.name}"
                                },
                                tint = if (hasEffect) Color.White else NeonCyan,
                                modifier = Modifier.size(11.dp)
                            )
                        }

                        TimelineTransitionDropdownMenu(
                            expanded = activeJunctionClipId == clip.id,
                            targetClip = clip,
                            onDismiss = { activeJunctionClipId = null },
                            onSelectTransition = { effect ->
                                activeJunctionClipId = null
                                onApplyTransitionToClip(clip.id, effect, effect.defaultDurationSec)
                            },
                            onOpenFullStudio = {
                                activeJunctionClipId = null
                                onSelectClip(clip.id)
                                onOpenTransitions()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineTransitionDropdownMenu(
    expanded: Boolean,
    targetClip: TimelineClip?,
    onDismiss: () -> Unit,
    onSelectTransition: (TransitionEffect) -> Unit,
    onOpenFullStudio: () -> Unit
) {
    val palette = OmniTheme.colors
    val quickEffects = listOf(
        Triple(TransitionEffect.FADE, "Fade", "Smooth crossfade dissolve"),
        Triple(TransitionEffect.SLIDE_LEFT, "Slide", "Horizontal swipe transition"),
        Triple(TransitionEffect.ZOOM_IN, "Zoom", "Dynamic scale punch-in"),
        Triple(TransitionEffect.SLIDE_UP, "Slide Up", "Vertical upward push"),
        Triple(TransitionEffect.FLASH_CUT, "Flash Cut", "High-energy strobe flash"),
        Triple(TransitionEffect.NONE, "None (Cut)", "Remove transition")
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        modifier = Modifier
            .background(StudioSurface)
            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
            .testTag("timeline_transition_menu")
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            Text(
                text = "CLIP TRANSITION EFFECT",
                color = palette.accentCyan,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )
            if (targetClip != null) {
                Text(
                    text = "Target: ${targetClip.name}",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        HorizontalDivider(color = StudioBorder)

        quickEffects.forEach { (effect, shortTitle, desc) ->
            val isCurrent = targetClip?.transitionIn == effect
            val itemTag = when (effect) {
                TransitionEffect.FADE -> "timeline_menu_fade"
                TransitionEffect.SLIDE_LEFT -> "timeline_menu_slide"
                TransitionEffect.ZOOM_IN -> "timeline_menu_zoom"
                TransitionEffect.NONE -> "timeline_menu_none"
                else -> "timeline_menu_${effect.name.lowercase(Locale.US)}"
            }
            DropdownMenuItem(
                modifier = Modifier.testTag(itemTag),
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = shortTitle,
                                color = if (isCurrent) palette.accentPurple else TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = if (isCurrent) FontWeight.Black else FontWeight.Bold
                            )
                            Text(
                                text = desc,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        if (isCurrent) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Active",
                                tint = palette.accentPurple,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                leadingIcon = {
                    Icon(
                        imageVector = timelineTransitionIcon(effect),
                        contentDescription = shortTitle,
                        tint = if (isCurrent) palette.accentPurple else palette.accentCyan,
                        modifier = Modifier.size(18.dp)
                    )
                },
                onClick = { onSelectTransition(effect) }
            )
        }

        HorizontalDivider(color = StudioBorder)

        DropdownMenuItem(
            modifier = Modifier.testTag("timeline_menu_all_studio"),
            text = {
                Text(
                    text = "More Effects & Duration…",
                    color = palette.accentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                    contentDescription = "Open Transitions Studio",
                    tint = palette.accentCyan,
                    modifier = Modifier.size(16.dp)
                )
            },
            onClick = onOpenFullStudio
        )
    }
}

private fun timelineTransitionIcon(effect: TransitionEffect): ImageVector = when (effect) {
    TransitionEffect.NONE -> Icons.Default.ContentCut
    TransitionEffect.FADE -> Icons.Default.Gradient
    TransitionEffect.SLIDE_LEFT -> Icons.Default.SwapHoriz
    TransitionEffect.SLIDE_UP -> Icons.Default.ArrowUpward
    TransitionEffect.ZOOM_IN -> Icons.Default.ZoomIn
    TransitionEffect.WHIP_PAN -> Icons.Default.Bolt
    TransitionEffect.FLASH_CUT -> Icons.Default.FlashOn
}
