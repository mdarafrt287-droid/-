package com.example.ui.screens

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.AppLanguage
import com.example.data.model.VoiceState
import com.example.data.repository.GyroscopeOffset
import com.example.service.ArifaLiveWallpaperService
import com.example.ui.components.CyberAvatarView
import com.example.ui.components.GlowingMicButton
import com.example.ui.components.NeonCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    language: AppLanguage,
    isOnline: Boolean,
    gyroOffset: GyroscopeOffset,
    gyroEnabled: Boolean,
    onNavigate: (String) -> Unit,
    onQuickPrompt: (String) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val welcomeTitle = if (language == AppLanguage.BENGALI) {
        "স্বাগতম! আমি আরিফা"
    } else {
        "Welcome! I am ARIFA"
    }

    val welcomeSubtitle = if (language == AppLanguage.BENGALI) {
        "আপনার ব্যক্তিগত ফিউচারিস্টিক এআই সহকারী। আজ কীভাবে আপনাকে সাহায্য করতে পারি?"
    } else {
        "Your futuristic AI assistant. How may I empower and assist you today?"
    }

    val suggestedPrompts = if (language == AppLanguage.BENGALI) {
        listOf(
            "একটি সুন্দর বাংলা কবিতা বলো ✨",
            "আজকের গুরুত্বপূর্ণ সংবাদ কী?",
            "ভবিষ্যৎ ঢাকার সাইবারপাঙ্ক ছবি বানাও 🏙️",
            "ইংরেজি থেকে বাংলা অনুবাদ সাহায্য",
            "আমার সাথে মজার একটি গল্প করো"
        )
    } else {
        listOf(
            "Write a futuristic neon poem ✨",
            "Explain quantum computing simply",
            "Generate cyberpunk futuristic city 🏙️",
            "Help me write an email draft",
            "Tell me an inspiring sci-fi story"
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("home_screen_content"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Hero Avatar & Gyroscope 3D Visual Section
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            CyberAvatarView(
                sizeDp = 145.dp,
                voiceState = VoiceState.IDLE,
                isOnline = isOnline,
                gyroOffset = gyroOffset,
                gyroEnabled = gyroEnabled
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Bengali & English Welcome Card
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            borderBrush = Brush.linearGradient(listOf(NeonPurple.copy(alpha = 0.5f), NeonCyan.copy(alpha = 0.5f)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = welcomeTitle,
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = welcomeSubtitle,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Central Animated Glowing Microphone Button
        GlowingMicButton(
            voiceState = VoiceState.IDLE,
            amplitude = 0f,
            buttonSize = 78.dp,
            onClick = {
                onNavigate("voice")
            }
        )

        Text(
            text = if (language == AppLanguage.BENGALI) "কথা বলতে মাইক চাপুন" else "Tap mic to speak with ARIFA",
            color = NeonCyan,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 4 Quick Feature Shortcut Cards
        Text(
            text = if (language == AppLanguage.BENGALI) "প্রধান ফিচারসমূহ" else "Core Features",
            color = TextPrimary,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 10.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                title = if (language == AppLanguage.BENGALI) "এআই চ্যাট" else "AI Chat",
                subtitle = if (language == AppLanguage.BENGALI) "বাংলা ও ইংরেজি" else "Bilingual Chat",
                icon = Icons.Default.Chat,
                accentColor = NeonPurple,
                onClick = { onNavigate("chat") }
            )

            FeatureCard(
                modifier = Modifier.weight(1f),
                title = if (language == AppLanguage.BENGALI) "ভয়েস মোড" else "Voice Talk",
                subtitle = if (language == AppLanguage.BENGALI) "নারী কণ্ঠে উত্তর" else "Female Voice TTS",
                icon = Icons.Default.Mic,
                accentColor = NeonCyan,
                onClick = { onNavigate("voice") }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FeatureCard(
                modifier = Modifier.weight(1f),
                title = if (language == AppLanguage.BENGALI) "ছবি তৈরি" else "Image Studio",
                subtitle = if (language == AppLanguage.BENGALI) "সাইবারপাঙ্ক আর্ট" else "Cyberpunk Art",
                icon = Icons.Default.Image,
                accentColor = NeonPink,
                onClick = { onNavigate("image_gen") }
            )

            FeatureCard(
                modifier = Modifier.weight(1f),
                title = if (language == AppLanguage.BENGALI) "রাইটার" else "AI Writer",
                subtitle = if (language == AppLanguage.BENGALI) "কবিতা ও নিবন্ধ" else "Poems & Essays",
                icon = Icons.Default.Create,
                accentColor = Color(0xFF8B5CF6),
                onClick = {
                    val prompt = if (language == AppLanguage.BENGALI) "একটি সুন্দর বাংলা কবিতা রচনা করো" else "Write a creative essay on artificial intelligence"
                    onQuickPrompt(prompt)
                    onNavigate("chat")
                }
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Native Live Wallpaper Action Banner
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = SurfaceVariantDark,
            onClick = {
                try {
                    val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                        putExtra(
                            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                            ComponentName(context, ArifaLiveWallpaperService::class.java)
                        )
                    }
                    context.startActivity(intent)
                } catch (_: Exception) {
                    Toast.makeText(context, "Open Wallpaper settings to set Arifa Live Wallpaper", Toast.LENGTH_SHORT).show()
                }
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(NeonPurple.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Wallpaper,
                        contentDescription = "Live Wallpaper",
                        tint = NeonPurple,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (language == AppLanguage.BENGALI) "সাইবার লাইভ ওয়ালপেপার" else "Cyber Neon Live Wallpaper",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language == AppLanguage.BENGALI) "আপনার ফোনের হোমস্ক্রিনে সেট করুন" else "Apply animated neon grid to home screen",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                Text(
                    text = if (language == AppLanguage.BENGALI) "সেট করুন" else "Apply",
                    color = NeonCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Suggested Prompts Chips Row
        Text(
            text = if (language == AppLanguage.BENGALI) "সাজেস্টেড প্রশ্নাবলী" else "Suggested Prompts",
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 4.dp, bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            suggestedPrompts.forEach { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(SurfaceDark)
                        .border(1.dp, NeonPurple.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                        .clickable {
                            onQuickPrompt(prompt)
                            onNavigate("chat")
                        }
                        .padding(horizontal = 14.dp, vertical = 9.dp)
                        .testTag("prompt_chip")
                ) {
                    Text(
                        text = prompt,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun FeatureCard(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    onClick: () -> Unit
) {
    NeonCard(
        modifier = modifier,
        borderBrush = Brush.linearGradient(
            listOf(accentColor.copy(alpha = 0.6f), Color.Transparent)
        ),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = accentColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }
    }
}
