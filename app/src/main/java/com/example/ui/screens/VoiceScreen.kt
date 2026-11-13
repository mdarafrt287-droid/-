package com.example.ui.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.core.content.ContextCompat
import com.example.data.model.AppLanguage
import com.example.data.model.VoiceState
import com.example.data.repository.VoiceProfileInfo
import com.example.ui.components.AudioWaveVisualizer
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
fun VoiceScreen(
    modifier: Modifier = Modifier,
    language: AppLanguage,
    voiceState: VoiceState,
    audioAmplitude: Float,
    recognizedText: String,
    lastAiResponse: String?,
    wakeWordEnabled: Boolean,
    errorMessage: String? = null,
    permissionDenied: Boolean = false,
    ttsStatusMessage: String? = null,
    voiceProfileInfo: VoiceProfileInfo? = null,
    onToggleListening: () -> Unit,
    onStopSpeaking: () -> Unit = {},
    onStopListening: () -> Unit = {},
    onReplay: () -> Unit,
    onRetryQuery: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
    onDismissError: () -> Unit = {},
    onToggleWakeWord: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onToggleListening()
        } else {
            onPermissionDenied()
        }
    }

    val stateBadgeText = when (voiceState) {
        VoiceState.LISTENING -> if (language == AppLanguage.BENGALI) "শুনছি... কথা বলুন" else "Listening... Speak now"
        VoiceState.THINKING -> if (language == AppLanguage.BENGALI) "প্রসেসিং হচ্ছে (Gemini AI)..." else "Processing with Gemini AI..."
        VoiceState.SPEAKING -> if (language == AppLanguage.BENGALI) "আরিফা কথা বলছে..." else "ARIFA is speaking..."
        VoiceState.ERROR -> if (language == AppLanguage.BENGALI) "ত্রুটি হয়েছে • আবার চেষ্টা করুন" else "Error • Please try again"
        VoiceState.IDLE -> if (language == AppLanguage.BENGALI) "প্রস্তুত • কথা বলতে মাইক চাপুন" else "Idle • Tap microphone to speak"
    }

    val stateBadgeColor = when (voiceState) {
        VoiceState.LISTENING -> NeonCyan
        VoiceState.THINKING -> NeonPurple
        VoiceState.SPEAKING -> NeonPink
        VoiceState.ERROR -> Color(0xFFEF4444)
        VoiceState.IDLE -> TextSecondary
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .testTag("voice_screen_content"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Badge Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.2.dp, stateBadgeColor.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
                .testTag("voice_status_badge")
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(stateBadgeColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stateBadgeText,
                    color = stateBadgeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Active Bengali Female Voice Profile Badge
        if (voiceProfileInfo != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(SurfaceVariantDark.copy(alpha = 0.7f))
                    .padding(horizontal = 10.dp, vertical = 4.dp)
                    .testTag("voice_profile_badge")
            ) {
                Icon(
                    imageVector = Icons.Default.RecordVoiceOver,
                    contentDescription = null,
                    tint = NeonPink,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                val voiceLabel = when {
                    language == AppLanguage.BENGALI && voiceProfileInfo.isFemaleSelected ->
                        "বাংলা নারী কণ্ঠ (${voiceProfileInfo.localeTag})"
                    language == AppLanguage.BENGALI && voiceProfileInfo.isBengaliSupported ->
                        "বাংলা কণ্ঠ (${voiceProfileInfo.localeTag})"
                    language == AppLanguage.BENGALI ->
                        "বিকল্প কণ্ঠ (${voiceProfileInfo.localeTag})"
                    voiceProfileInfo.isFemaleSelected ->
                        "Female Voice (${voiceProfileInfo.localeTag})"
                    else ->
                        "Voice (${voiceProfileInfo.localeTag})"
                }
                Text(
                    text = voiceLabel,
                    color = TextSecondary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        if (!ttsStatusMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = ttsStatusMessage,
                color = NeonCyan,
                fontSize = 11.sp
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Large Cyber Avatar Reacting to Voice
        CyberAvatarView(
            sizeDp = 165.dp,
            voiceState = voiceState,
            isOnline = true,
            gyroEnabled = false
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic Audio Wave Visualizer
        AudioWaveVisualizer(
            isAnimating = voiceState == VoiceState.LISTENING ||
                voiceState == VoiceState.SPEAKING ||
                voiceState == VoiceState.THINKING,
            amplitude = if (voiceState == VoiceState.THINKING) 0.35f else audioAmplitude,
            barCount = 20,
            maxHeight = 44.dp
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Error & Permission Banner
        AnimatedVisibility(
            visible = !errorMessage.isNullOrBlank(),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            if (!errorMessage.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF2A0D15))
                        .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                        .testTag("voice_error_banner")
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Text(
                                text = "⚠️ $errorMessage",
                                color = Color(0xFFFCA5A5),
                                fontSize = 12.sp,
                                lineHeight = 17.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "✕",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clickable { onDismissError() }
                                    .padding(start = 8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (permissionDenied) {
                                Button(
                                    onClick = {
                                        val intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        )
                                        context.startActivity(intent)
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.BENGALI) "সেটিংস খুলুন" else "Open Settings",
                                        color = Color.White,
                                        fontSize = 11.sp
                                    )
                                }
                            } else if (recognizedText.isNotBlank()) {
                                Button(
                                    onClick = onRetryQuery,
                                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.defaultMinSize(minHeight = 36.dp)
                                ) {
                                    Text(
                                        text = if (language == AppLanguage.BENGALI) "আবার চেষ্টা করুন" else "Retry",
                                        color = VoidBlack,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Recognized Text Display Card
        if (recognizedText.isNotBlank()) {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                borderBrush = Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.6f), NeonPurple.copy(alpha = 0.4f)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Text(
                        text = if (language == AppLanguage.BENGALI) "আপনার ভয়েস:" else "You said:",
                        color = NeonCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = recognizedText,
                        color = TextPrimary,
                        fontSize = 14.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Spoken AI Response Card
        if (!lastAiResponse.isNullOrBlank()) {
            NeonCard(
                modifier = Modifier.fillMaxWidth(),
                borderBrush = Brush.linearGradient(listOf(NeonPink.copy(alpha = 0.6f), NeonPurple.copy(alpha = 0.4f)))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "আরিফার উত্তর (Gemini):" else "ARIFA Response (Gemini):",
                            color = NeonPink,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (voiceState == VoiceState.SPEAKING) {
                                IconButton(
                                    onClick = onStopSpeaking,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("card_stop_speaking_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Stop,
                                        contentDescription = "Stop Speech Playback",
                                        tint = NeonPink,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            IconButton(
                                onClick = onReplay,
                                modifier = Modifier
                                    .size(32.dp)
                                    .testTag("replay_speech_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = "Replay Speech",
                                    tint = NeonCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = lastAiResponse,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        lineHeight = 19.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Central Animated Glowing Mic Control
        GlowingMicButton(
            voiceState = voiceState,
            amplitude = audioAmplitude,
            buttonSize = 84.dp,
            onClick = {
                if (voiceState == VoiceState.SPEAKING) {
                    onStopSpeaking()
                } else {
                    val hasPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.RECORD_AUDIO
                    ) == PackageManager.PERMISSION_GRANTED
                    if (hasPermission) {
                        onToggleListening()
                    } else {
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                }
            }
        )

        // Explicit Stop Playback / Stop Listening Action Pill
        AnimatedVisibility(
            visible = voiceState == VoiceState.SPEAKING || voiceState == VoiceState.LISTENING
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(6.dp))
                Button(
                    onClick = {
                        if (voiceState == VoiceState.SPEAKING) onStopSpeaking() else onStopListening()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (voiceState == VoiceState.SPEAKING) NeonPink.copy(alpha = 0.22f) else NeonCyan.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .border(
                            1.dp,
                            if (voiceState == VoiceState.SPEAKING) NeonPink else NeonCyan,
                            RoundedCornerShape(24.dp)
                        )
                        .testTag(if (voiceState == VoiceState.SPEAKING) "stop_speaking_button" else "stop_listening_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Stop,
                        contentDescription = if (voiceState == VoiceState.SPEAKING) "Stop speech playback" else "Stop listening",
                        tint = if (voiceState == VoiceState.SPEAKING) NeonPink else NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (voiceState == VoiceState.SPEAKING) {
                            if (language == AppLanguage.BENGALI) "কথা বলা থামান" else "Stop Speaking"
                        } else {
                            if (language == AppLanguage.BENGALI) "শোনা থামান" else "Stop Listening"
                        },
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Background Wake Word Detection Control Card
        NeonCard(
            modifier = Modifier.fillMaxWidth(),
            borderBrush = Brush.linearGradient(listOf(NeonPurple.copy(alpha = 0.4f), Color.Transparent))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(NeonPurple.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Hearing,
                            contentDescription = null,
                            tint = NeonPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = if (language == AppLanguage.BENGALI) "ওয়েক-ওয়ার্ড সার্ভিস" else "Wake-Word Service",
                            color = TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (language == AppLanguage.BENGALI) "‘আরিফা’ বা ‘Hey Arifa’ বলে ডাকুন" else "Say 'Hey Arifa' anytime",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }

                Switch(
                    checked = wakeWordEnabled,
                    onCheckedChange = { enabled ->
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECORD_AUDIO
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            onToggleWakeWord(enabled)
                        } else {
                            permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = NeonPurple,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = VoidBlack
                    ),
                    modifier = Modifier.testTag("wake_word_switch")
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
