package com.example.ui.components

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AiCategory
import com.example.model.GeneratedAdCopy
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

@Composable
fun AiCopyGeneratorPanel(
    selectedCategory: AiCategory,
    onCategorySelected: (AiCategory) -> Unit,
    copy: GeneratedAdCopy,
    onAddTextToCanvas: (String, Boolean) -> Unit,
    onAddBadgeToCanvas: (String) -> Unit,
    onSpeakScript: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val lazyListState = rememberLazyListState()
    LazyColumn(
        state = lazyListState,
        modifier = modifier
            .fillMaxSize()
            .background(StudioSurface)
            .verticalScrollbar(lazyListState)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // Category Pills
        item {
            Column {
                Text(
                    text = "AI COPYWRITING ENGINE",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(AiCategory.entries) { cat ->
                        val isSelected = cat == selectedCategory
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) NeonCyan else StudioSurfaceElevated)
                                .border(1.dp, if (isSelected) NeonCyan else StudioBorder, RoundedCornerShape(8.dp))
                                .clickable { onCategorySelected(cat) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("category_pill_${cat.name}")
                        ) {
                            Text(
                                text = cat.displayName,
                                color = if (isSelected) Color.White else TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Section: Viral Hooks
        item {
            CopySectionHeader(title = "VIRAL PRODUCT HOOKS (5)", tagColor = NeonPink)
        }
        items(copy.hooks) { hook ->
            CopyCard(
                text = hook,
                onAddToCanvas = { onAddTextToCanvas(hook, true) },
                onSpeak = { onSpeakScript(hook) }
            )
        }

        // Section: Sales Headlines
        item {
            CopySectionHeader(title = "SALES HEADLINES (5)", tagColor = NeonCyan)
        }
        items(copy.headlines) { headline ->
            CopyCard(
                text = headline,
                onAddToCanvas = { onAddTextToCanvas(headline, true) },
                onSpeak = { onSpeakScript(headline) }
            )
        }

        // Section: High-Converting Offers
        item {
            CopySectionHeader(title = "OFFERS & BADGES (5)", tagColor = NeonPurple)
        }
        items(copy.offers) { offer ->
            CopyCard(
                text = offer,
                badgeMode = true,
                onAddToCanvas = { onAddBadgeToCanvas(offer) },
                onSpeak = { onSpeakScript(offer) }
            )
        }

        // Section: Full Ad Scripts with Voiceover
        item {
            CopySectionHeader(title = "30-SECOND AD SCRIPTS (5)", tagColor = OmniTheme.colors.accentGold)
        }
        items(copy.scripts) { script ->
            ScriptCard(
                script = script,
                onAddToCanvas = { onAddTextToCanvas(script, false) },
                onSpeak = { onSpeakScript(script) }
            )
        }

        // Section: CTA Variations
        item {
            CopySectionHeader(title = "CALL TO ACTION VARIATIONS (5)", tagColor = Color(0xFF10B981))
        }
        items(copy.ctas) { cta ->
            CopyCard(
                text = cta,
                onAddToCanvas = { onAddTextToCanvas(cta, false) },
                onSpeak = { onSpeakScript(cta) }
            )
        }
    }
}

@Composable
fun CopySectionHeader(title: String, tagColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(tagColor)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = tagColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
fun CopyCard(
    text: String,
    badgeMode: Boolean = false,
    onAddToCanvas: () -> Unit,
    onSpeak: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(StudioSurfaceElevated)
            .border(1.dp, StudioBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onSpeak, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.VolumeUp,
                    contentDescription = "Speak",
                    tint = NeonCyan,
                    modifier = Modifier.size(16.dp)
                )
            }
            IconButton(onClick = onAddToCanvas, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to Canvas",
                    tint = NeonPink,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ScriptCard(
    script: String,
    onAddToCanvas: () -> Unit,
    onSpeak: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(StudioSurfaceElevated)
            .border(1.dp, StudioBorder, RoundedCornerShape(10.dp))
            .padding(10.dp)
    ) {
        Text(
            text = script,
            color = TextPrimary,
            fontSize = 11.sp,
            lineHeight = 16.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2B2100))
                    .border(1.dp, Color(0xFFFFD700), RoundedCornerShape(6.dp))
                    .clickable { onSpeak() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = "Voiceover",
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Voiceover TTS", color = Color(0xFFFFD700), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(StudioBorder)
                    .clickable { onAddToCanvas() }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Insert",
                    tint = TextPrimary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Insert Text", color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
