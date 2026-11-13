package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Opacity
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.BackgroundPreset
import com.example.model.LightingSettings
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
fun PhotoAndLightingPanel(
    selectedBgPreset: BackgroundPreset,
    onBgPresetSelected: (BackgroundPreset) -> Unit,
    customPrompt: String,
    onCustomPromptSubmit: (String) -> Unit,
    lighting: LightingSettings,
    onUpdateLighting: ((LightingSettings) -> LightingSettings) -> Unit,
    onTriggerAiCutout: () -> Unit,
    onImportCustomProduct: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var promptInput by remember { mutableStateOf(customPrompt) }
    var activeSubTab by remember { mutableStateOf(0) } // 0: Backdrops, 1: Lighting, 2: AI Cutout

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            onImportCustomProduct(uri.toString(), "Custom Product Photo")
        }
    }

    val lazyListState = rememberLazyListState()

    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .background(StudioSurface)
            .imePadding()
            .verticalScrollbar(lazyListState)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Drag Handle + Pill Tabs Row inside LazyColumn so the entire panel scrolls seamlessly
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(StudioBorder.copy(alpha = 0.5f))
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StudioSurfaceElevated)
                        .border(1.dp, StudioBorder, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Backdrops", "Studio Lighting", "Smart Cutout").forEachIndexed { index, label ->
                        val isSelected = activeSubTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) {
                                        Brush.horizontalGradient(listOf(NeonPurple, NeonCyan))
                                    } else {
                                        Brush.linearGradient(listOf(Color.Transparent, Color.Transparent))
                                    }
                                )
                                .clickable { activeSubTab = index }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        when (activeSubTab) {
                0 -> {
                    // Backdrops Carousel
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PROCEDURAL BACKGROUND PRESETS",
                                color = NeonCyan,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(BackgroundPreset.entries.filter { it != BackgroundPreset.CUSTOM_PROMPT }) { preset ->
                                val isSelected = preset == selectedBgPreset
                                Column(
                                    modifier = Modifier
                                        .width(115.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) StudioSurfaceElevated else StudioSurface)
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            brush = if (isSelected) {
                                                Brush.horizontalGradient(listOf(NeonPurple, NeonCyan))
                                            } else {
                                                Brush.linearGradient(listOf(StudioBorder, StudioBorder))
                                            },
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                        .clickable { onBgPresetSelected(preset) }
                                        .padding(10.dp)
                                        .testTag("bg_preset_${preset.name}"),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        CanvasBackground(preset = preset, modifier = Modifier.fillMaxSize())
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        preset.title,
                                        color = TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                    Text(preset.category, color = TextMuted, fontSize = 9.sp)
                                }
                            }
                        }
                    }

                    // Custom AI Prompt Backdrop Card
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(StudioSurfaceElevated)
                                .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.AutoFixHigh,
                                    contentDescription = "AI Prompt",
                                    tint = NeonPurple,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Custom AI Studio Backdrop",
                                    color = TextPrimary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = promptInput,
                                onValueChange = { promptInput = it },
                                placeholder = {
                                    Text(
                                        "e.g. Cyberpunk neon rain on wet asphalt",
                                        color = TextMuted,
                                        fontSize = 12.sp
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = StudioBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onCustomPromptSubmit(promptInput) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .testTag("generate_backdrop_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                            ) {
                                Text(
                                    "Generate Realistic Backdrop",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Studio Lighting & Custom Gradient Sliders
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            StudioGradientSlider(
                                value = lighting.shadowOpacity,
                                onValueChange = { v -> onUpdateLighting { it.copy(shadowOpacity = v) } },
                                valueRange = 0f..1f,
                                label = "Drop Shadow Density",
                                valueDisplay = String.format(Locale.US, "%.0f%%", lighting.shadowOpacity * 100),
                                icon = Icons.Default.Opacity
                            )

                            StudioGradientSlider(
                                value = lighting.shadowBlur,
                                onValueChange = { v -> onUpdateLighting { it.copy(shadowBlur = v) } },
                                valueRange = 0f..60f,
                                label = "Shadow Softness & Diffusion",
                                valueDisplay = String.format(Locale.US, "%.0f px", lighting.shadowBlur),
                                icon = Icons.Default.BlurOn
                            )

                            StudioGradientSlider(
                                value = lighting.reflectionOpacity,
                                onValueChange = { v -> onUpdateLighting { it.copy(reflectionOpacity = v) } },
                                valueRange = 0f..1f,
                                label = "Floor Mirror Reflection",
                                valueDisplay = String.format(Locale.US, "%.0f%%", lighting.reflectionOpacity * 100),
                                icon = Icons.Default.FlipCameraAndroid
                            )
                        }
                    }
                }

                2 -> {
                    // Smart Cutout
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(20.dp))
                                .background(StudioSurfaceElevated)
                                .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
                                .padding(16.dp)
                        ) {
                            Text(
                                "AI AUTO-SEGMENTATION",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Instantly detect product boundaries and isolate items with sub-pixel alpha masking.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onTriggerAiCutout,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("ai_cutout_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                            ) {
                                Icon(
                                    Icons.Default.CleaningServices,
                                    contentDescription = "Cutout",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Run AI Auto-Cutout",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Button(
                                onClick = {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .testTag("import_and_cutout_button"),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                            ) {
                                Icon(
                                    Icons.Default.AddPhotoAlternate,
                                    contentDescription = "Import Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Import Custom Photo from Gallery",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
        }
    }
}
