package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.AspectRatioOption
import com.example.model.BackgroundPreset
import com.example.model.CanvasLayer
import com.example.model.LightingSettings
import com.example.model.MotionTemplate
import com.example.model.SubscriptionPlan
import com.example.model.TimelineClip
import com.example.model.isPaidTier
import com.example.model.isWatermarkFreeTier
import com.example.studio.ExportEngine
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OmniTheme
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
fun ExportDialog(
    layers: List<CanvasLayer>,
    bgPreset: BackgroundPreset,
    lighting: LightingSettings,
    aspectRatio: AspectRatioOption,
    motionTemplate: MotionTemplate = MotionTemplate.DYNAMIC_ZOOM_PUNCH,
    motionSpeed: Float = 1.0f,
    motionIntensity: Float = 1.0f,
    totalDurationSec: Float = 5.0f,
    subscriptionTier: String = "FREE",
    timelineClips: List<TimelineClip> = emptyList(),
    onUpgradeToPro: () -> Unit = {},
    onSaveToCloud: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Default to Web-Compatible MP4 Video (1), while Still Photo (0) remains selectable
    var exportType by remember { mutableIntStateOf(1) }
    var selectedFormat by remember { mutableStateOf(ExportEngine.ExportFormat.PNG) }
    var quality by remember { mutableFloatStateOf(95f) }

    val defaultResolution = if (isPaidTier(subscriptionTier)) 1080 else 720
    var selectedResolutionShortEdge by remember(subscriptionTier) { mutableIntStateOf(defaultResolution) }
    var selectedFps by remember { mutableIntStateOf(30) }
    var selectedDurationSec by remember(totalDurationSec) {
        mutableFloatStateOf(
            when {
                totalDurationSec >= 9f -> 10f
                totalDurationSec >= 7f -> 8f
                else -> 5f
            }
        )
    }

    var isExporting by remember { mutableStateOf(false) }
    var exportProgress by remember { mutableFloatStateOf(0f) }
    var exportStatusText by remember { mutableStateOf("") }
    var exportError by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val plan = SubscriptionPlan.fromTierId(subscriptionTier)
    val isWatermarkFree = isWatermarkFreeTier(subscriptionTier)
    val alignedVideoDims = remember(aspectRatio, selectedResolutionShortEdge) {
        ExportEngine.alignVideoDimensions(aspectRatio, selectedResolutionShortEdge)
    }

    Dialog(onDismissRequest = { if (!isExporting) onDismiss() }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, StudioBorder, RoundedCornerShape(16.dp)),
            color = StudioSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 660.dp)
                    .verticalScroll(scrollState)
                    .verticalScrollbar(scrollState)
                    .padding(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXPORT AD CAMPAIGN",
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = { if (!isExporting) onDismiss() },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Type Toggle: Still Photo vs Web Video (.MP4)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioSurfaceElevated)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Still Photo", "Web Video (.MP4)").forEachIndexed { index, title ->
                        val isSel = exportType == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) NeonCyan else Color.Transparent)
                                .clickable(enabled = !isExporting) {
                                    exportType = index
                                    exportError = null
                                }
                                .padding(vertical = 8.dp)
                                .testTag("export_mode_${if (index == 0) "photo" else "mp4"}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSel) Color.White else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (exportType == 0) {
                    // Image Format Options
                    Text("Image Format", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ExportEngine.ExportFormat.entries.forEach { format ->
                            val isSel = selectedFormat == format
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) StudioSurfaceElevated else StudioSurface)
                                    .border(1.dp, if (isSel) NeonCyan else StudioBorder, RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isExporting) { selectedFormat = format }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = format.extension.uppercase(Locale.US),
                                    color = if (isSel) NeonCyan else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quality Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Compression Quality", color = TextSecondary, fontSize = 11.sp)
                        Text("${quality.toInt()}%", color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = quality,
                        onValueChange = { quality = it },
                        valueRange = 60f..100f,
                        colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
                    )
                } else {
                    // Web-Compatible H.264 MP4 Video Options
                    Text("Resolution", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(720 to "720p HD", 1080 to "1080p Full HD").forEach { (resVal, label) ->
                            val isSel = selectedResolutionShortEdge == resVal
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) StudioSurfaceElevated else StudioSurface)
                                    .border(1.dp, if (isSel) NeonCyan else StudioBorder, RoundedCornerShape(8.dp))
                                    .clickable(enabled = !isExporting) { selectedResolutionShortEdge = resVal }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    color = if (isSel) NeonCyan else TextPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Frame Rate", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(24, 30, 60).forEach { fpsOpt ->
                                    val isSel = selectedFps == fpsOpt
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) StudioSurfaceElevated else StudioSurface)
                                            .border(1.dp, if (isSel) NeonPurple else StudioBorder, RoundedCornerShape(8.dp))
                                            .clickable(enabled = !isExporting) { selectedFps = fpsOpt }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "${fpsOpt}",
                                            color = if (isSel) NeonCyan else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text("Duration", color = TextSecondary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(5f to "5s", 8f to "8s", 10f to "10s").forEach { (durVal, label) ->
                                    val isSel = selectedDurationSec == durVal
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSel) StudioSurfaceElevated else StudioSurface)
                                            .border(1.dp, if (isSel) NeonPurple else StudioBorder, RoundedCornerShape(8.dp))
                                            .clickable(enabled = !isExporting) { selectedDurationSec = durVal }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSel) NeonCyan else TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Video Spec Summary
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioSurfaceElevated)
                            .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("H.264 / AVC Web-Compatible MP4", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text(
                            text = "• Output: ${alignedVideoDims.first} x ${alignedVideoDims.second} (${aspectRatio.label}) @ $selectedFps FPS",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Text(
                            text = "• Motion: ${motionTemplate.title} (${selectedDurationSec.toInt()}s loop)",
                            color = TextSecondary,
                            fontSize = 10.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CyberGreen, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Ready for Chrome, Safari, TikTok, Reels & YouTube Shorts",
                                color = CyberGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tiered Watermark Information Card
                if (!isWatermarkFree) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioSurfaceElevated)
                            .border(1.dp, Color(0x3300F0FF), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${plan.badgeTitle} TIER",
                                        color = OmniTheme.colors.accentCyan,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Watermark included",
                                        color = TextMuted,
                                        fontSize = 10.sp
                                    )
                                }

                                Text(
                                    text = "Remove Watermark",
                                    color = OmniTheme.colors.accentGold,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clickable(enabled = !isExporting) { onUpgradeToPro() }
                                        .padding(4.dp)
                                        .testTag("upgrade_pro_export_button")
                                )
                            }
                            Text(
                                text = "Upgrade to Pro ($15/mo) or Premium ($20/mo) for clean 1080p/4K 60 FPS exports.",
                                color = TextSecondary,
                                fontSize = 9.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF332006), Color(0xFF130924))
                                )
                            )
                            .border(1.dp, Color(0xFFF59E0B), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Text(
                                    text = "OMNISTUDIO ${plan.badgeTitle} ACTIVE",
                                    color = Color(0xFFFBBF24),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black
                                )
                                Text(
                                    text = "Watermark removed • ${plan.maxResolutionLabel}",
                                    color = Color.White,
                                    fontSize = 9.sp
                                )
                            }
                        }
                    }
                }

                if (exportError != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(OmniTheme.colors.errorContainer)
                            .border(1.dp, DangerRed.copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(exportError.orEmpty(), color = OmniTheme.colors.onErrorContainer, fontSize = 11.sp)
                    }
                }

                if (isExporting) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(StudioSurfaceElevated)
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(exportStatusText, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text("${(exportProgress * 100).toInt()}%", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        LinearProgressIndicator(
                            progress = { exportProgress.coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = NeonCyan,
                            trackColor = StudioSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Save MP4 / Image & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            exportError = null
                            scope.launch {
                                isExporting = true
                                exportProgress = 0.05f

                                if (exportType == 1) {
                                    exportStatusText = "Encoding H.264 MP4..."
                                    val bitrate = if (selectedResolutionShortEdge >= 1080) 7_000_000 else 4_500_000
                                    val videoSpec = ExportEngine.VideoExportSpec(
                                        widthPx = alignedVideoDims.first,
                                        heightPx = alignedVideoDims.second,
                                        fps = selectedFps,
                                        durationSec = selectedDurationSec,
                                        bitrateBps = bitrate
                                    )
                                    val result = ExportEngine.exportCanvasToMp4File(
                                        context = context,
                                        layers = layers,
                                        bgPreset = bgPreset,
                                        lighting = lighting,
                                        aspectRatio = aspectRatio,
                                        motionTemplate = motionTemplate,
                                        motionSpeed = motionSpeed,
                                        motionIntensity = motionIntensity,
                                        subscriptionTier = subscriptionTier,
                                        spec = videoSpec,
                                        timelineClips = timelineClips,
                                        onProgress = { pct -> exportProgress = pct }
                                    )
                                    result.fold(
                                        onSuccess = { mp4File ->
                                            exportStatusText = "Saving MP4 to Movies..."
                                            val savedUri = withContext(Dispatchers.IO) {
                                                ExportEngine.saveVideoToGallery(context, mp4File)
                                            }
                                            isExporting = false
                                            if (savedUri != null) {
                                                Toast.makeText(
                                                    context,
                                                    "MP4 video saved to Movies/OmniStudio!",
                                                    Toast.LENGTH_LONG
                                                ).show()
                                                onDismiss()
                                            } else {
                                                exportError = "MP4 encoded, but MediaStore save failed."
                                            }
                                        },
                                        onFailure = { err ->
                                            isExporting = false
                                            exportError = err.localizedMessage ?: "Failed to encode MP4 video."
                                        }
                                    )
                                } else {
                                    exportStatusText = "Rendering ${selectedFormat.extension.uppercase(Locale.US)}..."
                                    val savedUri = withContext(Dispatchers.Default) {
                                        val bitmap = ExportEngine.renderCanvasToBitmap(
                                            context = context,
                                            layers = layers,
                                            bgPreset = bgPreset,
                                            lighting = lighting,
                                            aspectRatio = aspectRatio,
                                            subscriptionTier = subscriptionTier,
                                            motionTemplate = motionTemplate,
                                            motionSpeed = motionSpeed,
                                            motionIntensity = motionIntensity
                                        )
                                        exportProgress = 0.7f
                                        ExportEngine.saveBitmapToGallery(
                                            context = context,
                                            bitmap = bitmap,
                                            format = selectedFormat,
                                            quality = quality.toInt()
                                        )
                                    }
                                    exportProgress = 1f
                                    isExporting = false
                                    if (savedUri != null) {
                                        Toast.makeText(context, "Saved to Pictures/OmniStudio!", Toast.LENGTH_LONG).show()
                                        onDismiss()
                                    } else {
                                        exportError = "Unable to save image to Pictures/OmniStudio."
                                    }
                                }
                            }
                        },
                        enabled = !isExporting,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_gallery_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (exportType == 1) "Save MP4" else "Save Image",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            exportError = null
                            scope.launch {
                                isExporting = true
                                exportProgress = 0.05f

                                if (exportType == 1) {
                                    exportStatusText = "Rendering MP4 share file..."
                                    val bitrate = if (selectedResolutionShortEdge >= 1080) 7_000_000 else 4_500_000
                                    val videoSpec = ExportEngine.VideoExportSpec(
                                        widthPx = alignedVideoDims.first,
                                        heightPx = alignedVideoDims.second,
                                        fps = selectedFps,
                                        durationSec = selectedDurationSec,
                                        bitrateBps = bitrate
                                    )
                                    val result = ExportEngine.exportCanvasToMp4File(
                                        context = context,
                                        layers = layers,
                                        bgPreset = bgPreset,
                                        lighting = lighting,
                                        aspectRatio = aspectRatio,
                                        motionTemplate = motionTemplate,
                                        motionSpeed = motionSpeed,
                                        motionIntensity = motionIntensity,
                                        subscriptionTier = subscriptionTier,
                                        spec = videoSpec,
                                        timelineClips = timelineClips,
                                        onProgress = { pct -> exportProgress = pct }
                                    )
                                    isExporting = false
                                    result.fold(
                                        onSuccess = { mp4File ->
                                            val launched = ExportEngine.shareVideo(
                                                context = context,
                                                videoFile = mp4File,
                                                caption = "Created with OmniStudio AI by ENJEBEL TECH"
                                            )
                                            if (launched) {
                                                onDismiss()
                                            } else {
                                                exportError = "No share target available for MP4 video."
                                            }
                                        },
                                        onFailure = { err ->
                                            exportError = err.localizedMessage ?: "Unable to export shareable MP4."
                                        }
                                    )
                                } else {
                                    exportStatusText = "Generating share package..."
                                    val bitmap = withContext(Dispatchers.Default) {
                                        ExportEngine.renderCanvasToBitmap(
                                            context = context,
                                            layers = layers,
                                            bgPreset = bgPreset,
                                            lighting = lighting,
                                            aspectRatio = aspectRatio,
                                            subscriptionTier = subscriptionTier,
                                            motionTemplate = motionTemplate,
                                            motionSpeed = motionSpeed,
                                            motionIntensity = motionIntensity
                                        )
                                    }
                                    exportProgress = 1f
                                    ExportEngine.shareBitmap(
                                        context = context,
                                        bitmap = bitmap,
                                        caption = "Created with OmniStudio AI by ENJEBEL TECH"
                                    )
                                    isExporting = false
                                    onDismiss()
                                }
                            }
                        },
                        enabled = !isExporting,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("share_ad_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (exportType == 1) "Share MP4" else "Share Ad",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cloud Save option
                Button(
                    onClick = {
                        onSaveToCloud()
                        Toast.makeText(context, "Campaign saved to Firestore Cloud History!", Toast.LENGTH_SHORT).show()
                    },
                    enabled = !isExporting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("save_to_cloud_export_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = StudioSurfaceElevated),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = "Cloud Save", tint = NeonCyan, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Campaign to Cloud History", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
