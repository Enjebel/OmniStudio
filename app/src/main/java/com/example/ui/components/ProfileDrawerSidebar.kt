package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DynamicFeed
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MovieCreation
import androidx.compose.material.icons.filled.PhotoFilter
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.SubscriptionPlan
import com.example.model.UserProfile
import com.example.model.isPaidTier
import com.example.model.isUnlimitedAiTier
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
import com.example.viewmodel.OmniModule

@Composable
fun ProfileDrawerSidebar(
    userProfile: UserProfile,
    activeModule: OmniModule,
    isDarkTheme: Boolean,
    onToggleTheme: (Boolean) -> Unit,
    onSelectModule: (OmniModule) -> Unit,
    onOpenPaywall: () -> Unit,
    onSaveToCloud: () -> Unit,
    onSignOut: () -> Unit,
    onCloseDrawer: () -> Unit
) {
    val palette = OmniTheme.colors
    val scrollState = rememberScrollState()
    val plan = SubscriptionPlan.fromTierId(userProfile.subscriptionTier)
    val isPaid = isPaidTier(plan.id)
    val isUnlimited = isUnlimitedAiTier(plan.id)

    ModalDrawerSheet(
        modifier = Modifier
            .width(310.dp)
            .fillMaxHeight()
            .testTag("profile_drawer_sidebar"),
        drawerContainerColor = StudioBackground,
        drawerContentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .verticalScrollbar(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Brand + Close Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(NeonTriGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("OS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "OMNISTUDIO AI",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(
                            text = "ENJEBEL TECH SUITE",
                            color = palette.accentCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                IconButton(
                    onClick = onCloseDrawer,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(StudioSurfaceElevated)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Menu",
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // 1. Creator Profile Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(StudioSurface)
                    .border(1.dp, StudioBorder, RoundedCornerShape(22.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val initials = userProfile.name
                        .split(" ")
                        .filter { it.isNotBlank() }
                        .take(2)
                        .joinToString("") { it.first().uppercaseChar().toString() }
                        .ifBlank { "OS" }

                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(NeonGradient),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userProfile.name.ifBlank { "OmniStudio Creator" },
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = userProfile.email.ifBlank { "enjebeln@gmail.com" },
                            color = TextSecondary,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Current Subscription Tier Badge
                val tierLabel = when (plan) {
                    SubscriptionPlan.FREE -> "FREE PLAN • WATERMARKED"
                    SubscriptionPlan.BASIC -> "$5 BASIC • ${userProfile.billingCycle}"
                    SubscriptionPlan.PRO -> "$15 PRO • ${userProfile.billingCycle}"
                    SubscriptionPlan.PREMIUM -> "$20 PREMIUM • ${userProfile.billingCycle}"
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isPaid) GoldGradient
                            else Brush.linearGradient(listOf(StudioSurfaceElevated, StudioSurfaceElevated))
                        )
                        .border(
                            1.dp,
                            if (isPaid) GoldGlow else StudioBorder,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            onCloseDrawer()
                            onOpenPaywall()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("sidebar_tier_badge"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isPaid) Icons.Default.WorkspacePremium else Icons.Default.Star,
                            contentDescription = null,
                            tint = if (isPaid) Color.Black else palette.accentGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = tierLabel,
                            color = if (isPaid) Color.Black else TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black
                        )
                    }

                    Text(
                        text = if (isPaid) "Manage" else "Upgrade",
                        color = if (isPaid) Color.Black else palette.accentCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Remaining Daily AI Credits Tracker
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StudioSurfaceElevated)
                        .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = palette.accentCyan,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Daily AI Credits",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = if (isUnlimited) "UNLIMITED" else "${userProfile.aiCreditsRemaining} / ${plan.dailyAiCredits}",
                            color = if (isUnlimited) palette.accentGold else palette.accentCyan,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.testTag("sidebar_credits_text")
                        )
                    }

                    val progress = if (isUnlimited) 1f else {
                        (userProfile.aiCreditsRemaining.toFloat() / plan.dailyAiCredits.coerceAtLeast(1)).coerceIn(0f, 1f)
                    }
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = if (isUnlimited) GoldGlow else palette.accentCyan,
                        trackColor = StudioBorder
                    )
                    Text(
                        text = if (isUnlimited) "Unlimited Muse AI & Director sessions active" else "Resets daily • Used across Muse AI & Director Chat",
                        color = TextMuted,
                        fontSize = 9.sp
                    )
                }
            }

            // 2. Dynamic Light & OLED Dark Theme Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StudioSurface)
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
                    .clickable { onToggleTheme(!isDarkTheme) }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
                    .testTag("sidebar_theme_switch_row"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isDarkTheme) palette.accentPurple.copy(alpha = 0.18f)
                                else palette.accentGold.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                            contentDescription = "Theme Mode",
                            tint = if (isDarkTheme) palette.accentPurple else palette.accentGold,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isDarkTheme) "OLED Dark Mode" else "Pristine Light Mode",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Synced locally & to Cloud Firestore",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                }

                Switch(
                    checked = isDarkTheme,
                    onCheckedChange = { onToggleTheme(it) },
                    modifier = Modifier.testTag("sidebar_theme_switch"),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonPurple,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = TextMuted
                    )
                )
            }

            // 3. Workspace Navigation Links
            Text(
                text = "CREATOR WORKSPACES",
                color = TextMuted,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(StudioSurface)
                    .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
                    .padding(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DrawerNavItem(
                    icon = Icons.Default.Movie,
                    label = "Video & Ad Studio",
                    subtitle = "Canvas & multi-track timeline",
                    selected = activeModule == OmniModule.STUDIO,
                    onClick = {
                        onSelectModule(OmniModule.STUDIO)
                        onCloseDrawer()
                    }
                )
                DrawerNavItem(
                    icon = Icons.Default.Psychology,
                    label = "Director AI Chat",
                    subtitle = "Conversational timeline editor",
                    selected = activeModule == OmniModule.DIRECTOR,
                    onClick = {
                        onSelectModule(OmniModule.DIRECTOR)
                        onCloseDrawer()
                    }
                )
                DrawerNavItem(
                    icon = Icons.Default.MovieCreation,
                    label = "Muse AI Generator",
                    subtitle = "Text & Image-to-Video camera rig",
                    selected = activeModule == OmniModule.MUSE_AI,
                    onClick = {
                        onSelectModule(OmniModule.MUSE_AI)
                        onCloseDrawer()
                    }
                )
                DrawerNavItem(
                    icon = Icons.AutoMirrored.Filled.CompareArrows,
                    label = "Transitions",
                    subtitle = "Fade, slide & zoom clip cuts",
                    selected = activeModule == OmniModule.TRANSITIONS,
                    onClick = {
                        onSelectModule(OmniModule.TRANSITIONS)
                        onCloseDrawer()
                    }
                )
                DrawerNavItem(
                    icon = Icons.Default.PhotoFilter,
                    label = "Filters",
                    subtitle = "CSS color grading presets",
                    selected = activeModule == OmniModule.FILTERS,
                    onClick = {
                        onSelectModule(OmniModule.FILTERS)
                        onCloseDrawer()
                    }
                )
                DrawerNavItem(
                    icon = Icons.Default.AutoFixHigh,
                    label = "Background Removal Studio",
                    subtitle = "AI matte & Chroma Key video studio",
                    selected = activeModule == OmniModule.BG_REMOVAL,
                    onClick = {
                        onSelectModule(OmniModule.BG_REMOVAL)
                        onCloseDrawer()
                    }
                )
                DrawerNavItem(
                    icon = Icons.Default.DynamicFeed,
                    label = "Campaign Feed & History",
                    subtitle = "Saved cloud productions",
                    selected = activeModule == OmniModule.FEED,
                    onClick = {
                        onSelectModule(OmniModule.FEED)
                        onCloseDrawer()
                    }
                )
            }

            HorizontalDivider(color = StudioBorder)

            // 4. Account & Cloud Actions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DrawerActionRow(
                    icon = Icons.Default.CloudUpload,
                    label = "Sync Current Project to Cloud",
                    accentColor = palette.accentCyan,
                    onClick = {
                        onSaveToCloud()
                        onCloseDrawer()
                    },
                    testTag = "sidebar_sync_cloud_btn"
                )
                DrawerActionRow(
                    icon = Icons.Default.WorkspacePremium,
                    label = if (isPaid) "Manage Membership Plans" else "Unlock Basic / Pro / Premium",
                    accentColor = palette.accentGold,
                    onClick = {
                        onCloseDrawer()
                        onOpenPaywall()
                    },
                    testTag = "sidebar_upgrade_btn"
                )
                DrawerActionRow(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    label = "Sign Out",
                    accentColor = NeonPink,
                    onClick = {
                        onCloseDrawer()
                        onSignOut()
                    },
                    testTag = "sidebar_sign_out_btn"
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun DrawerNavItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val palette = OmniTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (selected) palette.accentCyan.copy(alpha = 0.14f) else Color.Transparent
            )
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) palette.accentCyan.copy(alpha = 0.45f) else Color.Transparent,
                shape = RoundedCornerShape(14.dp)
            )
            .pressScaleClickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (selected) palette.accentCyan else TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                color = if (selected) palette.accentCyan else TextPrimary,
                fontSize = 12.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = subtitle,
                color = TextMuted,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun DrawerActionRow(
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit,
    testTag: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(StudioSurface)
            .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
            .pressScaleClickable { onClick() }
            .padding(horizontal = 12.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = label,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
