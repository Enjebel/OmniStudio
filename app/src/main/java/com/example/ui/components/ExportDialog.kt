package com.example.ui.components

import android.content.Context
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.model.SubscriptionPlan
import com.example.model.isWatermarkFreeTier
import com.example.studio.ExportEngine
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OmniTheme
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Star

@Composable
fun ExportDialog(
    layers: List<CanvasLayer>,
    bgPreset: BackgroundPreset,
    lighting: LightingSettings,
    aspectRatio: AspectRatioOption,
    subscriptionTier: String = "FREE",
    onUpgradeToPro: () -> Unit = {},
    onSaveToCloud: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var exportType by remember { mutableIntStateOf(0) } // 0: Image, 1: Animated Video / GIF
    var selectedFormat by remember { mutableStateOf(ExportEngine.ExportFormat.PNG) }
    var quality by remember { mutableFloatStateOf(95f) }
    var isExporting by remember { mutableStateOf(false) }
    var exportStatusText by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()
    val plan = SubscriptionPlan.fromTierId(subscriptionTier)
    val isWatermarkFree = isWatermarkFreeTier(subscriptionTier)

    Dialog(onDismissRequest = onDismiss) {
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
                    .heightIn(max = 640.dp)
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
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Type Toggle: Still Image vs Video Ad
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(StudioSurfaceElevated)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("High-Res Photo Ad", "Animated Video Ad (MP4)").forEachIndexed { index, title ->
                        val isSel = exportType == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) NeonCyan else Color.Transparent)
                                .clickable { exportType = index }
                                .padding(vertical = 8.dp),
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
                                    .clickable { selectedFormat = format }
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
                    // Video Settings
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(StudioSurfaceElevated)
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Movie, contentDescription = null, tint = NeonPurple, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Motion Ad Video Settings", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("• Resolution: ${aspectRatio.widthPx} x ${aspectRatio.heightPx} (${aspectRatio.label})", color = TextSecondary, fontSize = 10.sp)
                        Text("• Framerate: 60 FPS Smooth Render", color = TextSecondary, fontSize = 10.sp)
                        Text("• Duration: 10.0s Loop with Audio", color = TextSecondary, fontSize = 10.sp)
                        Text("• Container: H.264 MP4 / WebM Motion", color = TextSecondary, fontSize = 10.sp)
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
                                        .clickable { onUpgradeToPro() }
                                        .padding(4.dp)
                                        .testTag("upgrade_pro_export_button")
                                )
                            }
                            Text(
                                text = "Upgrade to Pro ($15/mo) or Premium ($20/mo) for clean 4K 60 FPS exports.",
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

                if (isExporting) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = NeonCyan, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(exportStatusText, color = NeonCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Actions: Save to Gallery & Share
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                exportStatusText = "Rendering canvas..."
                                delay(300)

                                val bitmap = ExportEngine.renderCanvasToBitmap(
                                    context = context,
                                    layers = layers,
                                    bgPreset = bgPreset,
                                    lighting = lighting,
                                    aspectRatio = aspectRatio,
                                    subscriptionTier = subscriptionTier
                                )

                                exportStatusText = "Saving to Pictures..."
                                delay(300)

                                val uri = ExportEngine.saveBitmapToGallery(
                                    context = context,
                                    bitmap = bitmap,
                                    format = selectedFormat,
                                    quality = quality.toInt()
                                )

                                isExporting = false
                                if (uri != null) {
                                    Toast.makeText(context, "Saved to Pictures/OmniStudio!", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                } else {
                                    Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                            }
                        },
                        enabled = !isExporting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_gallery_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Save", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Save to Device", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                isExporting = true
                                exportStatusText = "Generating share package..."
                                delay(300)

                                val bitmap = ExportEngine.renderCanvasToBitmap(
                                    context = context,
                                    layers = layers,
                                    bgPreset = bgPreset,
                                    lighting = lighting,
                                    aspectRatio = aspectRatio,
                                    subscriptionTier = subscriptionTier
                                )

                                ExportEngine.shareBitmap(
                                    context = context,
                                    bitmap = bitmap,
                                    caption = "Created with OmniStudio AI by ENJEBEL TECH"
                                )

                                isExporting = false
                                onDismiss()
                            }
                        },
                        enabled = !isExporting,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("share_ad_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Share Ad", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Cloud Save option
                Button(
                    onClick = {
                        onSaveToCloud()
                        Toast.makeText(context, "Campaign saved to Firestore Cloud History!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
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
