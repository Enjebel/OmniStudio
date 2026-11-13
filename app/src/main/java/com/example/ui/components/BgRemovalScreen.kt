package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BgRemovalMode
import com.example.model.BgReplacementType
import com.example.model.ChromaKeyColor
import com.example.model.VideoBgRemovalState
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGradient
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonTriGradient
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
fun BgRemovalScreen(
    bgState: VideoBgRemovalState,
    onPickVideo: (String) -> Unit,
    onScrubPosition: (Long) -> Unit,
    onModeChanged: (BgRemovalMode) -> Unit,
    onChromaKeyChanged: (ChromaKeyColor) -> Unit,
    onSensitivityChanged: (Float) -> Unit,
    onFeatherChanged: (Float) -> Unit,
    onReplacementChanged: (BgReplacementType) -> Unit,
    onSolidColorChanged: (Long) -> Unit,
    onCustomBackdropPicked: (String) -> Unit,
    onInjectToStudioTimeline: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OmniTheme.colors
    val scrollState = rememberScrollState()
    var showOriginalCompare by remember { mutableStateOf(false) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onPickVideo(uri.toString())
        }
    }

    val backdropImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onCustomBackdropPicked(uri.toString())
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .imePadding()
            .verticalScroll(scrollState)
            .verticalScrollbar(scrollState)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Header Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = null,
                        tint = palette.accentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "VIDEO BG REMOVAL STUDIO",
                        color = palette.accentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "Real-Time Chroma Key & AI Matte",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Pick Local Video Action Button
            Box(
                modifier = Modifier
                    .height(40.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(NeonGradient)
                    .clickable {
                        videoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                        )
                    }
                    .padding(horizontal = 12.dp)
                    .testTag("pick_local_video_btn"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VideoLibrary,
                        contentDescription = "Pick Video",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (bgState.videoUri == null) "Select Video" else "Change Video",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 2. Real-Time Segmented Video Viewport
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(StudioSurface)
                .border(1.5.dp, palette.border, RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            val displayBitmap = if (showOriginalCompare) {
                bgState.originalFrame
            } else {
                bgState.segmentedPreviewFrame ?: bgState.originalFrame
            }

            if (displayBitmap != null) {
                Image(
                    bitmap = displayBitmap.asImageBitmap(),
                    contentDescription = "Video Background Removal Preview",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                // Top Overlay Info Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.72f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${bgState.videoTitle.take(22)} • ${bgState.widthPx}x${bgState.heightPx}",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (showOriginalCompare) NeonPink else Color.Black.copy(alpha = 0.72f)
                            )
                            .clickable { showOriginalCompare = !showOriginalCompare }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("bg_compare_toggle_btn"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Compare Original",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (showOriginalCompare) "Original" else "Cutout",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Bottom Mode Badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.Black.copy(alpha = 0.72f))
                        .border(1.dp, NeonCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${bgState.mode.label} • ${bgState.replacementType.label}",
                        color = NeonCyan,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(StudioSurfaceElevated)
                            .border(1.dp, StudioBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = null,
                            tint = palette.accentCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Select a Local Video Clip",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Strip backgrounds in real time with AI matte segmentation or Chroma Key and replace with alpha transparency, studio color, cinematic blur, or custom art.",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 16.sp
                    )
                }
            }

            if (bgState.isProcessing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(30.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Segmenting video keyframe...",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (bgState.errorMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.errorContainer)
                    .border(1.dp, DangerRed, RoundedCornerShape(14.dp))
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(bgState.errorMessage, color = palette.onErrorContainer, fontSize = 11.sp)
            }
        }

        // 3. Video Frame Scrubber
        if (bgState.durationMs > 0L) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StudioSurface)
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FRAME SCRUBBER",
                        color = palette.accentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = String.format(
                            Locale.US,
                            "%.1fs / %.1fs",
                            bgState.scrubPositionMs / 1000f,
                            bgState.durationMs / 1000f
                        ),
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = bgState.scrubPositionMs.toFloat(),
                    onValueChange = { onScrubPosition(it.toLong()) },
                    valueRange = 0f..bgState.durationMs.toFloat().coerceAtLeast(100f),
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = StudioBorder
                    ),
                    modifier = Modifier.testTag("bg_video_scrub_slider")
                )
            }
        }

        // 4. Segmentation Mode Switcher (AI Matte vs Chroma Key)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(StudioSurface)
                .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "SEGMENTATION ENGINE",
                color = palette.accentPurple,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(StudioSurfaceElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BgRemovalMode.entries.forEach { mode ->
                    val isSel = bgState.mode == mode
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSel) NeonGradient
                                else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable { onModeChanged(mode) }
                            .padding(vertical = 8.dp, horizontal = 6.dp)
                            .testTag("bg_mode_${mode.name}"),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = mode.label,
                            color = if (isSel) Color.White else TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = mode.subtitle,
                            color = if (isSel) Color.White.copy(alpha = 0.85f) else TextMuted,
                            fontSize = 9.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Chroma Key Screen Color Selector
            if (bgState.mode == BgRemovalMode.CHROMA_KEY) {
                Text(
                    text = "CHROMA KEY SCREEN COLOR",
                    color = TextSecondary,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ChromaKeyColor.entries.forEach { kc ->
                        val isSel = bgState.chromaKeyColor == kc
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(StudioSurfaceElevated)
                                .border(
                                    width = if (isSel) 1.5.dp else 1.dp,
                                    color = if (isSel) NeonCyan else StudioBorder,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable { onChromaKeyChanged(kc) }
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .testTag("chroma_${kc.name}"),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(Color(kc.argb))
                                    .border(1.dp, StudioBorder, CircleShape)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = kc.label.substringBefore(" "),
                                color = if (isSel) palette.accentCyan else TextPrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Sensitivity & Feathering Sliders
            StudioGradientSlider(
                value = bgState.sensitivity,
                onValueChange = onSensitivityChanged,
                valueRange = 0.05f..0.95f,
                label = "Key Sensitivity & Tolerance",
                valueDisplay = "${(bgState.sensitivity * 100).toInt()}%",
                icon = Icons.Default.Tune,
                modifier = Modifier.testTag("bg_sensitivity_slider")
            )

            StudioGradientSlider(
                value = bgState.edgeFeather,
                onValueChange = onFeatherChanged,
                valueRange = 0.0f..0.9f,
                label = "Edge Feather & Spill Suppression",
                valueDisplay = "${(bgState.edgeFeather * 100).toInt()}%",
                icon = Icons.Default.BlurOn,
                modifier = Modifier.testTag("bg_feather_slider")
            )
        }

        // 5. Replacement Backdrop Studio Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(StudioSurface)
                .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "BACKDROP REPLACEMENT LAYER",
                color = palette.accentPink,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                BgReplacementType.entries.forEach { rep ->
                    val isSel = bgState.replacementType == rep
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSel) palette.accentCyan.copy(alpha = 0.16f) else StudioSurfaceElevated)
                            .border(
                                1.dp,
                                if (isSel) palette.accentCyan else StudioBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                if (rep == BgReplacementType.CUSTOM_IMAGE && bgState.customBackdropUri == null) {
                                    backdropImagePickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                } else {
                                    onReplacementChanged(rep)
                                }
                            }
                            .padding(vertical = 9.dp, horizontal = 4.dp)
                            .testTag("bg_replace_${rep.name}"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rep.label,
                            color = if (isSel) palette.accentCyan else TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (bgState.replacementType == BgReplacementType.SOLID_COLOR) {
                val colorPalette = listOf(
                    0xFF06B6D4L,
                    0xFF8B5CF6L,
                    0xFFEC4899L,
                    0xFFF59E0BL,
                    0xFF10B981L,
                    0xFF030712L,
                    0xFFF8FAFC
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(colorPalette) { colArgb ->
                        val isSel = bgState.solidColorArgb == colArgb
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(colArgb))
                                .border(
                                    width = if (isSel) 2.5.dp else 1.dp,
                                    color = if (isSel) palette.accentCyan else StudioBorder,
                                    shape = CircleShape
                                )
                                .clickable { onSolidColorChanged(colArgb) }
                        )
                    }
                }
            }

            if (bgState.replacementType == BgReplacementType.CUSTOM_IMAGE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioSurfaceElevated)
                        .border(1.dp, palette.accentPurple.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable {
                            backdropImagePickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = palette.accentPurple, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (bgState.customBackdropUri == null) "Choose Custom Backdrop Image" else "Swap Custom Backdrop Image",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // 6. Apply Cutout to Studio Timeline Button
        Button(
            onClick = onInjectToStudioTimeline,
            enabled = bgState.originalFrame != null && !bgState.isProcessing,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("bg_inject_studio_btn"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        if (bgState.originalFrame != null) NeonTriGradient
                        else Brush.linearGradient(listOf(StudioSurfaceElevated, StudioSurfaceElevated))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (bgState.cutoutAlphaFrame != null) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = if (bgState.originalFrame != null) Color.White else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Send Cutout to Studio Timeline",
                        color = if (bgState.originalFrame != null) Color.White else TextMuted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}
