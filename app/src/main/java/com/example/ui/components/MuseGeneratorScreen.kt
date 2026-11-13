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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.OpenWith
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AspectRatioOption
import com.example.model.CameraMovement
import com.example.model.MuseGenUiState
import com.example.model.MuseGeneratedClip
import com.example.model.MuseGenerationMode
import com.example.model.SubscriptionPlan
import com.example.model.isUnlimitedAiTier
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.DangerRed
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.GoldGradient
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MuseGeneratorScreen(
    museGenState: MuseGenUiState,
    generatedClips: List<MuseGeneratedClip>,
    subscriptionTier: String,
    creditsRemaining: Int,
    onGenerateClicked: (
        prompt: String,
        camera: CameraMovement,
        panAmount: Float,
        zoomFactor: Float,
        tiltAngle: Float,
        ratio: AspectRatioOption,
        audioCue: String,
        batchCount: Int,
        referenceUri: String?
    ) -> Unit,
    onImportClipToTimeline: (MuseGeneratedClip) -> Unit,
    onOpenUpgradeModal: () -> Unit,
    modifier: Modifier = Modifier
) {
    var mode by remember { mutableStateOf(MuseGenerationMode.TEXT_TO_VIDEO) }
    var prompt by remember { mutableStateOf("Luxury futuristic matte black smartwatch floating through neon rain droplets, volumetric cyan rim lighting") }
    var audioCue by remember { mutableStateOf("Deep sub-bass drop + raindrop ambience + crisp whoosh") }
    var selectedCamera by remember { mutableStateOf(CameraMovement.SLOW_PUSH_IN) }
    var panAmount by remember { mutableFloatStateOf(CameraMovement.SLOW_PUSH_IN.defaultPan) }
    var zoomFactor by remember { mutableFloatStateOf(CameraMovement.SLOW_PUSH_IN.defaultZoom) }
    var tiltAngle by remember { mutableFloatStateOf(CameraMovement.SLOW_PUSH_IN.defaultTilt) }
    var selectedRatio by remember { mutableStateOf(AspectRatioOption.RATIO_9_16) }
    var batchCount by remember { mutableIntStateOf(1) }
    var referenceUri by remember { mutableStateOf<String?>(null) }

    val isUnlimited = isUnlimitedAiTier(subscriptionTier)
    val activePlan = SubscriptionPlan.fromTierId(subscriptionTier)
    val isLoading = museGenState is MuseGenUiState.Loading
    val lazyListState = rememberLazyListState()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            referenceUri = uri.toString()
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .imePadding()
            .verticalScrollbar(lazyListState)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // 1. Header & Credits Gateway
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MovieCreation,
                            contentDescription = null,
                            tint = NeonPink,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MUSE ENGINE AI",
                            color = NeonPink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                    }
                    Text(
                        text = "Faceless Video & Scene Generator",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Credits Pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isUnlimited) GoldGradient else Brush.linearGradient(listOf(StudioSurfaceElevated, StudioSurfaceElevated)))
                        .border(1.dp, if (isUnlimited) GoldGlow else NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                        .clickable { onOpenUpgradeModal() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("muse_credits_pill")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isUnlimited) Icons.Default.Star else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (isUnlimited) Color.Black else NeonCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isUnlimited) "UNLIMITED ${activePlan.badgeTitle}" else "$creditsRemaining CREDITS",
                            color = if (isUnlimited) Color.Black else NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // 2. Mode Switcher (Text-to-Video vs Image-to-Video)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StudioSurface)
                    .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                MuseGenerationMode.entries.forEach { m ->
                    val isSel = mode == m
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSel) NeonGradient else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                            )
                            .clickable { mode = m }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = m.label,
                            color = if (isSel) Color.White else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3. Optional Image-to-Video Reference Frame Uploader
        if (mode == MuseGenerationMode.IMAGE_TO_VIDEO) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(StudioSurface)
                        .border(1.dp, NeonPurple.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                        .clickable {
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(14.dp)
                        .testTag("muse_reference_image_picker")
                ) {
                    if (referenceUri != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                AsyncImage(
                                    model = referenceUri,
                                    contentDescription = "Reference Image",
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Source Keyframe Loaded", color = NeonCyan, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Muse will animate this subject", color = TextSecondary, fontSize = 11.sp)
                                }
                            }
                            IconButton(onClick = { referenceUri = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextMuted)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Tap to Pick Start Image Frame from Gallery", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // 4. Prompt Builder Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(StudioSurface)
                    .border(1.dp, StudioBorder, RoundedCornerShape(22.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "VISUAL SCENE PROMPT",
                    color = NeonCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 0.8.sp
                )

                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    minLines = 2,
                    maxLines = 4,
                    placeholder = { Text("Describe the scene, lighting, and subject motion...", color = TextMuted, fontSize = 12.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("muse_prompt_input"),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Quick Prompt Starters
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val starters = listOf(
                        "Neon Cyberpunk Watch Rain" to "Sleek obsidian chronograph floating in neon cyberpunk rain, dramatic cyan rim light",
                        "Golden Luxury Perfume Mist" to "Crystal fragrance bottle on golden travertine stone, warm sunbeams and shimmering mist",
                        "Minimalist Tech Desk Setup" to "Ultra-clean futuristic matte black desk setup with glowing purple ambient light"
                    )
                    items(starters) { (label, fullPrompt) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(StudioSurfaceElevated)
                                .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                                .clickable { prompt = fullPrompt }
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        ) {
                            Text(label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Camera Movement Presets & Pan/Zoom/Tilt Rig
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CAMERA MOVEMENT (PAN • ZOOM • TILT)",
                        color = NeonPurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = selectedCamera.axisTag,
                        color = OmniTheme.colors.accentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(CameraMovement.entries) { cam ->
                        val isSel = selectedCamera == cam
                        Column(
                            modifier = Modifier
                                .width(128.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSel) StudioSurfaceElevated else StudioBackground)
                                .border(
                                    width = if (isSel) 1.5.dp else 1.dp,
                                    color = if (isSel) NeonCyan else StudioBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    selectedCamera = cam
                                    panAmount = cam.defaultPan
                                    zoomFactor = cam.defaultZoom
                                    tiltAngle = cam.defaultTilt
                                }
                                .padding(10.dp)
                                .testTag("muse_cam_${cam.name}")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Videocam,
                                    contentDescription = null,
                                    tint = if (isSel) NeonCyan else TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = cam.axisTag,
                                    color = if (isSel) NeonCyan else NeonPurple,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(cam.label, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            Text(cam.subtitle, color = TextMuted, fontSize = 9.sp, maxLines = 1)
                        }
                    }
                }

                // Granular Pan, Zoom & Tilt Precision Sliders
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StudioGradientSlider(
                        value = panAmount,
                        onValueChange = { panAmount = it },
                        valueRange = -1f..1f,
                        label = "Camera Pan (Left / Right)",
                        valueDisplay = String.format(Locale.US, "%+d%%", (panAmount * 100).toInt()),
                        icon = Icons.Default.SwapHoriz,
                        modifier = Modifier.testTag("muse_pan_slider")
                    )

                    StudioGradientSlider(
                        value = zoomFactor,
                        onValueChange = { zoomFactor = it },
                        valueRange = 0.5f..2.0f,
                        label = "Camera Zoom (Out / In)",
                        valueDisplay = String.format(Locale.US, "%.2fx", zoomFactor),
                        icon = Icons.Default.ZoomIn,
                        modifier = Modifier.testTag("muse_zoom_slider")
                    )

                    StudioGradientSlider(
                        value = tiltAngle,
                        onValueChange = { tiltAngle = it },
                        valueRange = -1f..1f,
                        label = "Camera Tilt (Down / Up)",
                        valueDisplay = String.format(Locale.US, "%+d%%", (tiltAngle * 100).toInt()),
                        icon = Icons.Default.SwapVert,
                        modifier = Modifier.testTag("muse_tilt_slider")
                    )
                }

                // Aspect Ratio Selection (All Formats) & Batch Count
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("ASPECT RATIO", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AspectRatioOption.entries.forEach { r ->
                            val isSel = selectedRatio == r
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) OmniTheme.colors.accentCyan.copy(alpha = 0.16f) else StudioSurfaceElevated)
                                    .border(1.dp, if (isSel) OmniTheme.colors.accentCyan else StudioBorder, RoundedCornerShape(10.dp))
                                    .clickable { selectedRatio = r }
                                    .padding(vertical = 7.dp, horizontal = 4.dp)
                                    .testTag("muse_ratio_${r.name}"),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(r.label, color = if (isSel) OmniTheme.colors.accentCyan else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = r.subtitle.substringBefore(" /"),
                                    color = TextMuted,
                                    fontSize = 8.sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("BATCH SHOTS", color = TextSecondary, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(1, 2, 3).forEach { count ->
                            val isSel = batchCount == count
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) OmniTheme.colors.accentPurple.copy(alpha = 0.16f) else StudioSurfaceElevated)
                                    .border(1.dp, if (isSel) OmniTheme.colors.accentPurple else StudioBorder, RoundedCornerShape(10.dp))
                                    .clickable { batchCount = count }
                                    .padding(vertical = 7.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("${count}x Shot${if (count > 1) "s" else ""}", color = if (isSel) OmniTheme.colors.accentPurple else TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Native Audio Cue Description
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.GraphicEq, contentDescription = null, tint = NeonPink, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("NATIVE AUDIO CUE DESCRIPTION", color = NeonPink, fontSize = 10.sp, fontWeight = FontWeight.Black)
                }

                OutlinedTextField(
                    value = audioCue,
                    onValueChange = { audioCue = it },
                    singleLine = true,
                    placeholder = { Text("e.g. Deep bass swell + crisp shutter click", color = TextMuted, fontSize = 11.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonPink,
                        unfocusedBorderColor = StudioBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                // Generate Button
                Button(
                    onClick = {
                        onGenerateClicked(
                            prompt,
                            selectedCamera,
                            panAmount,
                            zoomFactor,
                            tiltAngle,
                            selectedRatio,
                            audioCue,
                            batchCount,
                            if (mode == MuseGenerationMode.IMAGE_TO_VIDEO) referenceUri else null
                        )
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("muse_generate_btn"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(NeonTriGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Synthesizing AI Scenes...", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        } else {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate ${batchCount}x AI Clip(s)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        // 5. Loading / Error States (Strict Mock Ban: Real Errors Surfaced Visibly)
        when (museGenState) {
            is MuseGenUiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(StudioSurface)
                            .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .padding(20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(30.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = museGenState.stageText,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Firebase AI Logic • Gemini Vision & Nano Banana 2.1 (30-60s)",
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
            is MuseGenUiState.Error -> {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(OmniTheme.colors.errorContainer)
                            .border(1.dp, DangerRed, RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Muse AI Generation Error", color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Text(museGenState.message, color = OmniTheme.colors.onErrorContainer, fontSize = 11.sp)
                        }
                    }
                }
            }
            else -> {}
        }

        // 6. Generated Clips Library with 1-Tap Import to Timeline or Empty State
        if (generatedClips.isNotEmpty()) {
            item {
                Text(
                    text = "GENERATED AI CLIPS (${generatedClips.size})",
                    color = OmniTheme.colors.accentCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
            }

            items(generatedClips, key = { it.id }) { clip ->
                MuseClipCard(
                    clip = clip,
                    onImport = { onImportClipToTimeline(clip) }
                )
            }
        } else if (museGenState is MuseGenUiState.Idle) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(StudioSurface)
                        .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(StudioSurfaceElevated)
                            .border(1.dp, StudioBorder, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = null,
                            tint = OmniTheme.colors.accentPurple,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No AI Scenes Generated Yet",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Configure your visual prompt, Pan/Zoom/Tilt camera movement, and aspect ratio above, then generate scenes to inject into your timeline.",
                        color = TextMuted,
                        fontSize = 12.sp,
                        lineHeight = 17.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MuseClipCard(
    clip: MuseGeneratedClip,
    onImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(StudioSurface)
            .border(1.dp, StudioBorder, RoundedCornerShape(22.dp))
            .padding(14.dp)
            .testTag("muse_clip_${clip.id}")
    ) {
        if (clip.keyframeBitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
            ) {
                Image(
                    bitmap = clip.keyframeBitmap.asImageBitmap(),
                    contentDescription = clip.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.7f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${clip.aspectRatio.label} • ${clip.cameraMovement.label}",
                        color = NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1fs", clip.durationSec),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Text(
            text = clip.title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = clip.sceneDirection,
            color = TextSecondary,
            fontSize = 11.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = String.format(
                Locale.US,
                "Rig: Pan %+d%% • Zoom %.2fx • Tilt %+d%%",
                (clip.panAmount * 100).toInt(),
                clip.zoomFactor,
                (clip.tiltAngle * 100).toInt()
            ),
            color = OmniTheme.colors.accentCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.GraphicEq, contentDescription = null, tint = NeonPink, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "Audio Cue: ${clip.audioCue}",
                color = NeonPink,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onImport,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .testTag("muse_inject_timeline_${clip.id}"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (clip.isImportedToTimeline) Brush.linearGradient(listOf(CyberGreen, Color(0xFF059669))) else NeonGradient),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (clip.isImportedToTimeline) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (clip.isImportedToTimeline) "Re-Inject into Timeline" else "Inject to Multi-Track Timeline",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
