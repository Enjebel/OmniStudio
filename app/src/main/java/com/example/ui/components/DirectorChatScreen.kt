package com.example.ui.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.model.AspectRatioOption
import com.example.model.DirectorMessage
import com.example.model.MotionTemplate
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonGradient
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

import com.example.model.SubscriptionPlan
import com.example.model.isUnlimitedAiTier
import com.example.ui.theme.GoldGlow
import com.example.ui.theme.GoldGradient

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DirectorChatScreen(
    messages: List<DirectorMessage>,
    isThinking: Boolean,
    aspectRatio: AspectRatioOption,
    motionTemplate: MotionTemplate,
    totalDurationSec: Float,
    clipCount: Int,
    subscriptionTier: String = "FREE",
    creditsRemaining: Int = 5,
    onSendPrompt: (prompt: String, mediaUri: String?, mediaName: String?) -> Unit,
    onJumpToStudio: () -> Unit,
    onOpenUpgradeModal: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var inputText by remember { mutableStateOf("") }
    var attachedUri by remember { mutableStateOf<String?>(null) }
    var attachedName by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()

    val mediaPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            attachedUri = uri.toString()
            attachedName = "Attached Media Frame"
        }
    }

    LaunchedEffect(messages.size, isThinking) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(StudioBackground)
            .imePadding()
    ) {
        val isCompactHeight = maxHeight < 420.dp
        val compactScroll = rememberScrollState()
        val containerMod = if (isCompactHeight) {
            Modifier
                .fillMaxWidth()
                .verticalScroll(compactScroll)
                .verticalScrollbar(compactScroll)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        } else {
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        }

        Column(modifier = containerMod) {
        // 1. Live Workspace Status Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(StudioSurface)
                .border(1.dp, StudioBorder, RoundedCornerShape(18.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(NeonGradient),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "DIRECTOR CHAT AI",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "${aspectRatio.label} • ${motionTemplate.title} • ${clipCount} clips (${String.format(Locale.US, "%.1fs", totalDurationSec)})",
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isUnlimited = isUnlimitedAiTier(subscriptionTier)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isUnlimited) GoldGradient
                            else Brush.linearGradient(listOf(StudioSurfaceElevated, StudioSurfaceElevated))
                        )
                        .border(1.dp, if (isUnlimited) GoldGlow else NeonPurple.copy(alpha = 0.55f), RoundedCornerShape(12.dp))
                        .clickable { onOpenUpgradeModal() }
                        .padding(horizontal = 8.dp, vertical = 5.dp)
                        .testTag("director_credits_pill")
                ) {
                    Text(
                        text = if (isUnlimited) SubscriptionPlan.fromTierId(subscriptionTier).badgeTitle else "$creditsRemaining CR",
                        color = if (isUnlimited) Color.Black else NeonCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(StudioSurfaceElevated)
                        .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .clickable { onJumpToStudio() }
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("director_view_studio_btn")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Preview",
                            color = NeonCyan,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Conversational Thread
        val listMod = if (isCompactHeight) {
            Modifier
                .fillMaxWidth()
                .height(220.dp)
        } else {
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = 160.dp)
        }
        LazyColumn(
            state = listState,
            modifier = listMod.verticalScrollbar(listState),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(messages, key = { it.id }) { msg ->
                DirectorMessageBubble(
                    message = msg,
                    onFollowUpClick = { followUp ->
                        onSendPrompt(followUp, null, null)
                    }
                )
            }

            if (isThinking) {
                item {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .background(StudioSurface)
                            .border(1.dp, NeonPurple.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = NeonCyan,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "AI Director is analyzing pacing & updating timeline...",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 3. Quick Director Presets Strip
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val quickCommands = listOf(
                "Make this punchier for TikTok",
                "Add smooth fade transition between clips",
                "Add slide transition to next clip",
                "Add zoom punch transition to all clips",
                "Cut out dead air",
                "Add a suspenseful transition here",
                "Switch to luxury gold showcase"
            )
            items(quickCommands) { cmd ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(StudioSurfaceElevated)
                        .border(1.dp, StudioBorder, RoundedCornerShape(14.dp))
                        .clickable(enabled = !isThinking) {
                            onSendPrompt(cmd, attachedUri, attachedName)
                            attachedUri = null
                            attachedName = null
                        }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = cmd,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 4. Attached Media Preview Chip (if picked)
        if (attachedUri != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(StudioSurfaceElevated)
                    .border(1.dp, NeonCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = attachedUri,
                        contentDescription = "Attached frame",
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = attachedName ?: "Attached Media",
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Ready for AI Director multimodal analysis",
                            color = NeonCyan,
                            fontSize = 9.sp
                        )
                    }
                }
                IconButton(
                    onClick = {
                        attachedUri = null
                        attachedName = null
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Remove media", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }

        // 5. Composer Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(StudioSurface)
                .border(1.dp, StudioBorder, RoundedCornerShape(22.dp))
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    mediaPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier
                    .size(40.dp)
                    .testTag("director_attach_media_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.AddPhotoAlternate,
                    contentDescription = "Attach raw photo or frame",
                    tint = NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = "Ask Director AI to cut, pace, or style...",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("director_chat_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isThinking) NeonGradient
                        else Brush.linearGradient(listOf(StudioSurfaceElevated, StudioSurfaceElevated))
                    )
                    .clickable(enabled = inputText.isNotBlank() && !isThinking) {
                        val prompt = inputText.trim()
                        val uri = attachedUri
                        val name = attachedName
                        inputText = ""
                        attachedUri = null
                        attachedName = null
                        onSendPrompt(prompt, uri, name)
                    }
                    .testTag("director_send_btn"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send to Director",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DirectorMessageBubble(
    message: DirectorMessage,
    onFollowUpClick: (String) -> Unit
) {
    val palette = OmniTheme.colors
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (message.isUser) Alignment.End else Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (message.isUser) 20.dp else 6.dp,
                        bottomEnd = if (message.isUser) 6.dp else 20.dp
                    )
                )
                .background(
                    when {
                        message.isError -> palette.errorContainer
                        message.isUser -> StudioSurfaceElevated
                        else -> StudioSurface
                    }
                )
                .border(
                    width = 1.dp,
                    color = when {
                        message.isError -> DangerRed.copy(alpha = 0.6f)
                        message.isUser -> palette.accentPurple.copy(alpha = 0.5f)
                        else -> StudioBorder
                    },
                    shape = RoundedCornerShape(
                        topStart = 20.dp,
                        topEnd = 20.dp,
                        bottomStart = if (message.isUser) 20.dp else 6.dp,
                        bottomEnd = if (message.isUser) 6.dp else 20.dp
                    )
                )
                .padding(12.dp)
        ) {
            Column {
                if (!message.isUser) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (message.isError) Icons.Default.ErrorOutline else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (message.isError) DangerRed else palette.accentCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (message.isError) "DIRECTOR DIAGNOSTIC" else "OMNISTUDIO DIRECTOR",
                            color = if (message.isError) DangerRed else palette.accentCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.6.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                }

                if (message.attachedMediaUri != null) {
                    AsyncImage(
                        model = message.attachedMediaUri,
                        contentDescription = message.attachedMediaName,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                if (message.executedActions.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        message.executedActions.forEach { action ->
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberGreen.copy(alpha = 0.16f))
                                    .border(1.dp, CyberGreen.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = CyberGreen,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = action,
                                    color = CyberGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        if (message.followUpSuggestions.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                message.followUpSuggestions.forEach { suggestion ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(NeonPurple.copy(alpha = 0.16f))
                            .border(1.dp, NeonPurple.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                            .clickable { onFollowUpClick(suggestion) }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = suggestion,
                            color = TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
