package com.example.ui.components

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AdFramework
import com.example.model.AspectRatioOption
import com.example.model.SubscriptionPlan
import com.example.model.UserProfile
import com.example.model.isPaidTier
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.GoldGradient
import com.example.ui.theme.LuxuryGold
import com.example.ui.theme.NeonGradient
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonTriGradient
import com.example.ui.theme.OmniTheme
import com.example.ui.theme.StudioBorder
import com.example.ui.theme.StudioSurface
import com.example.ui.theme.StudioSurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun StudioTopBar(
    aspectRatio: AspectRatioOption,
    onRatioSelected: (AspectRatioOption) -> Unit,
    currentFramework: AdFramework,
    onFrameworkSelected: (AdFramework) -> Unit,
    userProfile: UserProfile,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    zoomLevel: Float,
    onZoomSelected: (Float) -> Unit,
    onExportClicked: () -> Unit,
    onSaveCloudClicked: () -> Unit = {},
    onSignOutClicked: () -> Unit = {},
    onOpenUpgradeClicked: () -> Unit = {},
    onOpenProfileSidebar: () -> Unit = {},
    isDarkTheme: Boolean = true,
    onToggleTheme: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var ratioMenuExpanded by remember { mutableStateOf(false) }
    val plan = SubscriptionPlan.fromTierId(userProfile.subscriptionTier)
    val isPaid = isPaidTier(plan.id)
    val palette = OmniTheme.colors

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(StudioSurface)
            .border(width = 1.dp, color = StudioBorder)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Brand & Profile Avatar Trigger -> Opens Slide-out Profile & Settings Sidebar
        Box(modifier = Modifier.weight(1f, fill = false)) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onOpenProfileSidebar() }
                    .padding(2.dp)
                    .testTag("user_profile_badge"),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(NeonTriGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Open Profile & Settings Sidebar",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Omni",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Text(
                            text = "Studio",
                            color = palette.accentCyan,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = userProfile.name.ifBlank { "Creator" },
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Right Controls: Theme Toggle + Tier Pill + Ratio + Undo/Redo + Export
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Easy-Access Theme Toggle Button
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StudioSurfaceElevated)
                    .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                    .pressScaleClickable { onToggleTheme() }
                    .testTag("theme_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                    tint = if (isDarkTheme) GoldGlow else palette.accentPurple,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Tier / Upgrade Pill
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPaid) GoldGradient
                        else if (isDarkTheme) Brush.horizontalGradient(listOf(Color(0xFF2D1F07), Color(0xFF1F1638)))
                        else Brush.horizontalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFEDE9FE)))
                    )
                    .border(
                        width = 1.dp,
                        color = if (isPaid) GoldGlow else LuxuryGold.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .pressScaleClickable { onOpenUpgradeClicked() }
                    .padding(horizontal = 7.dp)
                    .testTag("top_bar_tier_pill"),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = if (isPaid) Color.Black else palette.accentGold,
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isPaid) plan.badgeTitle else "UPGRADE",
                        color = if (isPaid) Color.Black else palette.accentGold,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            // Aspect Ratio Picker
            Box {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .testTag("aspect_ratio_picker")
                        .clip(RoundedCornerShape(10.dp))
                        .background(StudioSurfaceElevated)
                        .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
                        .clickable { ratioMenuExpanded = true }
                        .padding(horizontal = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = aspectRatio.label,
                        color = palette.accentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                DropdownMenu(
                    expanded = ratioMenuExpanded,
                    onDismissRequest = { ratioMenuExpanded = false },
                    modifier = Modifier
                        .background(StudioSurfaceElevated)
                        .border(1.dp, StudioBorder, RoundedCornerShape(12.dp))
                ) {
                    AspectRatioOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(option.label, color = if (option == aspectRatio) palette.accentCyan else TextPrimary, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(option.subtitle, color = TextSecondary, fontSize = 11.sp)
                                }
                            },
                            onClick = {
                                onRatioSelected(option)
                                ratioMenuExpanded = false
                            }
                        )
                    }
                }
            }

            IconButton(
                onClick = onUndo,
                enabled = canUndo,
                modifier = Modifier
                    .size(26.dp)
                    .testTag("undo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Undo,
                    contentDescription = "Undo",
                    tint = if (canUndo) TextPrimary else TextMuted,
                    modifier = Modifier.size(15.dp)
                )
            }

            IconButton(
                onClick = onRedo,
                enabled = canRedo,
                modifier = Modifier
                    .size(26.dp)
                    .testTag("redo_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Redo,
                    contentDescription = "Redo",
                    tint = if (canRedo) TextPrimary else TextMuted,
                    modifier = Modifier.size(15.dp)
                )
            }

            // Glowing Export Pill
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .testTag("export_button")
                    .clip(RoundedCornerShape(12.dp))
                    .background(NeonGradient)
                    .pressScaleClickable { onExportClicked() }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileDownload,
                        contentDescription = "Export",
                        tint = Color.White,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "Export",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
