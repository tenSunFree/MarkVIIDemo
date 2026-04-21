package com.example.mark_vii_demo.features.chat

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mark_vii_demo.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import com.example.mark_vii_demo.core.data.ChatData
import com.example.mark_vii_demo.core.data.FirebaseConfigManager
import com.example.mark_vii_demo.core.data.GeminiClient
import com.example.mark_vii_demo.core.data.ModelInfo
import com.example.mark_vii_demo.core.data.SecureUserConfigManager
import com.example.mark_vii_demo.features.chat.components.AttachmentMenuPopup
import com.example.mark_vii_demo.features.chat.components.ChatInputBar
import com.example.mark_vii_demo.features.chat.components.ChatMessageList
import com.example.mark_vii_demo.features.chat.components.getSelectedBitmap
import com.example.mark_vii_demo.ui.theme.LocalAppColors

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

    var showAttachMenu by remember { mutableStateOf(false) }

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = run {
                val cursor = context.contentResolver.query(it, null, null, null, null)
                cursor?.use { c ->
                    val nameIndex = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                    if (c.moveToFirst() && nameIndex >= 0) c.getString(nameIndex) else "attached_file"
                } ?: "attached_file"
            }
            val mimeType = context.contentResolver.getType(it)
            chatViewModel.onEvent(
                ChatUiEvent.AttachFile(
                    uri = it.toString(),
                    fileName = fileName,
                    mimeType = mimeType
                )
            )
        }
    }

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

    // Observe local OpenRouter key and Firebase-backed Gemini/model config
    val userConfig by SecureUserConfigManager.config.collectAsState()
    val localOpenRouterKey = userConfig.openRouterApiKey
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

    // Update OpenRouter key from local secure storage.
    LaunchedEffect(localOpenRouterKey) {
        if (localOpenRouterKey.isNotEmpty()) {
            ChatData.updateApiKey(localOpenRouterKey)
        }
    }
    LaunchedEffect(geminiApiKey) {
        if (geminiApiKey.isNotEmpty()) {
            GeminiClient.updateApiKey(geminiApiKey)
        }
    }

    // Load free models from OpenRouter AFTER Firebase initializes
    val exceptionModels by FirebaseConfigManager.exceptionModels.collectAsState()

    LaunchedEffect(localOpenRouterKey, exceptionModels) {
        if (localOpenRouterKey.isNotEmpty()) {
            val modelsCacheKey = "${localOpenRouterKey.hashCode()}|${exceptionModels.hashCode()}"
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
        } else {
            freeModels = emptyList()
            isLoadingModels = false
        }
    }

    // Show loading popup while models are loading
    if (isLoadingModels && localOpenRouterKey.isNotEmpty()) {
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
        when (currentApiProvider) {
            ApiProvider.OPENROUTER -> {
                if (freeModels.isNotEmpty()) {
                    ChatData.selected_model = freeModels[0].apiModel
                }
            }

            ApiProvider.GEMINI -> {
                if (geminiModels.isNotEmpty()) {
                    ChatData.selected_model = geminiModels[0].apiModel
                }
            }
        }
    }

    // Set initial model when models load (only once)
    var hasSetInitialModel by remember { mutableStateOf(false) }
    LaunchedEffect(freeModels, geminiModels, currentApiProvider) {
        if (hasSetInitialModel) return@LaunchedEffect

        when (currentApiProvider) {
            ApiProvider.OPENROUTER -> {
                if (freeModels.isNotEmpty() && promptItemPosition.value < freeModels.size) {
                    ChatData.selected_model = freeModels[promptItemPosition.value].apiModel
                    hasSetInitialModel = true
                }
            }

            ApiProvider.GEMINI -> {
                if (geminiModels.isNotEmpty() && promptItemPosition.value < geminiModels.size) {
                    ChatData.selected_model = geminiModels[promptItemPosition.value].apiModel
                    hasSetInitialModel = true
                }
            }
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
            onToggleAttachMenu = { showAttachMenu = !showAttachMenu },
            onDismissAttachMenu = { showAttachMenu = false },
            chatState = chatState,
            bitmap = bitmap,
            onEvent = { event -> chatViewModel.onEvent(event) },
            onClearImage = { uriState.update { "" } },
            voiceInputLauncher = voiceInputLauncher,
            appColors = appColors
        )
        // Click elsewhere to turn off the transparent overlay of the menu
        if (showAttachMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        showAttachMenu = false
                    }
            )
            AttachmentMenuPopup(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 12.dp, bottom = 84.dp),
                onPickPhoto = {
                    showAttachMenu = false
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onScanPhoto = {
                    showAttachMenu = false
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                onPickFile = {
                    showAttachMenu = false
                    filePickerLauncher.launch(
                        arrayOf("text/plain", "text/markdown", "application/json", "text/csv")
                    )
                },
                onDismiss = { showAttachMenu = false },
                visible = showAttachMenu
            )
        }
    }
}
