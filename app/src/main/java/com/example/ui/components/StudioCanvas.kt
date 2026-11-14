package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AspectRatioOption
import com.example.model.BackgroundPreset
import com.example.model.CanvasLayer
import com.example.model.ColorFilterPreset
import com.example.model.LayerType
import com.example.model.LightingSettings
import com.example.model.MotionTemplate
import com.example.model.SubscriptionPlan
import com.example.model.TextAnimationType
import com.example.model.TimelineClip
import com.example.model.TransitionEffect
import com.example.model.isWatermarkFreeTier
import com.example.studio.ColorGradingUtils
import com.example.studio.ExportEngine
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.StudioBackground
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioBorderGlow
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextPrimary
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StudioCanvas(
    aspectRatio: AspectRatioOption,
    zoomLevel: Float,
    bgPreset: BackgroundPreset,
    motionTemplate: MotionTemplate,
    motionSpeed: Float,
    motionIntensity: Float,
    lighting: LightingSettings,
    layers: List<CanvasLayer>,
    selectedLayerId: String?,
    playheadSec: Float,
    isPlaying: Boolean,
    onSelectLayer: (String?) -> Unit,
    onUpdateLayer: (String, (CanvasLayer) -> CanvasLayer) -> Unit,
    onFinishTransform: () -> Unit,
    onDuplicateLayer: (String) -> Unit,
    onDeleteLayer: (String) -> Unit,
    onBringForward: (String) -> Unit,
    onSendBackward: (String) -> Unit,
    subscriptionTier: String = "FREE",
    timelineClips: List<TimelineClip> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Calculate motion offsets
    val time = playheadSec * motionSpeed
    var motionScale = 1.0f
    var motionOffsetX = 0f
    var motionOffsetY = 0f
    var motionRotation = 0f
    var motionAlpha = 1.0f

    when (motionTemplate) {
        MotionTemplate.DYNAMIC_ZOOM_PUNCH -> {
            val punchCycle = (time % 1.5f)
            motionScale = if (punchCycle < 0.25f) 1.0f + (0.22f * motionIntensity) else 1.0f
            if (punchCycle < 0.25f) {
                motionOffsetX = (sin(time * 40.0) * 12f * motionIntensity).toFloat()
                motionOffsetY = (cos(time * 40.0) * 12f * motionIntensity).toFloat()
            }
        }
        MotionTemplate.ORBIT_3D -> {
            motionOffsetX = (cos(time * 2.0) * 24f * motionIntensity).toFloat()
            motionOffsetY = (sin(time * 2.0) * 16f * motionIntensity).toFloat()
            motionRotation = (sin(time * 1.5) * 6f * motionIntensity).toFloat()
        }
        MotionTemplate.FLOATING_LUXURY -> {
            motionOffsetY = (sin(time * 1.8) * 20f * motionIntensity).toFloat()
            motionRotation = (cos(time * 1.2) * 3f * motionIntensity).toFloat()
        }
        MotionTemplate.FLASH_SALE_GLITCH -> {
            val isFlash = ((time * 12).toInt() % 4 == 0)
            if (isFlash) {
                motionOffsetX = ((Math.random() - 0.5) * 20f * motionIntensity).toFloat()
                motionOffsetY = ((Math.random() - 0.5) * 10f * motionIntensity).toFloat()
                motionScale = 1.0f + (0.08f * motionIntensity)
            }
        }
        MotionTemplate.CINEMATIC_REVEAL -> {
            val progress = (time / 2.0f).coerceIn(0f, 1f)
            motionAlpha = progress
            motionScale = 0.9f + (0.1f * progress)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .clickable { onSelectLayer(null) },
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .padding(10.dp)
                .scale(zoomLevel),
            contentAlignment = Alignment.Center
        ) {
            val containerWidth = maxWidth
            val containerHeight = maxHeight

            // Determine dimensions based on Aspect Ratio
            val isPortrait = aspectRatio.ratio < 1f
            val canvasModifier = if (isPortrait) {
                Modifier
                    .fillMaxSize(0.94f)
                    .aspectRatio(aspectRatio.ratio)
            } else {
                Modifier
                    .fillMaxWidth(0.96f)
                    .aspectRatio(aspectRatio.ratio)
            }

            Box(
                modifier = canvasModifier
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
                    .shadow(8.dp, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                // 1. Procedural Background Drawing
                CanvasBackground(preset = bgPreset, modifier = Modifier.fillMaxSize())

                // 2. Multi-layer Composition
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val cw = maxWidth
                    val ch = maxHeight
                    val transition = with(androidx.compose.ui.platform.LocalDensity.current) {
                        ExportEngine.computeClipTransitionTransform(
                            clips = timelineClips,
                            timeSec = playheadSec,
                            targetWidth = cw.toPx(),
                            targetHeight = ch.toPx()
                        )
                    }

                    val sortedLayers = layers.filter { it.isVisible }.sortedBy { it.zIndex }

                    // Draw floor reflection if product layer exists
                    val prodLayer = sortedLayers.firstOrNull { it.type == LayerType.PRODUCT }
                    prodLayer?.let { prod ->
                        val refX = (prod.x * cw.value).dp - (cw * 0.35f)
                        val refY = ((prod.y + 0.28f) * ch.value).dp - (ch * 0.35f)
                        val reflectionModel: Any = prod.aiBitmap ?: prod.customImageUri ?: prod.productDrawableRes

                        val reflectionColorFilter = ColorGradingUtils.toComposeColorFilter(prod.colorFilter, prod.filterIntensity)
                        AsyncImage(
                            model = reflectionModel,
                            contentDescription = "Floor Reflection",
                            colorFilter = reflectionColorFilter,
                            modifier = Modifier
                                .offset(x = refX, y = refY)
                                .size(cw * 0.7f * prod.scale * transition.scale)
                                .scale(1f, -1f)
                                .rotate(-prod.rotation - transition.rotationDeg)
                                .alpha(lighting.reflectionOpacity * 0.35f * transition.alpha),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Draw Sorted Layers
                    for (layer in sortedLayers) {
                        val isSelected = layer.id == selectedLayerId
                        val layerCenterX = (layer.x * cw.value).dp
                        val layerCenterY = (layer.y * ch.value).dp

                        // Apply motion + active transition transformations
                        val baseScale = if (layer.type == LayerType.PRODUCT) layer.scale * motionScale else layer.scale
                        val currentScale = baseScale * transition.scale
                        val currentRot = (if (layer.type == LayerType.PRODUCT) layer.rotation + motionRotation else layer.rotation) + transition.rotationDeg
                        val currentOffX = (if (layer.type == LayerType.PRODUCT) motionOffsetX else 0f) + transition.offsetX
                        val currentOffY = (if (layer.type == LayerType.PRODUCT) motionOffsetY else 0f) + transition.offsetY

                        Box(
                            modifier = Modifier
                                .offset {
                                    IntOffset(
                                        x = (layerCenterX.toPx() + currentOffX).toInt(),
                                        y = (layerCenterY.toPx() + currentOffY).toInt()
                                    )
                                }
                                .offset(x = (-cw * 0.35f * currentScale), y = (-ch * 0.35f * currentScale))
                                .size(cw * 0.7f * currentScale, ch * 0.7f * currentScale)
                                .rotate(currentRot)
                                .alpha((layer.opacity * motionAlpha * transition.alpha).coerceIn(0f, 1f))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(1.5.dp, NeonCyan, RoundedCornerShape(8.dp))
                                    } else Modifier
                                )
                                .pointerInput(layer.id, layer.isLocked) {
                                    if (!layer.isLocked) {
                                        detectDragGestures(
                                            onDragStart = { onSelectLayer(layer.id) },
                                            onDragEnd = { onFinishTransform() },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                val newX = (layer.x + dragAmount.x / cw.toPx()).coerceIn(0.1f, 0.9f)
                                                val newY = (layer.y + dragAmount.y / ch.toPx()).coerceIn(0.1f, 0.9f)
                                                onUpdateLayer(layer.id) { it.copy(x = newX, y = newY) }
                                            }
                                        )
                                    }
                                }
                                .clickable { onSelectLayer(layer.id) },
                            contentAlignment = Alignment.Center
                        ) {
                            when (layer.type) {
                                LayerType.PRODUCT -> {
                                    val imageModel: Any = layer.aiBitmap ?: layer.customImageUri ?: layer.productDrawableRes
                                    val prodColorFilter = ColorGradingUtils.toComposeColorFilter(layer.colorFilter, layer.filterIntensity)
                                    AsyncImage(
                                        model = imageModel,
                                        contentDescription = layer.name,
                                        colorFilter = prodColorFilter,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .shadow(
                                                elevation = (lighting.shadowBlur * 0.4f).dp,
                                                shape = CircleShape,
                                                spotColor = Color.Black.copy(alpha = lighting.shadowOpacity)
                                            ),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                LayerType.TEXT -> {
                                    val textAnim = layer.textAnimation
                                    val displayText = if (textAnim == TextAnimationType.TYPEWRITER && isPlaying) {
                                        val charsToShow = ((time * 8).toInt() % (layer.text.length + 3)).coerceIn(1, layer.text.length)
                                        layer.text.take(charsToShow)
                                    } else layer.text

                                    if (layer.isGradientText) {
                                        Text(
                                            text = displayText,
                                            fontSize = layer.fontSizeSp.sp,
                                            fontWeight = FontWeight.Black,
                                            textAlign = TextAlign.Center,
                                            letterSpacing = layer.letterSpacingSp.sp,
                                            style = androidx.compose.ui.text.TextStyle(
                                                brush = Brush.horizontalGradient(
                                                    listOf(Color(layer.gradientColorStart), Color(layer.gradientColorEnd))
                                                )
                                            ),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    } else {
                                        Text(
                                            text = displayText,
                                            fontSize = layer.fontSizeSp.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(layer.textColor),
                                            textAlign = TextAlign.Center,
                                            letterSpacing = layer.letterSpacingSp.sp,
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                }
                                LayerType.BADGE -> {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(Color(layer.badgeAccentColor))
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = layer.badgeLabel,
                                            color = Color.White,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                else -> {}
                            }
                        }
                    }

                    if (transition.flashAlpha > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.White.copy(alpha = transition.flashAlpha))
                        )
                    }

                    val activeFilterLayer = prodLayer?.takeIf {
                        it.colorFilter != ColorFilterPreset.NONE && it.filterIntensity > 0.01f
                    }
                    if (activeFilterLayer != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(
                                    start = 8.dp,
                                    bottom = if (transition.effect != TransitionEffect.NONE) 30.dp else 8.dp
                                )
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(activeFilterLayer.colorFilter.accentHex).copy(alpha = 0.88f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                                .testTag("canvas_active_filter_badge")
                        ) {
                            Text(
                                text = "✦ ${activeFilterLayer.colorFilter.title}",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (transition.effect != TransitionEffect.NONE) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 8.dp, bottom = 8.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(NeonPurple.copy(alpha = 0.85f))
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "⇄ ${transition.effect.title}",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Selection Quick Toolbar Overlay
                selectedLayerId?.let { selId ->
                    val selLayer = layers.find { it.id == selId }
                    if (selLayer != null) {
                        LayerQuickToolbar(
                            layer = selLayer,
                            onBringForward = { onBringForward(selId) },
                            onSendBackward = { onSendBackward(selId) },
                            onDuplicate = { onDuplicateLayer(selId) },
                            onDelete = { onDeleteLayer(selId) },
                            onToggleLock = {
                                onUpdateLayer(selId) { it.copy(isLocked = !it.isLocked) }
                                onFinishTransform()
                            },
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 8.dp)
                        )
                    }
                }

                // Floating Top-Left Format Badge & Top-Right FPS Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${aspectRatio.label} • 9:16 REELS",
                            color = NeonCyan,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.6f))
                            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "60 FPS • 1080p",
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Tiered Watermark Overlay (FREE/BASIC: Translucent badge; PRO/PREMIUM: Watermark-free badge)
                if (!isWatermarkFreeTier(subscriptionTier)) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 8.dp, bottom = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xD905070D))
                            .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                            .testTag("free_tier_watermark")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(NeonCyan)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "OmniStudio AI Free | ENJEBEL TECH",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.3.sp
                            )
                        }
                    }
                } else {
                    val activeBadge = SubscriptionPlan.fromTierId(subscriptionTier).badgeTitle
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = 8.dp, bottom = 8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFFF59E0B).copy(alpha = 0.25f), NeonPurple.copy(alpha = 0.25f))
                                )
                            )
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                            .testTag("pro_tier_badge")
                    ) {
                        Text(
                            text = "$activeBadge • WATERMARK FREE",
                            color = Color(0xFFFBBF24),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CanvasBackground(preset: BackgroundPreset, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (preset) {
            BackgroundPreset.LUXURY_MARBLE -> {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF0A0C13), Color(0xFF161A26), Color(0xFF0A0C13)),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )
                // Marble gold veins
                drawLine(
                    color = Color(0x33D4AF37),
                    start = Offset(0f, h * 0.25f),
                    end = Offset(w, h * 0.5f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = Color(0x22D4AF37),
                    start = Offset(0f, h * 0.7f),
                    end = Offset(w, h * 0.85f),
                    strokeWidth = 2f
                )
            }
            BackgroundPreset.WHITE_STUDIO -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF2C3246), Color(0xFF141724), Color(0xFF0B0D14)),
                        center = Offset(w * 0.5f, h * 0.35f),
                        radius = w * 0.8f
                    )
                )
            }
            BackgroundPreset.NATURE_SUNBEAMS -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF1E281A), Color(0xFF0F180F), Color(0xFF060A06))
                    )
                )
                // Sunbeam ray
                drawLine(
                    brush = Brush.linearGradient(listOf(Color(0x44FFE28A), Color.Transparent)),
                    start = Offset(w * 0.8f, 0f),
                    end = Offset(w * 0.2f, h),
                    strokeWidth = w * 0.5f
                )
            }
            BackgroundPreset.FASHION_RUNWAY -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF350047), Color(0xFF13001C), Color(0xFF000000))
                    )
                )
                // Vertical spotlights
                drawLine(
                    color = Color(0x44FF2A85),
                    start = Offset(w * 0.2f, 0f),
                    end = Offset(w * 0.35f, h),
                    strokeWidth = 4f
                )
                drawLine(
                    color = Color(0x4400F0FF),
                    start = Offset(w * 0.8f, 0f),
                    end = Offset(w * 0.65f, h),
                    strokeWidth = 4f
                )
            }
            BackgroundPreset.CYBERPUNK_NEON -> {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF080616), Color(0xFF1B0733), Color(0xFF021B2B))
                    )
                )
                // Neon grid
                for (i in 0..12) {
                    val y = h * 0.5f + (h * 0.5f) * (i / 12f) * (i / 12f)
                    drawLine(
                        color = Color(0x3300F0FF),
                        start = Offset(0f, y),
                        end = Offset(w, y),
                        strokeWidth = 1.5f
                    )
                }
            }
            BackgroundPreset.GOLDEN_LUXURY -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF3D2C04), Color(0xFF181202), Color(0xFF000000)),
                        center = Offset(w * 0.5f, h * 0.45f),
                        radius = w * 0.75f
                    )
                )
            }
            BackgroundPreset.TECH_GRID -> {
                drawRect(color = Color(0xFF070B14))
                val step = w / 12f
                var x = 0f
                while (x <= w) {
                    drawLine(color = Color(0x1F00F0FF), start = Offset(x, 0f), end = Offset(x, h), strokeWidth = 1f)
                    x += step
                }
                var y = 0f
                while (y <= h) {
                    drawLine(color = Color(0x1F00F0FF), start = Offset(0f, y), end = Offset(w, y), strokeWidth = 1f)
                    y += step
                }
            }
            BackgroundPreset.LUXURY_BLACK_GOLD -> {
                drawRect(color = Color(0xFF070709))
                drawRect(
                    color = Color(0x66D4AF37),
                    topLeft = Offset(16f, 16f),
                    size = Size(w - 32f, h - 32f),
                    style = Stroke(width = 4f)
                )
            }
            BackgroundPreset.CUSTOM_PROMPT -> {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFF1F1235), Color(0xFF0C091A), Color(0xFF05030A))
                    )
                )
            }
        }
    }
}

@Composable
fun LayerQuickToolbar(
    layer: CanvasLayer,
    onBringForward: () -> Unit,
    onSendBackward: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onToggleLock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(StudioSurfaceElevated.copy(alpha = 0.95f))
            .border(1.dp, StudioBorder, RoundedCornerShape(20.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        IconButton(onClick = onBringForward, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ArrowUpward, contentDescription = "Forward", tint = TextPrimary, modifier = Modifier.size(14.dp))
        }
        IconButton(onClick = onSendBackward, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ArrowDownward, contentDescription = "Backward", tint = TextPrimary, modifier = Modifier.size(14.dp))
        }
        IconButton(onClick = onDuplicate, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", tint = TextPrimary, modifier = Modifier.size(14.dp))
        }
        IconButton(onClick = onToggleLock, modifier = Modifier.size(28.dp)) {
            Icon(
                if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                contentDescription = "Lock",
                tint = if (layer.isLocked) NeonPink else TextPrimary,
                modifier = Modifier.size(14.dp)
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = NeonPink, modifier = Modifier.size(14.dp))
        }
    }
}


