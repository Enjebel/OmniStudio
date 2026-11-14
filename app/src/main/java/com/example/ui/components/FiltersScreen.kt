package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.CanvasLayer
import com.example.model.ColorFilterPreset
import com.example.model.LayerType
import com.example.studio.ColorGradingUtils
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OmniTheme
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun FiltersScreen(
    layers: List<CanvasLayer>,
    activeFilter: ColorFilterPreset,
    intensity: Float,
    onSelectFilter: (ColorFilterPreset) -> Unit,
    onIntensityChanged: (Float) -> Unit,
    onResetFilter: () -> Unit,
    onPreviewInStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = OmniTheme.colors
    val listState = rememberLazyListState()
    val prodLayer = remember(layers) {
        layers.firstOrNull { it.type == LayerType.PRODUCT }
    }
    var compareOriginal by remember { mutableStateOf(false) }

    val effectivePreset = if (compareOriginal) ColorFilterPreset.NONE else activeFilter
    val effectiveIntensity = if (compareOriginal) 0f else intensity
    val liveColorFilter = remember(effectivePreset, effectiveIntensity) {
        ColorGradingUtils.toComposeColorFilter(effectivePreset, effectiveIntensity)
    }

    val presetRows = remember { ColorFilterPreset.entries.chunked(2) }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .verticalScrollbar(listState)
            .testTag("filters_screen"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Header
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(palette.accentCyan.copy(alpha = 0.16f))
                            .border(1.dp, palette.accentCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoFilter,
                            contentDescription = "Color Grading Filters",
                            tint = palette.accentCyan,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Text(
                        text = "COLOR GRADING & FILTERS",
                        color = palette.accentCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.1.sp
                    )
                }
                Text(
                    text = "Product Color Studio",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "Apply CSS color grading presets directly to the product display layer and video exports.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
            }
        }

        // 2. Live Graded Product Preview Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(StudioSurface)
                    .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(224.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(activeFilter.accentHex).copy(alpha = 0.24f),
                                    Color(0xFF090D16)
                                )
                            )
                        )
                        .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    ProductLayerThumbnail(
                        layer = prodLayer,
                        colorFilter = liveColorFilter,
                        contentDescription = "Live product display preview with ${activeFilter.title} filter",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    )

                    // Active preset / compare pill (top-start)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xCC0B0F19))
                            .border(
                                1.dp,
                                Color(effectivePreset.accentHex).copy(alpha = 0.55f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(effectivePreset.accentHex))
                        )
                        Text(
                            text = if (compareOriginal) {
                                "Comparing: Original"
                            } else if (activeFilter == ColorFilterPreset.NONE) {
                                "Original (Ungraded)"
                            } else {
                                "${activeFilter.title} • ${(intensity * 100).roundToInt()}%"
                            },
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Before / After toggle button (top-end, >=48dp touch height)
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (compareOriginal) NeonPurple.copy(alpha = 0.9f) else Color(0xCC111827)
                            )
                            .border(
                                1.dp,
                                if (compareOriginal) Color.White.copy(alpha = 0.35f) else StudioBorder,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { compareOriginal = !compareOriginal }
                            .padding(horizontal = 12.dp)
                            .testTag("filters_compare_toggle"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Compare,
                            contentDescription = "Compare original product look",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (compareOriginal) "Original" else "Compare",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // CSS Filter Spec readout
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioSurfaceElevated)
                        .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "ACTIVE CSS FILTER SPEC (PRODUCT LAYER)",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = activeFilter.cssFilterSpec,
                        color = palette.accentCyan,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("active_css_filter_spec")
                    )
                }
            }
        }

        // 3. Intensity Slider Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StudioSurface)
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter intensity",
                            tint = NeonPurple,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = "Grade Intensity",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = if (activeFilter == ColorFilterPreset.NONE) "0%" else "${(intensity * 100).roundToInt()}%",
                        color = if (activeFilter == ColorFilterPreset.NONE) TextMuted else palette.accentCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Slider(
                    value = if (activeFilter == ColorFilterPreset.NONE) 0f else intensity,
                    onValueChange = { newValue ->
                        compareOriginal = false
                        onIntensityChanged(newValue)
                    },
                    enabled = activeFilter != ColorFilterPreset.NONE,
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonPurple,
                        inactiveTrackColor = StudioBorder
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("filter_intensity_slider")
                )

                Text(
                    text = if (activeFilter == ColorFilterPreset.NONE) {
                        "Select a color grading preset below to blend its CSS filter matrix from 0% to 100%."
                    } else {
                        "Blends ${activeFilter.title} parameters linearly with the ungraded product layer."
                    },
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // 4. Color Grading Presets Section Header
        item {
            Text(
                text = "COLOR GRADING PRESETS (${ColorFilterPreset.entries.size})",
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
        }

        // 5. 2-Column Preset Grid Rows
        items(presetRows) { rowPresets ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowPresets.forEach { preset ->
                    FilterPresetCard(
                        preset = preset,
                        prodLayer = prodLayer,
                        isSelected = preset == activeFilter,
                        onClick = {
                            compareOriginal = false
                            onSelectFilter(preset)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowPresets.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // 6. Footer Action Buttons
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        compareOriginal = false
                        onResetFilter()
                    },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .testTag("filters_reset_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset filter",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Reset",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onPreviewInStudio,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonPurple,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp)
                        .testTag("filters_apply_studio_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Movie,
                        contentDescription = "Preview in Studio",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "View in Studio",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterPresetCard(
    preset: ColorFilterPreset,
    prodLayer: CanvasLayer?,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = Color(preset.accentHex)
    val previewColorFilter = remember(preset) {
        ColorGradingUtils.toComposeColorFilter(preset, 1.0f)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) StudioSurfaceElevated else StudioSurface)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accent else StudioBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .pressScaleClickable { onClick() }
            .padding(10.dp)
            .testTag("filter_preset_${preset.name}"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.22f),
                            Color(0xFF0A0E17)
                        )
                    )
                )
                .border(1.dp, StudioBorder, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            ProductLayerThumbnail(
                layer = prodLayer,
                colorFilter = previewColorFilter,
                contentDescription = "${preset.title} preset preview",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(10.dp)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "${preset.title} selected",
                    tint = accent,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(accent)
            )
            Text(
                text = preset.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text(
            text = preset.subtitle,
            color = TextSecondary,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ProductLayerThumbnail(
    layer: CanvasLayer?,
    colorFilter: ColorFilter?,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    when {
        layer?.aiBitmap != null -> {
            Image(
                bitmap = layer.aiBitmap.asImageBitmap(),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = modifier
            )
        }
        layer?.customImageUri != null -> {
            AsyncImage(
                model = layer.customImageUri,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = modifier
            )
        }
        else -> {
            Image(
                painter = painterResource(id = layer?.productDrawableRes ?: com.example.R.drawable.prod_watch),
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                colorFilter = colorFilter,
                modifier = modifier
            )
        }
    }
}
