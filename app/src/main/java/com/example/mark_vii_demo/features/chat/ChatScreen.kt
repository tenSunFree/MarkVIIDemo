package com.example.mark_vii_demo.features.chat

import android.R.attr.languageTag
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
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
import com.example.mark_vii_demo.features.chat.components.ChatInputBar
import com.example.mark_vii_demo.features.chat.components.ChatMessageList
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

    // LaunchedEffect(chatState.chatList) {
    //     val ids = chatState.chatList.take(5).joinToString { it.prompt }
    //     Log.d("chat", "top5: $ids")
    // }

    // Auto-scroll to bottom when new message appears or when generating
    LaunchedEffect(chatState.chatList.size, chatState.isGeneratingResponse) {
        // if (chatState.chatList.isNotEmpty()) {
        //     listState.animateScrollToItem(0)
        // }
        val lastIndex = chatState.chatList.lastIndex
        if (lastIndex >= 0) {
            listState.animateScrollToItem(lastIndex)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = paddingValues.calculateTopPadding())
    ) {
        // Chat messages list - extends to bottom of screen
        ChatMessageList(
            chatState = chatState,
            listState = listState,
            freeModels = freeModels,
            geminiModels = geminiModels,
            isTtsInitialized = isTtsInitialized,
            textToSpeech = textToSpeech,
            onActionClick = { text ->
                chatViewModel.onEvent(ChatUiEvent.UpdatePrompt(text))
            },
            onRetry = { prompt, bitmap ->
                chatViewModel.onEvent(ChatUiEvent.RetryPrompt(prompt, bitmap))
            },
            onApiSwitch = { provider ->
                chatViewModel.onEvent(ChatUiEvent.SwitchApiProvider(provider))
            },
            onSpeakText = onSpeakText
        )
        // Prompt box - overlays at bottom
        ChatInputBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            chatState = chatState,
            bitmap = bitmap,
            onEvent = { event -> chatViewModel.onEvent(event) },
            onClearImage = { uriState.update { "" } },
            voiceInputLauncher = voiceInputLauncher,
            appColors = appColors
        )
    }
}
