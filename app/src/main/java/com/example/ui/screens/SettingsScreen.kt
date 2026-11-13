package com.example.ui.screens

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.AppLanguage
import com.example.service.ArifaLiveWallpaperService
import com.example.ui.components.NeonCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.OnlineGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack

@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    language: AppLanguage,
    voicePitch: Float,
    voiceSpeed: Float,
    edgeLightingEnabled: Boolean,
    gyroscopeEnabled: Boolean,
    wakeWordEnabled: Boolean,
    overlayEnabled: Boolean,
    backendApiUrl: String,
    snackbarMessage: String?,
    onLanguageChange: (AppLanguage) -> Unit,
    onVoicePitchChange: (Float) -> Unit,
    onVoiceSpeedChange: (Float) -> Unit,
    onEdgeLightingChange: (Boolean) -> Unit,
    onGyroscopeChange: (Boolean) -> Unit,
    onWakeWordChange: (Boolean) -> Unit,
    onOverlayChange: (Boolean) -> Unit,
    onBackendApiUrlChange: (String) -> Unit,
    onClearChatHistory: () -> Unit,
    onTestVoice: () -> Unit,
    onClearSnackbar: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    var apiUrlInput by remember(backendApiUrl) { mutableStateOf(backendApiUrl) }

    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage != null) {
            Toast.makeText(context, snackbarMessage, Toast.LENGTH_SHORT).show()
            onClearSnackbar()
        }
    }

    // Permission Check States
    val hasMic = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val canDrawOverlays = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else true

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("settings_screen_content")
    ) {
        // Title
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Tune,
                contentDescription = null,
                tint = NeonCyan,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (language == AppLanguage.BENGALI) "অ্যাপ সেটিংস" else "Application Settings",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Language Section
        SettingsSectionHeader(
            title = if (language == AppLanguage.BENGALI) "ভাষা নির্বাচন (Language)" else "App Language",
            icon = Icons.Default.Language
        )

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                LanguageSelectionPill(
                    modifier = Modifier.weight(1f),
                    title = "বাংলা (Bengali)",
                    subtitle = "প্রধান ভাষা",
                    isSelected = language == AppLanguage.BENGALI,
                    onClick = { onLanguageChange(AppLanguage.BENGALI) }
                )

                LanguageSelectionPill(
                    modifier = Modifier.weight(1f),
                    title = "English",
                    subtitle = "Secondary Language",
                    isSelected = language == AppLanguage.ENGLISH,
                    onClick = { onLanguageChange(AppLanguage.ENGLISH) }
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Female Bengali Voice & TTS Settings
        SettingsSectionHeader(
            title = if (language == AppLanguage.BENGALI) "নারী কণ্ঠ ও স্পিচ সিন্থেসিস" else "Female Voice & Speech Engine",
            icon = Icons.Default.GraphicEq
        )

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Pitch slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (language == AppLanguage.BENGALI) "কণ্ঠের সুর (Pitch)" else "Voice Pitch",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = String.format("%.2fx", voicePitch),
                        color = NeonPink,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = voicePitch,
                    onValueChange = onVoicePitchChange,
                    valueRange = 0.6f..1.8f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonPink,
                        activeTrackColor = NeonPink,
                        inactiveTrackColor = SurfaceVariantDark
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Speed slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (language == AppLanguage.BENGALI) "কথা বলার গতি (Speed)" else "Voice Speed",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = String.format("%.2fx", voiceSpeed),
                        color = NeonCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Slider(
                    value = voiceSpeed,
                    onValueChange = onVoiceSpeedChange,
                    valueRange = 0.6f..1.6f,
                    colors = SliderDefaults.colors(
                        thumbColor = NeonCyan,
                        activeTrackColor = NeonCyan,
                        inactiveTrackColor = SurfaceVariantDark
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Test voice button
                Button(
                    onClick = onTestVoice,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceVariantDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.BENGALI) "কণ্ঠ পরীক্ষা করুন (Test Voice)" else "Test Bengali Voice",
                        color = TextPrimary,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Visual & Gyroscope Effects
        SettingsSectionHeader(
            title = if (language == AppLanguage.BENGALI) "ভিজুয়াল ও সেন্সর এফেক্টস" else "Visual & Gyroscope Effects",
            icon = Icons.Default.Animation
        )

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                SettingSwitchRow(
                    title = if (language == AppLanguage.BENGALI) "অ্যানিমেটেড নিয়ন এজ লাইটিং" else "Animated Neon Edge Lighting",
                    subtitle = if (language == AppLanguage.BENGALI) "স্ক্রিনের চারপাশে প্রবাহিত সাইবার নিয়ন আলো" else "Glowing futuristic perimeter glow",
                    checked = edgeLightingEnabled,
                    onCheckedChange = onEdgeLightingChange
                )

                Spacer(modifier = Modifier.height(14.dp))

                SettingSwitchRow(
                    title = if (language == AppLanguage.BENGALI) "জাইরোস্কোপ ৩ডি প্যারালাক্স" else "Gyroscope 3D Parallax Tilt",
                    subtitle = if (language == AppLanguage.BENGALI) "ফোন নাড়ালে অবতার ও কার্ডে ৩ডি প্রতিক্রিয়া" else "Card tilt responding to device movement",
                    checked = gyroscopeEnabled,
                    onCheckedChange = onGyroscopeChange
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Native Android Services (Foreground, Overlay, Wallpaper)
        SettingsSectionHeader(
            title = if (language == AppLanguage.BENGALI) "নেটিভ অ্যান্ড্রয়েড সার্ভিস" else "Native Android Services",
            icon = Icons.Default.Layers
        )

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Wake word
                SettingSwitchRow(
                    title = if (language == AppLanguage.BENGALI) "ভয়েস ওয়েক-ওয়ার্ড ফোরগ্রাউন্ড সার্ভিস" else "Wake-Word Foreground Service",
                    subtitle = if (language == AppLanguage.BENGALI) "নোটিফিকেশনসহ ব্যাকগ্রাউন্ডে ‘আরিফা’ ডাক শুনবে" else "Listens for 'Hey Arifa' with persistent notification",
                    checked = wakeWordEnabled,
                    onCheckedChange = onWakeWordChange
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Floating Overlay Bubble
                SettingSwitchRow(
                    title = if (language == AppLanguage.BENGALI) "ফ্লোটিং অ্যাসিস্ট্যান্ট ওভারলে" else "Floating Assistant Overlay Bubble",
                    subtitle = if (language == AppLanguage.BENGALI) "অন্যান্য অ্যাপের উপর ভাসমান ড্র্যাগেবল বাবল" else "Draggable floating avatar over other apps",
                    checked = overlayEnabled,
                    onCheckedChange = { enabled ->
                        if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else {
                            onOverlayChange(enabled)
                        }
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Live Wallpaper Launcher
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceDark)
                        .clickable {
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
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Wallpaper,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (language == AppLanguage.BENGALI) "সাইবার লাইভ ওয়ালপেপার সেট করুন" else "Apply Cyber Live Wallpaper",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (language == AppLanguage.BENGALI) "হোমস্ক্রিনে নিয়ন গ্রিড ওয়ালপেপার চালান" else "Set animated wallpaper to home screen",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    Text(
                        text = if (language == AppLanguage.BENGALI) "ওপেন" else "Open",
                        color = NeonCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Security & Secure Gemini Backend API Interface
        SettingsSectionHeader(
            title = if (language == AppLanguage.BENGALI) "সিকিউরিটি ও ব্যাকএন্ড ইন্টারফেস" else "Security & Gemini Backend API",
            icon = Icons.Default.Security
        )

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = if (language == AppLanguage.BENGALI) "নিরাপদ জেমিনাই ইঞ্জিন (Firebase AI Logic)" else "Secure Gemini Engine (Firebase AI Logic)",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = if (language == AppLanguage.BENGALI)
                        "ডিফল্টভাবে কোনো API key ছাড়াই Firebase AI Logic (App Check) সক্রিয় আছে। শুধুমাত্র কাস্টম সার্ভার ব্যবহার করতে চাইলে নিচে HTTPS URL দিন, নতুবা ফাঁকা রাখুন।"
                    else
                        "Built-in Firebase AI Logic (App Check) is active with no client API keys. Leave blank to use built-in Gemini, or enter an optional custom HTTPS backend URL.",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                )

                OutlinedTextField(
                    value = apiUrlInput,
                    onValueChange = {
                        apiUrlInput = it
                        onBackendApiUrlChange(it)
                    },
                    placeholder = {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "ঐচ্ছিক: https://... (ফাঁকা থাকলে বিল্ট-ইন Gemini)" else "Optional: https://... (blank = built-in Gemini)",
                            color = TextSecondary.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_backend_url_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = SurfaceVariantDark,
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        cursorColor = NeonCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Clear History & Privacy
        SettingsSectionHeader(
            title = if (language == AppLanguage.BENGALI) "প্রাইভেসি ও ডাটা" else "Privacy & Local Data",
            icon = Icons.Default.Delete
        )

        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Button(
                    onClick = onClearChatHistory,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7F1D1D)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (language == AppLanguage.BENGALI) "সকল চ্যাট হিস্ট্রি মুছে ফেলুন" else "Clear All Chat History",
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.BENGALI) "মাইক্রোফোন পারমিশন" else "Microphone Permission",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (hasMic) "অনুমোদিত (Granted)" else "অনুমতি দেওয়া হয়নি (Denied)",
                        color = if (hasMic) OnlineGreen else Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (language == AppLanguage.BENGALI) "ওভারলে ডিসপ্লে পারমিশন" else "Overlay Display Permission",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                    Text(
                        text = if (canDrawOverlays) "অনুমোদিত (Granted)" else "অনুমতি দেওয়া হয়নি (Denied)",
                        color = if (canDrawOverlays) OnlineGreen else Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // About Card
        NeonCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "ARIFA AI v1.0",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Native Android • Jetpack Compose • Kotlin • Room MVVM",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
                Text(
                    text = "Bengali & English Futuristic Cyber Intelligence Companion",
                    color = TextSecondary,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = NeonPurple,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LanguageSelectionPill(
    modifier: Modifier = Modifier,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) NeonPurple.copy(alpha = 0.2f) else SurfaceDark)
            .border(
                1.2.dp,
                if (isSelected) NeonPurple else SurfaceVariantDark,
                RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = if (isSelected) NeonPurple else TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = NeonPurple,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                color = TextSecondary,
                fontSize = 11.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = NeonPurple,
                uncheckedThumbColor = TextSecondary,
                uncheckedTrackColor = VoidBlack
            )
        )
    }
}
