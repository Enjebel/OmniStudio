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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Gradient
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
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
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun TransitionsScreen(
    clips: List<TimelineClip>,
    selectedClipId: String?,
    selectedTransition: TransitionEffect,
    transitionDurationSec: Float,
    onSelectClip: (String) -> Unit,
    onSelectTransition: (TransitionEffect) -> Unit,
    onDurationChanged: (Float) -> Unit,
    onApplyToSelectedClip: () -> Unit,
    onApplyToAllClips: () -> Unit,
    onSplitPrimaryClip: () -> Unit,
    onPreviewInStudio: () -> Unit,
    onAskDirectorForTransition: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OmniTheme.colors
    val listState = rememberLazyListState()
    val layerClips = clips.filter {
        it.trackType == TimelineTrackType.PRODUCT || it.trackType == TimelineTrackType.TEXT
    }.sortedBy { it.startTimeSec }
    val activeClip = layerClips.find { it.id == selectedClipId } ?: layerClips.firstOrNull()

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .verticalScrollbar(listState)
            .testTag("transitions_screen"),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(StudioSurface)
                    .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(NeonGradient),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.CompareArrows,
                                contentDescription = "Transitions",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Clip Transitions",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Fade, Slide, Zoom & Whip between multi-layered clips",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Quick action row: Split clip or Ask Director AI
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioSurfaceElevated)
                            .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                            .clickable { onSplitPrimaryClip() }
                            .padding(horizontal = 10.dp)
                            .testTag("transitions_split_clip_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.ContentCut, contentDescription = "Split Clip", tint = palette.accentCyan, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Split Video Clip", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(StudioSurfaceElevated)
                            .border(1.dp, palette.accentPurple.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .clickable { onAskDirectorForTransition() }
                            .padding(horizontal = 10.dp)
                            .testTag("transitions_ask_director_btn"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Default.Psychology, contentDescription = "Ask Director AI", tint = palette.accentPurple, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("AI Director Cut", color = palette.accentPurple, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Section 1: Multi-Layered Timeline Clip Selector
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "1. TARGET TIMELINE LAYER / CLIP",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.7.sp
                    )
                    Text(
                        text = "${layerClips.size} layers",
                        color = palette.accentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 4.dp)
                ) {
                    items(layerClips, key = { it.id }) { clip ->
                        val isSel = clip.id == activeClip?.id
                        val hasTrans = clip.transitionIn != TransitionEffect.NONE
                        Column(
                            modifier = Modifier
                                .width(156.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSel) StudioSurfaceElevated else StudioSurface)
                                .border(
                                    width = if (isSel) 1.5.dp else 1.dp,
                                    color = if (isSel) palette.accentCyan else StudioBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { onSelectClip(clip.id) }
                                .padding(10.dp)
                                .testTag("transition_clip_card_${clip.id}"),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(clip.trackType.color.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (clip.trackType == TimelineTrackType.PRODUCT) "VIDEO" else "TEXT",
                                        color = clip.trackType.color,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                }
                                Text(
                                    text = String.format(Locale.US, "%.1fs–%.1fs", clip.startTimeSec, clip.startTimeSec + clip.durationSec),
                                    color = TextMuted,
                                    fontSize = 9.sp
                                )
                            }

                            Text(
                                text = clip.name,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (hasTrans) NeonPurple else TextMuted)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = if (hasTrans) {
                                        "${clip.transitionIn.title} (${String.format(Locale.US, "%.1fs", clip.transitionDurationSec)})"
                                    } else {
                                        "No Transition"
                                    },
                                    color = if (hasTrans) palette.accentPurple else TextMuted,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Transition Effect Picker
        item {
            Text(
                text = "2. CHOOSE TRANSITION EFFECT",
                color = TextSecondary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.7.sp
            )
        }

        items(TransitionEffect.entries.toList(), key = { it.name }) { effect ->
            val isSelected = effect == selectedTransition
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) StudioSurfaceElevated else StudioSurface)
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) palette.accentPurple else StudioBorder,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clickable { onSelectTransition(effect) }
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("transition_effect_${effect.name}"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isSelected) palette.accentPurple.copy(alpha = 0.2f)
                            else StudioBackground
                        )
                        .border(
                            1.dp,
                            if (isSelected) palette.accentPurple else StudioBorder,
                            RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = transitionIconFor(effect),
                        contentDescription = effect.title,
                        tint = if (isSelected) palette.accentPurple else palette.accentCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = effect.title,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(StudioBackground)
                                .border(1.dp, StudioBorder, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = effect.category,
                                color = palette.accentCyan,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                    Text(
                        text = effect.subtitle,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Selected",
                        tint = palette.accentPurple,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Section 3: Duration & Apply Actions
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StudioSurface)
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Transition Duration",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = String.format(Locale.US, "%.2fs", transitionDurationSec),
                        color = palette.accentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Slider(
                    value = transitionDurationSec,
                    onValueChange = onDurationChanged,
                    valueRange = 0.25f..1.5f,
                    enabled = selectedTransition != TransitionEffect.NONE,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonPurple
                    )
                )

                Button(
                    onClick = onApplyToSelectedClip,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("apply_transition_clip_btn"),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Apply ${selectedTransition.badge} to ${activeClip?.name?.take(18) ?: "Selected Clip"}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onApplyToAllClips,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("apply_transition_all_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(
                            text = "Apply to All Clips",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onPreviewInStudio,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("preview_transition_studio_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Preview", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Preview",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private fun transitionIconFor(effect: TransitionEffect): ImageVector = when (effect) {
    TransitionEffect.NONE -> Icons.Default.ContentCut
    TransitionEffect.FADE -> Icons.Default.Gradient
    TransitionEffect.SLIDE_LEFT -> Icons.Default.SwapHoriz
    TransitionEffect.SLIDE_UP -> Icons.Default.ArrowUpward
    TransitionEffect.ZOOM_IN -> Icons.Default.ZoomIn
    TransitionEffect.WHIP_PAN -> Icons.Default.Bolt
    TransitionEffect.FLASH_CUT -> Icons.Default.FlashOn
}
