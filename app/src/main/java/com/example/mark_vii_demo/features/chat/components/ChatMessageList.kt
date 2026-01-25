package com.example.mark_vii_demo.features.chat.components

import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mark_vii_demo.core.data.ModelInfo
import com.example.mark_vii_demo.features.chat.ApiProvider
import com.example.mark_vii_demo.features.chat.ChatState

/**
 * Chat message list component.
 *
 * @param modifier Compose modifier, defaults to Modifier
 * @param chatState Chat state data
 * @param listState LazyColumn scroll state
 * @param freeModels List of available free models
 * @param geminiModels List of Gemini models
 * @param isTtsInitialized Whether TTS is initialized
 * @param textToSpeech TextToSpeech object (nullable)
 * @param onActionClick Callback for quick action clicks, receives action text
 * @param onRetry Retry callback, parameters: prompt and optional image
 * @param onApiSwitch Callback to switch API provider
 * @param onSpeakText Callback to request speaking plain text
 */
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ChatMessageList(
    modifier: Modifier = Modifier,
    chatState: ChatState,
    listState: LazyListState,
    freeModels: List<ModelInfo>,
    geminiModels: List<ModelInfo>,
    isTtsInitialized: Boolean,
    textToSpeech: TextToSpeech?,
    onActionClick: (String) -> Unit,
    onRetry: (String, android.graphics.Bitmap?) -> Unit,
    onApiSwitch: (ApiProvider) -> Unit,
    onSpeakText: (String) -> Unit
) {
    val context = LocalContext.current
    Box(modifier = modifier.fillMaxSize()) {
        // Display quick suggestions when the chat list is empty
        if (chatState.showPromptSuggestions && chatState.chatList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 75.dp),
                contentAlignment = Alignment.Center
            ) {
                ChatQuickActionsPanel(
                    onActionClick = onActionClick
                )
            }
        }
        // Chat message list
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            state = listState,
            contentPadding = PaddingValues(bottom = 140.dp)
        ) {
            itemsIndexed(
                items = chatState.chatList,
                key = { _, chat -> chat.id }
            ) { index, chat ->
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(300)) +
                            slideInVertically(
                                initialOffsetY = { it / 4 },
                                animationSpec = tween(
                                    durationMillis = 300,
                                    easing = FastOutSlowInEasing
                                )
                            )
                ) {
                    if (chat.isFromUser) {
                        UserChatItem(
                            prompt = chat.prompt,
                            bitmap = chat.bitmap
                        )
                    } else {
                        // Retrieve the most recent user-sent message to use for retry
                        val previousUserChat = chatState.chatList
                            .take(index)
                            .lastOrNull { it.isFromUser }
                        ModelChatItem(
                            response = chat.prompt,
                            userPrompt = previousUserChat?.prompt ?: "",
                            modelUsed = chat.modelUsed,
                            isStreaming = chat.isStreaming,
                            isError = chat.isError,
                            freeModels = freeModels,
                            geminiModels = geminiModels,
                            currentApiProvider = chatState.currentApiProvider,
                            hasImage = previousUserChat?.bitmap != null,
                            onRetry = { _ ->
                                onRetry(
                                    previousUserChat?.prompt ?: "",
                                    previousUserChat?.bitmap
                                )
                            },
                            onApiSwitch = onApiSwitch,
                            isTtsReady = isTtsInitialized && textToSpeech != null,
                            isTtsSpeaking = textToSpeech?.isSpeaking == true,
                            onToggleTts = { cleanText ->
                                if (textToSpeech?.isSpeaking == true) {
                                    textToSpeech.stop()
                                    Toast.makeText(context, "Speech stopped", Toast.LENGTH_SHORT)
                                        .show()
                                } else {
                                    onSpeakText(cleanText)
                                    Toast.makeText(context, "Speaking...", Toast.LENGTH_SHORT)
                                        .show()
                                }
                            }
                        )
                    }
                }
            }
        }
        // Bottom gradient overlay (Fade effect)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        )
    }
}