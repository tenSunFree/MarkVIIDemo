package com.example.mark_vii_demo.features.chat

import android.R.attr.languageTag
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.with
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mark_vii_demo.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.mark_vii_demo.core.data.ChatData
import com.example.mark_vii_demo.core.data.FirebaseConfigManager
import com.example.mark_vii_demo.core.data.GeminiClient
import com.example.mark_vii_demo.core.data.ModelInfo
import com.example.mark_vii_demo.features.chat.components.ChatQuickActionsPanel
import com.example.mark_vii_demo.features.chat.components.ModelChatItem
import com.example.mark_vii_demo.features.chat.components.ModelMenuContent
import com.example.mark_vii_demo.features.chat.components.PromptSuggestionBubbles
import com.example.mark_vii_demo.features.chat.components.UserChatItem
import com.example.mark_vii_demo.features.chat.components.getSelectedBitmap
import com.example.mark_vii_demo.ui.theme.LocalAppColors
import com.example.mark_vii_demo.utils.getPreferredLocale
import com.example.mark_vii_demo.utils.toRecognizerTag
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChatScreen(
    paddingValues: PaddingValues,
    chatViewModel: ChatViewModel,
    uriState: MutableStateFlow<String>,
    voiceInputState: MutableStateFlow<String>,
    imagePicker: ActivityResultLauncher<PickVisualMediaRequest>,
    voiceInputLauncher: ActivityResultLauncher<Intent>,
    isTtsInitialized: Boolean,
    textToSpeech: TextToSpeech?,
    onSpeakText: (String) -> Unit,
) {
    // Log.d("more", "ChatScreen")
    val chatState = chatViewModel.chatState.collectAsState().value

    val bitmap = getSelectedBitmap(uriState)
    val voiceInput = voiceInputState.collectAsState().value

    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current

    // Track haptic feedback based on chunk arrivals
    val lastHapticTrigger = remember { mutableStateOf(0L) }
    val hapticTrigger = chatState.hapticTrigger

    // Trigger haptic when new chunk arrives
    LaunchedEffect(hapticTrigger) {
        if (hapticTrigger > 0L && hapticTrigger != lastHapticTrigger.value) {
            hapticFeedback.performHapticFeedback(
                HapticFeedbackType.TextHandleMove
            )
            lastHapticTrigger.value = hapticTrigger
        }
    }

    // State for loading free models from OpenRouter and Gemini
    var isLoadingModels by remember { mutableStateOf(ChatData.cachedFreeModels.isEmpty()) }
    var freeModels by remember { mutableStateOf<List<ModelInfo>>(ChatData.cachedFreeModels) }
    var modelsLoadError by remember { mutableStateOf<String?>(null) }

    val appColors = LocalAppColors.current

    // Observe Firebase API keys and Gemini models
    val firebaseApiKey by FirebaseConfigManager.apiKey.collectAsState()
    val geminiApiKey by FirebaseConfigManager.geminiApiKey.collectAsState()
    val firebaseGeminiModels by FirebaseConfigManager.geminiModels.collectAsState()

    // Convert Firebase Gemini models to ModelInfo
    val geminiModels = remember(firebaseGeminiModels) {
        firebaseGeminiModels.map { firebaseModel ->
            ModelInfo(
                displayName = firebaseModel.displayName,
                apiModel = firebaseModel.apiModel,
                isAvailable = firebaseModel.isAvailable
            )
        }
    }

    // Initialize Firebase to get API keys and exception models
    LaunchedEffect(Unit) {
        FirebaseConfigManager.initialize()
    }

    // Update API keys when Firebase data changes
    LaunchedEffect(firebaseApiKey) {
        if (firebaseApiKey.isNotEmpty()) {
            ChatData.updateApiKey(firebaseApiKey)
        }
    }
    LaunchedEffect(geminiApiKey) {
        if (geminiApiKey.isNotEmpty()) {
            GeminiClient.updateApiKey(geminiApiKey)
        }
    }

    // Load free models from OpenRouter AFTER Firebase initializes
    val exceptionModels by FirebaseConfigManager.exceptionModels.collectAsState()

    // Log.d("more", "ChatScreen, firebaseApiKey: $firebaseApiKey")
    LaunchedEffect(firebaseApiKey, exceptionModels) {
        if (firebaseApiKey.isNotEmpty()) {
            val modelsCacheKey = "$firebaseApiKey|${exceptionModels.hashCode()}"
            if (ChatData.cachedFreeModels.isNotEmpty() && ChatData.cachedFreeModelsKey == modelsCacheKey) {
                freeModels = ChatData.cachedFreeModels
                isLoadingModels = false
            } else {
                isLoadingModels = true
                try {
                    freeModels = ChatData.getOrFetchFreeModels(modelsCacheKey)
                    if (freeModels.isEmpty()) {
                        modelsLoadError = "No free models available"
                    }
                } catch (e: Exception) {
                    modelsLoadError = "Failed to load models: ${e.message}"
                } finally {
                    isLoadingModels = false
                }
            }
        }
    }

    // Show loading popup while models are loading
    if (isLoadingModels) {
        AlertDialog(
            onDismissRequest = { },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = null,
            text = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(60.dp),
                        color = appColors.accent,
                        strokeWidth = 4.dp
                    )
                    Text(
                        text = "Loading free models...",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontFamily = FontFamily(Font(R.font.typographica))
                    )
                }
            },
            confirmButton = { }
        )
    }

    // Model selector state for prompt box
    val isPromptDropDownExpanded: MutableState<Boolean> = remember { mutableStateOf(false) }
    val promptItemPosition: MutableState<Int> = remember { mutableStateOf(0) }

    // LazyList state for auto-scrolling
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Get current API provider from state
    val currentApiProvider = chatState.currentApiProvider

    // Switch models based on API provider
    // val currentModels = when (currentApiProvider) {
    //     ApiProvider.OPENROUTER -> freeModels
    //     ApiProvider.GEMINI -> geminiModels
    // }

    // Reset model selection when API provider changes
    LaunchedEffect(currentApiProvider) {
        promptItemPosition.value = 0
        if (freeModels.isNotEmpty()) {
            ChatData.selected_model = freeModels[0].apiModel
        }
    }

    // Set initial model when models load (only once)
    var hasSetInitialModel by remember { mutableStateOf(false) }
    LaunchedEffect(freeModels) {
        if (!hasSetInitialModel &&
            freeModels.isNotEmpty() &&
            promptItemPosition.value < freeModels.size
        ) {
            ChatData.selected_model = freeModels[promptItemPosition.value].apiModel
            hasSetInitialModel = true
        }
    }

    // Update prompt when voice input is received
    LaunchedEffect(voiceInput) {
        if (voiceInput.isNotEmpty()) {
            chatViewModel.onEvent(ChatUiEvent.UpdatePrompt(voiceInput))
            voiceInputState.update { "" }
        }
    }

    // Auto-scroll to bottom when new message appears or when generating
    LaunchedEffect(chatState.chatList.size, chatState.isGeneratingResponse) {
        if (chatState.chatList.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        // Chat messages list - extends to bottom of screen
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Show centered prompt suggestions when chat is empty
            if (chatState.showPromptSuggestions && chatState.chatList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 75.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ChatQuickActionsPanel(
                        onActionClick = { text ->
                            chatViewModel.onEvent(ChatUiEvent.UpdatePrompt(text))
                        }
                    )
                }
            }

            // Chat list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                state = listState,
                reverseLayout = true,
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
                            // Get the nearest previous user message for retry (skip error/model entries)
                            val previousUserChat = chatState.chatList
                                .drop(index + 1)
                                .firstOrNull { it.isFromUser }

                            ModelChatItem(
                                response = chat.prompt,
                                userPrompt = previousUserChat?.prompt ?: "",
                                modelUsed = chat.modelUsed,
                                isStreaming = chat.isStreaming,
                                isError = chat.isError,
                                freeModels = freeModels,
                                geminiModels = geminiModels,
                                currentApiProvider = currentApiProvider,
                                hasImage = previousUserChat?.bitmap != null,
                                onRetry = { _ ->
                                    chatViewModel.onEvent(
                                        ChatUiEvent.RetryPrompt(
                                            previousUserChat?.prompt ?: "",
                                            previousUserChat?.bitmap
                                        )
                                    )
                                },
                                onApiSwitch = { provider ->
                                    chatViewModel.onEvent(ChatUiEvent.SwitchApiProvider(provider))
                                },
                                isTtsReady = isTtsInitialized && textToSpeech != null,
                                isTtsSpeaking = textToSpeech?.isSpeaking == true,
                                onToggleTts = { cleanText ->
                                    if (textToSpeech?.isSpeaking == true) {
                                        textToSpeech.stop()
                                        Toast.makeText(
                                            context,
                                            "Speech stopped",
                                            Toast.LENGTH_SHORT
                                        ).show()
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

            // Fade effect at bottom
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

        // Prompt box - overlays at bottom
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .imePadding()
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // ================ Picked Image Display with Pin Icon ================
            bitmap?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(appColors.surfaceVariant)
                        .border(
                            width = 1.dp,
                            color = appColors.divider,
                            shape = RoundedCornerShape(16.dp)
                        )
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
                                .clickable { uriState.update { "" } }
                        )
                    }
                }
            }

            // ================ Enhanced Visible Input Box ================
            // Bottom input bar component
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(24.dp),
                            ambientColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            spotColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                        )
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Color(0xFF212121)
                            // if (appColors.textPrimary.luminance() > 0.5f) {
                            //     appColors.surfaceTertiary
                            // } else {
                            //     MaterialTheme.colorScheme.surface
                            // }
                        )
                        .border(
                            width = 1.5.dp,
                            color = appColors.divider.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(24.dp)
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Text input field at top (multiline, expands upward)
                        TextField(
                            modifier = Modifier
                                .weight(1f)
                                .padding(0.dp)
                                .heightIn(min = 40.dp, max = 150.dp),
                            value = chatState.prompt,
                            singleLine = false,
                            maxLines = 6,
                            onValueChange = { chatViewModel.onEvent(ChatUiEvent.UpdatePrompt(it)) },
                            placeholder = {
                                Text(
                                    text = "Ask ChatGPT",
                                    fontSize = 14.sp,
                                    color = appColors.textSecondary
                                )
                            },
                            textStyle = TextStyle(
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                lineHeight = 20.sp
                            ),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                                focusedContainerColor = Color(0xFF212121),
                                unfocusedContainerColor = Color(0xFF212121),
                                disabledContainerColor = Color(0xFF212121),
                                // focusedContainerColor = Color.Transparent,
                                // unfocusedContainerColor = Color.Transparent,
                                // disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                cursorColor = MaterialTheme.colorScheme.onSurface,
                                focusedPlaceholderColor = appColors.textSecondary,
                                unfocusedPlaceholderColor = appColors.textSecondary
                            )
                        )

                        // Microphone icon
                        IconButton(
                            onClick = {
                                try {
                                    val locale = getPreferredLocale()
                                    val languageTag = locale.toRecognizerTag()
                                    val intent =
                                        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                            )
                                            // Keep speech recognition aligned with the device/app language
                                            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
                                            // These two extras may make some devices/engines more responsive, optional to keep
                                            putExtra(
                                                RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE,
                                                languageTag
                                            )
                                            putExtra(
                                                RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE,
                                                languageTag
                                            )
                                            // putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")

                                            putExtra(
                                                RecognizerIntent.EXTRA_PROMPT,
                                                "Speak now..."
                                            )
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
                                // imageVector = Icons.Rounded.Mic,
                                painter = painterResource(id = R.drawable.chat_microphone),
                                contentDescription = "Voice input",
                                // tint = MaterialTheme.colorScheme.onSurface,
                                tint = Color.Unspecified,   // Disable default tint for Icon
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Send/Stop button
                        val interactionSource = remember { MutableInteractionSource() }
                        val isButtonEnabled =
                            chatState.isGeneratingResponse || chatState.prompt.isNotEmpty() || bitmap != null
                        val buttonScale by animateFloatAsState(
                            targetValue = if (isButtonEnabled) 1f else 0.85f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
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
                                    if (chatState.isGeneratingResponse) {
                                        appColors.error
                                    } else if (chatState.prompt.isNotEmpty() || bitmap != null) {
                                        appColors.accent
                                    } else {
                                        appColors.surfaceTertiary
                                    }
                                )
                                .clickable(
                                    enabled = isButtonEnabled,
                                    interactionSource = interactionSource,
                                    indication = null
                                ) {
                                    if (chatState.isGeneratingResponse) {
                                        chatViewModel.onEvent(ChatUiEvent.StopStreaming)
                                    } else {
                                        chatViewModel.onEvent(
                                            ChatUiEvent.SendPrompt(chatState.prompt, bitmap)
                                        )
                                        uriState.update { "" }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedContent(
                                targetState = chatState.isGeneratingResponse,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(200)) with fadeOut(
                                        animationSpec = tween(200)
                                    )
                                },
                                label = "icon_animation"
                            ) { isGenerating ->
                                val buttonColor = if (isGenerating) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else if (chatState.prompt.isNotEmpty() || bitmap != null) {
                                    Color.White
                                } else {
                                    Color(0xFF383838)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(buttonColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.chat_up_arrow),
                                        contentDescription = if (isGenerating) "Stop" else "Send",
                                        tint = Color.Unspecified,   // Disable default tint for Icon
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                // Icon(
                                //     // imageVector = if (isGenerating) {
                                //     //     Icons.Rounded.Stop
                                //     // } else {
                                //     //     Icons.Rounded.ArrowUpward
                                //     // },
                                //     painter = painterResource(id = R.drawable.chat_up_arrow),
                                //     contentDescription = if (isGenerating) "Stop" else "Send",
                                //     tint = if (isGenerating) {
                                //         MaterialTheme.colorScheme.onPrimary
                                //     } else if (chatState.prompt.isNotEmpty() || bitmap != null) {
                                //         // Color(0xFF1C1C1E)
                                //         // Color(0xFF383838)
                                //         Color.Blue
                                //     } else {
                                //         // appColors.textSecondary
                                //         // Color(0xFF383838)
                                //         Color.Red
                                //     },
                                //     modifier = Modifier.size(18.dp)
                                // )
                            }
                        }

                        /*
                        // Bottom row: Icons + Model Selector + Mic + Send button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Model selector
                            /*
                            Box(
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = ripple(
                                                color = MaterialTheme.colorScheme.onSurface.copy(
                                                    alpha = 0.3f
                                                )
                                            )
                                        ) { isPromptDropDownExpanded.value = true }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = if (freeModels.isNotEmpty() &&
                                            promptItemPosition.value < freeModels.size
                                        ) {
                                            freeModels[promptItemPosition.value].displayName
                                        } else {
                                            "Model Selector"
                                        },
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyMedium,
                                        lineHeight = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )

                                    Icon(
                                        painter = painterResource(id = R.drawable.drop_down_ic),
                                        contentDescription = "Select model",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }

                                // Dropdown animation
                                val dropdownScale by animateFloatAsState(
                                    targetValue = if (isPromptDropDownExpanded.value) 1f else 0.95f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    ),
                                    label = "dropdown_scale"
                                )

                                Box(
                                    modifier = Modifier
                                        .width(280.dp)
                                        .graphicsLayer {
                                            scaleX = dropdownScale
                                            scaleY = dropdownScale
                                            alpha = if (isPromptDropDownExpanded.value) 1f else 0f
                                        }
                                        .shadow(
                                            elevation = 12.dp,
                                            shape = RoundedCornerShape(20.dp),
                                            ambientColor = Color.Black.copy(alpha = 0.5f),
                                            spotColor = Color.Black.copy(alpha = 0.5f)
                                        )
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(MaterialTheme.colorScheme.surface)
                                        .border(
                                            width = 1.dp,
                                            color = appColors.divider,
                                            shape = RoundedCornerShape(20.dp)
                                        )
                                ) {
                                    DropdownMenu(
                                        expanded = isPromptDropDownExpanded.value,
                                        onDismissRequest = {
                                            isPromptDropDownExpanded.value = false
                                        },
                                        modifier = Modifier
                                            .width(280.dp)
                                            .heightIn(max = 450.dp)
                                            .background(MaterialTheme.colorScheme.surface),
                                        shape = RoundedCornerShape(20.dp),
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ) {
                                        // API Provider Switch
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Hide Gemini models list
                                                // Box(
                                                //     modifier = Modifier
                                                //         .weight(1f)
                                                //         .clip(RoundedCornerShape(10.dp))
                                                //         .background(
                                                //             if (currentApiProvider == ApiProvider.GEMINI)
                                                //                 appColors.accent.copy(alpha = 0.2f)
                                                //             else appColors.surfaceTertiary
                                                //         )
                                                //         .clickable {
                                                //             chatViewModel.onEvent(
                                                //                 ChatUiEvent.SwitchApiProvider(
                                                //                     ApiProvider.GEMINI
                                                //                 )
                                                //             )
                                                //         }
                                                //         .padding(vertical = 8.dp),
                                                //     contentAlignment = Alignment.Center
                                                // ) {
                                                //     Text(
                                                //         text = "Gemini",
                                                //         color = if (currentApiProvider == ApiProvider.GEMINI)
                                                //             appColors.accent
                                                //         else appColors.textSecondary,
                                                //         fontSize = 13.sp,
                                                //         fontWeight = if (currentApiProvider == ApiProvider.GEMINI)
                                                //             FontWeight.SemiBold
                                                //         else FontWeight.Normal
                                                //     )
                                                // }

                                                Spacer(modifier = Modifier.width(8.dp))

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(
                                                            if (currentApiProvider == ApiProvider.OPENROUTER)
                                                                appColors.accent.copy(alpha = 0.2f)
                                                            else appColors.surfaceTertiary
                                                        )
                                                        .clickable {
                                                            chatViewModel.onEvent(
                                                                ChatUiEvent.SwitchApiProvider(
                                                                    ApiProvider.OPENROUTER
                                                                )
                                                            )
                                                        }
                                                        .padding(vertical = 8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "OpenRouter",
                                                        color = if (currentApiProvider == ApiProvider.OPENROUTER)
                                                            appColors.accent
                                                        else appColors.textSecondary,
                                                        fontSize = 13.sp,
                                                        fontWeight = if (currentApiProvider == ApiProvider.OPENROUTER)
                                                            FontWeight.SemiBold
                                                        else FontWeight.Normal
                                                    )
                                                }
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp)
                                                    .height(1.dp)
                                                    .background(appColors.divider)
                                            )
                                        }

                                        // List of models
                                        ModelMenuContent(
                                            currentModels = freeModels,
                                            currentApiProvider = currentApiProvider,
                                            promptItemPosition = promptItemPosition.value,
                                            appColors = appColors,
                                            onReloadModels = if (currentApiProvider == ApiProvider.OPENROUTER) {
                                                {
                                                    // The corresponding false in finally
                                                    isLoadingModels = true
                                                    modelsLoadError = null
                                                    ChatData.cachedFreeModels = emptyList()
                                                    ChatData.cachedFreeModelsKey = ""
                                                    coroutineScope.launch {
                                                        try {
                                                            val modelsCacheKey =
                                                                "$firebaseApiKey|${exceptionModels.hashCode()}"
                                                            freeModels =
                                                                ChatData.getOrFetchFreeModels(
                                                                    modelsCacheKey
                                                                )
                                                            modelsLoadError =
                                                                if (freeModels.isEmpty()) "No free models available" else null
                                                        } catch (e: Exception) {
                                                            modelsLoadError =
                                                                "Failed to load models: ${e.message}"
                                                            Toast.makeText(
                                                                context,
                                                                modelsLoadError,
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        } finally {
                                                            // Avoid loading freezes
                                                            isLoadingModels = false
                                                        }
                                                    }
                                                }
                                            } else null,
                                            onSelectModel = { index, model ->
                                                isPromptDropDownExpanded.value = false
                                                promptItemPosition.value = index
                                                ChatData.selected_model = model.apiModel
                                                if (!model.isAvailable) {
                                                    Toast.makeText(
                                                        context,
                                                        "Model temporarily unavailable",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            */

                            // Image picker icon - only visible when Gemini is selected
                            /*
                            if (currentApiProvider == ApiProvider.GEMINI) {
                                IconButton(
                                    onClick = {
                                        imagePicker.launch(
                                            PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly
                                            )
                                        )
                                    },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = "Add image",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            */
                        }
                        */
                    }
                }
            }
        }
    }
}
