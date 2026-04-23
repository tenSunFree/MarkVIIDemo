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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import com.example.mark_vii_demo.core.data.ChatData
import com.example.mark_vii_demo.core.data.FirebaseConfigManager
import com.example.mark_vii_demo.core.data.GeminiClient
import com.example.mark_vii_demo.core.data.ModelInfo
import com.example.mark_vii_demo.features.chat.components.AttachmentMenuPopup
import com.example.mark_vii_demo.features.chat.components.ChatInputBar
import com.example.mark_vii_demo.features.chat.components.ChatMessageList
import com.example.mark_vii_demo.features.chat.components.getSelectedBitmap
import com.example.mark_vii_demo.ui.theme.LocalAppColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ChatScreen(
    paddingValues: androidx.compose.foundation.layout.PaddingValues,
    chatViewModel: ChatViewModel,
    uriState: MutableStateFlow<String>,
    voiceInputState: MutableStateFlow<String>,
    imagePicker: ActivityResultLauncher<PickVisualMediaRequest>,
    voiceInputLauncher: ActivityResultLauncher<Intent>,
    isTtsInitialized: Boolean,
    textToSpeech: TextToSpeech?,
    onSpeakText: (String) -> Unit,
) {
    val chatState = chatViewModel.chatState.collectAsState().value
    val bitmap = getSelectedBitmap(uriState)
    val voiceInput = voiceInputState.collectAsState().value
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
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
    // Haptic feedback on new chunk
    LaunchedEffect(hapticTrigger) {
        if (hapticTrigger > 0L && hapticTrigger != lastHapticTrigger.value) {
            hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            lastHapticTrigger.value = hapticTrigger
        }
    }
    val appColors = LocalAppColors.current
    // Only observe Firebase settings related to Gemini
    val geminiApiKey by FirebaseConfigManager.geminiApiKey.collectAsState()
    val firebaseGeminiModels by FirebaseConfigManager.geminiModels.collectAsState()
    val geminiModels = remember(firebaseGeminiModels) {
        firebaseGeminiModels.map { firebaseModel ->
            ModelInfo(
                displayName = firebaseModel.displayName,
                apiModel = firebaseModel.apiModel,
                isAvailable = firebaseModel.isAvailable
            )
        }
    }
    // This screen only uses Gemini, and freeModels is kept as an empty array.
    // If OpenRouter support is to be added in the future, it will be replaced with a real data source.
    val freeModels = remember { emptyList<ModelInfo>() }
    // Initialize Firebase to get Gemini API key and models
    LaunchedEffect(Unit) {
        FirebaseConfigManager.initialize()
    }
    // Update Gemini client when key changes
    LaunchedEffect(geminiApiKey) {
        if (geminiApiKey.isNotEmpty()) {
            GeminiClient.updateApiKey(geminiApiKey)
        }
    }
    // Set initial Gemini model once models load
    var hasSetInitialModel by remember { mutableStateOf(false) }
    LaunchedEffect(geminiModels) {
        if (!hasSetInitialModel && geminiModels.isNotEmpty()) {
            ChatData.selected_model = geminiModels[0].apiModel
            hasSetInitialModel = true
        }
    }
    // Update prompt from voice input
    LaunchedEffect(voiceInput) {
        if (voiceInput.isNotEmpty()) {
            chatViewModel.onEvent(ChatUiEvent.UpdatePrompt(voiceInput))
            voiceInputState.update { "" }
        }
    }
    // Auto-scroll to bottom
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    LaunchedEffect(chatState.chatList.size, chatState.isGeneratingResponse) {
        val lastIndex = chatState.chatList.lastIndex
        if (lastIndex >= 0) {
            listState.animateScrollToItem(lastIndex)
        }
    }
    // Model selection state
    val promptItemPosition: MutableState<Int> = remember { mutableStateOf(0) }
    // Update selected model when user changes position in model list
    LaunchedEffect(promptItemPosition.value, geminiModels) {
        if (geminiModels.isNotEmpty() && promptItemPosition.value < geminiModels.size) {
            ChatData.selected_model = geminiModels[promptItemPosition.value].apiModel
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = paddingValues.calculateTopPadding())
    ) {
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
            // This screen only uses Gemini; the selected_model is updated when switching.
            onApiSwitch = { provider ->
                when (provider) {
                    ApiProvider.GEMINI -> {
                        if (geminiModels.isNotEmpty()) {
                            ChatData.selected_model = geminiModels.first().apiModel
                        }
                    }
                }
            },
            onSpeakText = onSpeakText
        )
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
        if (showAttachMenu) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { showAttachMenu = false }
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