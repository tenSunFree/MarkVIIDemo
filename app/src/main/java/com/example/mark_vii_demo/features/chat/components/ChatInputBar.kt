package com.example.mark_vii_demo.features.chat.components

import androidx.activity.result.ActivityResultLauncher
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mark_vii_demo.features.chat.ChatState
import com.example.mark_vii_demo.features.chat.ChatUiEvent
import com.example.mark_vii_demo.ui.theme.AppColors
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import com.example.mark_vii_demo.R
import java.util.Locale

/**
 * Chat input bar component.
 *
 * @param modifier Optional [Modifier], default is [Modifier].
 * @param chatState Current chat state, containing the input text and response generation status.
 * @param bitmap Selected image (if any); a preview will be shown and can be sent together.
 * @param onEvent Callback to propagate UI events to the upper layer (ChatUiEvent).
 * @param onClearImage Callback to clear the selected image.
 * @param voiceInputLauncher [ActivityResultLauncher]<Intent> used to start voice recognition.
 * @param appColors Custom color palette.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChatInputBar(
    modifier: Modifier = Modifier,
    chatState: ChatState,
    bitmap: android.graphics.Bitmap?,
    onEvent: (ChatUiEvent) -> Unit,
    onClearImage: () -> Unit,
    voiceInputLauncher: ActivityResultLauncher<Intent>,
    appColors: AppColors // Custom color palette
) {
    val context = LocalContext.current
    val canSendOrStop =
        chatState.isGeneratingResponse || chatState.prompt.isNotEmpty() || bitmap != null
    val showMic = !canSendOrStop
    Column(
        modifier = modifier
            .fillMaxWidth()
            .imePadding()
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        // Image preview
        bitmap?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(appColors.surfaceVariant)
                    .border(1.dp, appColors.divider, RoundedCornerShape(16.dp))
                    .padding(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pin),
                        contentDescription = "Pinned image",
                        tint = appColors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Image(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentDescription = "picked image",
                        contentScale = ContentScale.Crop,
                        bitmap = it.asImageBitmap()
                    )
                    Text(
                        text = "Image attached",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Remove image",
                        tint = appColors.textSecondary,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable { onClearImage() }
                    )
                }
            }
        }
        // Input control row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Plus button
            IconButton(
                onClick = { /* TODO */ },
                modifier = Modifier.size(50.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.chat_add),
                    contentDescription = "Add",
                    tint = Color.Unspecified,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            // Text input container
            Box(
                modifier = Modifier
                    .weight(1f) // Let the input field take up the remaining space
                    .shadow(0.dp, RoundedCornerShape(24.dp))
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF212121))
                    .border(1.5.dp, appColors.divider.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextField(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 40.dp, max = 150.dp),
                        value = chatState.prompt,
                        onValueChange = { onEvent(ChatUiEvent.UpdatePrompt(it)) },
                        placeholder = {
                            Text("Ask ChatGPT", fontSize = 14.sp, color = appColors.textSecondary)
                        },
                        textStyle = TextStyle(
                            fontSize = 15.sp,
                            color = Color.White,
                            lineHeight = 20.sp
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            cursorColor = Color.White
                        )
                    )
                    // Voice input button logic
                    if (showMic) {
                        IconButton(
                            onClick = {
                                try {
                                    val intent =
                                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                            )
                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE,
                                                Locale.getDefault().toLanguageTag()
                                            )
                                            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
                                        }
                                    voiceInputLauncher.launch(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(
                                        context,
                                        "Voice input not available",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.chat_microphone),
                                contentDescription = "Voice input",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    // Send/Stop button
                    SendActionButton(
                        isGenerating = chatState.isGeneratingResponse,
                        isEnabled = chatState.isGeneratingResponse || chatState.prompt.isNotEmpty() || bitmap != null,
                        appColors = appColors,
                        onSend = { onEvent(ChatUiEvent.SendPrompt(chatState.prompt, bitmap)) },
                        onStop = { onEvent(ChatUiEvent.StopStreaming) }
                    )
                }
            }
        }
    }
}

/**
 * SendActionButton composable.
 *
 * Shows a circular send/stop button with animated scale and icon.
 *
 * @param isGenerating True when a response is being generated; shows stop icon and error color.
 * @param isEnabled Controls whether the button is clickable and its scale animation.
 * @param appColors Custom color palette used for button backgrounds and states.
 * @param onSend Callback invoked when the send action is triggered.
 * @param onStop Callback invoked to stop streaming response generation.
 */
@Composable
private fun SendActionButton(
    isGenerating: Boolean,
    isEnabled: Boolean,
    appColors: AppColors,
    onSend: () -> Unit,
    onStop: () -> Unit
) {
    val buttonScale by animateFloatAsState(
        targetValue = if (isEnabled) 1f else 0.85f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "button_scale"
    )
    Box(
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer {
                scaleX = buttonScale
                scaleY = buttonScale
            }
            .clip(CircleShape)
            .background(
                when {
                    isGenerating || isEnabled -> Color.White
                    else -> appColors.surfaceTertiary
                }
            )
            .clickable(enabled = isEnabled) {
                if (isGenerating) onStop() else onSend()
            },
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(targetState = isGenerating, label = "icon_animation") { generating ->
            Icon(
                painter = painterResource(id = if (generating) R.drawable.chat_stop else R.drawable.chat_up_arrow),
                contentDescription = null,
                tint = if (generating) Color.Black else Color.Unspecified,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}