package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.GeneratedImageItem
import com.example.ui.components.NeonCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ImageGenScreen(
    modifier: Modifier = Modifier,
    language: AppLanguage,
    promptInput: String,
    selectedStyle: String,
    selectedAspectRatio: String,
    isGenerating: Boolean,
    successMessage: String?,
    gallery: List<GeneratedImageItem>,
    onPromptChanged: (String) -> Unit,
    onStyleSelected: (String) -> Unit,
    onAspectRatioSelected: (String) -> Unit,
    onGenerate: () -> Unit,
    onClearSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    LaunchedEffect(successMessage) {
        if (successMessage != null) {
            Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show()
            onClearSuccess()
        }
    }

    val styleOptions = listOf(
        "Cyberpunk Neon",
        "Hyper-realistic",
        "3D Hologram",
        "Anime Sci-Fi",
        "Fantasy Glow"
    )

    val aspectRatios = listOf("1:1", "16:9", "9:16", "4:3")

    val promptSuggestions = if (language == AppLanguage.BENGALI) {
        listOf(
            "নিয়ন আলোয় ঢাকা সাইবার শহর 🏙️",
            "ভবিষ্যতমুখী সাইবার রোবট নারী 🤖",
            "হোলোগ্রাফিক পদ্ম ফুল ও নিয়ন নদী 🪷",
            "মহাকাশের বুকে ফিউচারিস্টিক স্টেশন 🚀"
        )
    } else {
        listOf(
            "Neon cyberpunk metropolis at night 🏙️",
            "Futuristic female AI with glowing circuits 🤖",
            "Holographic lotus with neon water reflections 🪷",
            "Cyber space station in distant cosmos 🚀"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("image_gen_screen_content")
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Brush,
                contentDescription = null,
                tint = NeonPink,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (language == AppLanguage.BENGALI) "এআই ইমেজ স্টুডিও" else "AI Image Generation Studio",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (language == AppLanguage.BENGALI) "আপনার কল্পনাকে নিয়ন আর্টে রূপান্তর করুন" else "Transform ideas into futuristic cyberpunk artworks",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
        }

        // Prompt Input
        OutlinedTextField(
            value = promptInput,
            onValueChange = onPromptChanged,
            placeholder = {
                Text(
                    text = if (language == AppLanguage.BENGALI) "আপনার কল্পনার ছবির বিস্তারিত বর্ণনা লিখুন..." else "Describe your image concept in detail...",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("image_prompt_input"),
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonPink,
                unfocusedBorderColor = SurfaceVariantDark,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                cursorColor = NeonPink,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            minLines = 3,
            maxLines = 5
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Prompt Suggestions Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            promptSuggestions.forEach { suggestion ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .background(SurfaceDark)
                        .border(1.dp, NeonPurple.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .clickable { onPromptChanged(suggestion) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = suggestion,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Art Style Selector
        Text(
            text = if (language == AppLanguage.BENGALI) "আর্ট স্টাইল নির্বাচন করুন" else "Select Art Style",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            styleOptions.forEach { style ->
                val isSelected = selectedStyle == style
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) NeonPink.copy(alpha = 0.2f) else SurfaceDark)
                        .border(
                            1.2.dp,
                            if (isSelected) NeonPink else SurfaceVariantDark,
                            RoundedCornerShape(16.dp)
                        )
                        .clickable { onStyleSelected(style) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = style,
                        color = if (isSelected) NeonPink else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Aspect Ratio Selector
        Text(
            text = if (language == AppLanguage.BENGALI) "অনুপাত (Aspect Ratio)" else "Aspect Ratio",
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            aspectRatios.forEach { ratio ->
                val isSelected = selectedAspectRatio == ratio
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) NeonCyan.copy(alpha = 0.2f) else SurfaceDark)
                        .border(
                            1.2.dp,
                            if (isSelected) NeonCyan else SurfaceVariantDark,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { onAspectRatioSelected(ratio) }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ratio,
                        color = if (isSelected) NeonCyan else TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Generate Action Button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(25.dp))
                .background(
                    if (!isGenerating && promptInput.isNotBlank()) {
                        Brush.linearGradient(listOf(NeonPurple, NeonPink))
                    } else {
                        Brush.linearGradient(listOf(SurfaceDark, SurfaceVariantDark))
                    }
                )
                .border(
                    1.dp,
                    if (promptInput.isNotBlank()) NeonPink else SurfaceVariantDark,
                    RoundedCornerShape(25.dp)
                )
                .clickable(
                    enabled = !isGenerating && promptInput.isNotBlank(),
                    onClick = onGenerate
                )
                .testTag("generate_image_button"),
            contentAlignment = Alignment.Center
        ) {
            if (isGenerating) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = NeonPink,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (language == AppLanguage.BENGALI) "ছবি তৈরি হচ্ছে..." else "Rendering Cyber Artwork...",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.BENGALI) "ছবি তৈরি করুন" else "Generate Artwork",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Gallery Showcase
        Text(
            text = if (language == AppLanguage.BENGALI) "তৈরিকৃত গ্যালারি" else "Generated Gallery",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 10.dp)
        )

        gallery.forEach { item ->
            NeonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (item.drawableResId != null) {
                        Image(
                            painter = painterResource(id = item.drawableResId),
                            contentDescription = item.prompt,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(170.dp),
                            contentScale = ContentScale.Crop
                        )
                    }

                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonPurple.copy(alpha = 0.25f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = item.style,
                                    color = NeonCyan,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = item.prompt,
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}
