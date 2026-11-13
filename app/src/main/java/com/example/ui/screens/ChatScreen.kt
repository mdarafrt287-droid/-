package com.example.ui.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.ChatMessageEntity
import com.example.data.model.AppLanguage
import com.example.data.model.ChatRole
import com.example.data.model.VoiceState
import com.example.ui.components.NeonCard
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPink
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidBlack
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    messages: List<ChatMessageEntity>,
    inputText: String,
    isSending: Boolean,
    language: AppLanguage,
    voiceState: VoiceState,
    partialVoiceText: String = "",
    errorMessage: String? = null,
    permissionDenied: Boolean = false,
    onInputTextChanged: (String) -> Unit,
    onSendMessage: (String?) -> Unit,
    onSpeakMessage: (String) -> Unit,
    onStopSpeaking: () -> Unit = {},
    onToggleVoice: () -> Unit,
    onClearChat: () -> Unit,
    onRetry: () -> Unit = {},
    onPermissionDenied: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    val context = LocalContext.current
    val listState = rememberLazyListState()

    // Microphone permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            onToggleVoice()
        } else {
            onPermissionDenied()
        }
    }

    LaunchedEffect(messages.size, isSending) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .testTag("chat_screen_content")
    ) {
        // Chat Header with Clear button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .border(1.dp, NeonPurple, CircleShape)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_arifa_avatar),
                        contentDescription = "ARIFA AI",
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "ARIFA AI Chat",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (language == AppLanguage.BENGALI) "বাংলা ও ইংরেজিতে উত্তর দিতে প্রস্তুত" else "Ready in Bengali & English",
                        color = NeonCyan,
                        fontSize = 11.sp
                    )
                }
            }

            if (messages.isNotEmpty()) {
                IconButton(
                    onClick = onClearChat,
                    modifier = Modifier.testTag("clear_chat_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = "Clear Chat",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Messages List or Empty Greeting State
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (messages.isEmpty()) {
                EmptyChatGreeting(
                    language = language,
                    onPromptSelected = { prompt ->
                        onSendMessage(prompt)
                    }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        ChatMessageItem(
                            message = message,
                            onSpeak = { onSpeakMessage(message.content) },
                            onCopy = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("ARIFA AI", message.content)
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(
                                    context,
                                    if (language == AppLanguage.BENGALI) "কপি করা হয়েছে" else "Copied to clipboard",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }

                    if (isSending) {
                        item {
                            TypingIndicatorItem()
                        }
                    }
                }
            }
        }

        // Voice Status Indicator Strip (Listening / Processing / Speaking)
        AnimatedVisibility(
            visible = voiceState == VoiceState.LISTENING ||
                voiceState == VoiceState.THINKING ||
                voiceState == VoiceState.SPEAKING
        ) {
            val statusColor = when (voiceState) {
                VoiceState.LISTENING -> NeonCyan
                VoiceState.THINKING -> NeonPurple
                VoiceState.SPEAKING -> NeonPink
                else -> TextSecondary
            }
            val statusText = when (voiceState) {
                VoiceState.LISTENING -> if (partialVoiceText.isNotBlank()) {
                    partialVoiceText
                } else if (language == AppLanguage.BENGALI) {
                    "শুনছি... কথা বলুন (বাংলা / EN)"
                } else {
                    "Listening... Speak now"
                }
                VoiceState.THINKING -> if (language == AppLanguage.BENGALI) {
                    "Gemini AI প্রক্রিয়াকরণ করছে..."
                } else {
                    "Processing with Gemini AI..."
                }
                VoiceState.SPEAKING -> if (language == AppLanguage.BENGALI) {
                    "আরিফা উত্তর পড়ে শোনাচ্ছে..."
                } else {
                    "ARIFA is speaking response..."
                }
                else -> ""
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(SurfaceDark)
                    .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("chat_voice_status_bar"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                }

                if (voiceState == VoiceState.SPEAKING || voiceState == VoiceState.LISTENING) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(statusColor.copy(alpha = 0.2f))
                            .clickable {
                                if (voiceState == VoiceState.SPEAKING) onStopSpeaking() else onToggleVoice()
                            }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                            .testTag("chat_stop_voice_button"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop voice",
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (language == AppLanguage.BENGALI) "থামান" else "Stop",
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Error Banner with Retry or Settings
        if (!errorMessage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF2A0D15))
                    .border(1.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .testTag("chat_error_banner")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠️ $errorMessage",
                            color = Color(0xFFFCA5A5),
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (permissionDenied) {
                            Text(
                                text = if (language == AppLanguage.BENGALI) "সেটিংস" else "Settings",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable {
                                        val intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.fromParts("package", context.packageName, null)
                                        )
                                        context.startActivity(intent)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        } else {
                            Text(
                                text = if (language == AppLanguage.BENGALI) "রিট্রাই" else "Retry",
                                color = NeonCyan,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clickable { onRetry() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Text(
                            text = "✕",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            modifier = Modifier
                                .clickable { onDismissError() }
                                .padding(start = 6.dp, end = 2.dp)
                        )
                    }
                }
            }
        }

        // Input Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = onInputTextChanged,
                placeholder = {
                    Text(
                        text = if (language == AppLanguage.BENGALI) "আরিফাকে যেকোনো কিছু জিজ্ঞাসা করুন..." else "Ask ARIFA anything...",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .testTag("chat_input_field"),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = SurfaceVariantDark,
                    focusedContainerColor = SurfaceDark,
                    unfocusedContainerColor = SurfaceDark,
                    cursorColor = NeonCyan,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Mic / Voice Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (voiceState) {
                            VoiceState.LISTENING -> NeonCyan
                            VoiceState.SPEAKING -> NeonPink
                            else -> SurfaceDark
                        }
                    )
                    .border(
                        1.dp,
                        if (voiceState == VoiceState.SPEAKING) NeonPink else NeonPurple,
                        CircleShape
                    )
                    .clickable {
                        if (voiceState == VoiceState.SPEAKING) {
                            onStopSpeaking()
                        } else {
                            val hasPermission = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPermission) {
                                onToggleVoice()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                    .testTag("chat_mic_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (voiceState == VoiceState.SPEAKING) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (voiceState == VoiceState.SPEAKING) "Stop voice playback" else "Voice input",
                    tint = if (voiceState == VoiceState.LISTENING || voiceState == VoiceState.SPEAKING) VoidBlack else NeonCyan,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            // Send Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (inputText.isNotBlank() && !isSending) {
                            Brush.linearGradient(listOf(NeonPurple, NeonPink))
                        } else {
                            Brush.linearGradient(listOf(SurfaceDark, SurfaceDark))
                        }
                    )
                    .border(
                        1.dp,
                        if (inputText.isNotBlank()) NeonPink else SurfaceVariantDark,
                        CircleShape
                    )
                    .clickable(
                        enabled = inputText.isNotBlank() && !isSending,
                        onClick = { onSendMessage(null) }
                    )
                    .testTag("chat_send_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send message",
                    tint = if (inputText.isNotBlank() && !isSending) Color.White else TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatMessageItem(
    message: ChatMessageEntity,
    onSpeak: () -> Unit,
    onCopy: () -> Unit
) {
    val isUser = message.role == ChatRole.USER.name
    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Image(
                painter = painterResource(id = R.drawable.img_arifa_avatar),
                contentDescription = "ARIFA AI",
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .border(1.dp, NeonCyan, CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 290.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(
                        if (isUser) {
                            Brush.linearGradient(listOf(NeonPurple, NeonPink))
                        } else {
                            Brush.linearGradient(listOf(SurfaceDark, SurfaceVariantDark))
                        }
                    )
                    .border(
                        width = 1.dp,
                        brush = if (isUser) {
                            Brush.linearGradient(listOf(NeonPink, NeonPurple))
                        } else {
                            Brush.linearGradient(listOf(NeonCyan.copy(alpha = 0.5f), NeonPurple.copy(alpha = 0.3f)))
                        },
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = message.content,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(3.dp))

            // Footer row with timestamp and action buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = formattedTime,
                    color = TextSecondary,
                    fontSize = 10.sp
                )

                if (!isUser) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Speak response in Bengali",
                        tint = NeonCyan,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(onClick = onSpeak)
                    )

                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy response",
                        tint = TextSecondary,
                        modifier = Modifier
                            .size(14.dp)
                            .clickable(onClick = onCopy)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyChatGreeting(
    language: AppLanguage,
    onPromptSelected: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(76.dp)
                .clip(CircleShape)
                .border(2.dp, NeonPurple, CircleShape)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_arifa_avatar),
                contentDescription = "ARIFA AI",
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = if (language == AppLanguage.BENGALI) "কথা বলুন আরিফার সাথে" else "Chat with ARIFA AI",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = if (language == AppLanguage.BENGALI)
                "যেকোনো প্রশ্ন, অনুবাদ, কবিতা লেখা বা ধারণার জন্য প্রস্তুত!"
            else
                "Ask anything, brainstorm ideas, request poems or translations!",
            color = TextSecondary,
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        val promptList = if (language == AppLanguage.BENGALI) {
            listOf(
                "তুমি কি কি কাজ করতে পারো?",
                "আমাকে একটি অনুপ্রেরণামূলক বাংলা কবিতা লিখে দাও",
                "ভবিষ্যতের প্রযুক্তির সম্ভাবনা কেমন?"
            )
        } else {
            listOf(
                "What can you do as ARIFA AI?",
                "Write an inspiring poem about neon stars",
                "Explain artificial intelligence to a beginner"
            )
        }

        promptList.forEach { prompt ->
            NeonCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                onClick = { onPromptSelected(prompt) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = prompt,
                        color = TextPrimary,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TypingIndicatorItem() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")

    val dot1Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d1"
    )

    val dot2Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 150, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d2"
    )

    val dot3Scale by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, delayMillis = 300, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "d3"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_arifa_avatar),
            contentDescription = "ARIFA AI",
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .border(1.dp, NeonPurple, CircleShape)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceDark)
                .border(1.dp, NeonPurple.copy(alpha = 0.4f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(dot1Scale)
                        .clip(CircleShape)
                        .background(NeonCyan)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(dot2Scale)
                        .clip(CircleShape)
                        .background(NeonPurple)
                )
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .scale(dot3Scale)
                        .clip(CircleShape)
                        .background(NeonPink)
                )
            }
        }
    }
}
