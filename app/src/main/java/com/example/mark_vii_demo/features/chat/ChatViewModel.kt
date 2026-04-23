package com.example.mark_vii_demo.features.chat

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mark_vii_demo.core.data.Chat
import com.example.mark_vii_demo.core.data.ChatData
import com.example.mark_vii_demo.core.data.ChatHistoryManager
import com.example.mark_vii_demo.core.data.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChatViewModel : ViewModel() {

    private val _chatState = MutableStateFlow(ChatState())
    val chatState = _chatState.asStateFlow()

    private var streamingJob: Job? = null

    init {
        _chatState.update {
            it.copy(chatList = mutableListOf(), showPromptSuggestions = true)
        }
    }

    fun onEvent(event: ChatUiEvent) {
        Log.d("ChatVM", "onEvent: $event")
        when (event) {
            is ChatUiEvent.SendPrompt -> {
                if (event.prompt.isNotEmpty() || event.bitmap != null) {
                    addPrompt(event.prompt, event.bitmap)
                    if (event.bitmap != null) {
                        getResponseWithImage(event.prompt, event.bitmap)
                    } else {
                        getResponse(event.prompt)
                    }
                }
            }

            is ChatUiEvent.RetryPrompt -> {
                if (event.prompt.isNotEmpty()) {
                    _chatState.update { it.copy(isGeneratingResponse = true) }
                    if (event.bitmap != null) {
                        getResponseWithImage(event.prompt, event.bitmap)
                    } else {
                        getResponse(event.prompt, isRetry = true)
                    }
                }
            }

            is ChatUiEvent.UpdatePrompt -> {
                _chatState.update { it.copy(prompt = event.newPrompt) }
            }

            is ChatUiEvent.StopStreaming -> {
                streamingJob?.cancel()
                streamingJob = null
                _chatState.update { state ->
                    val updatedList = state.chatList.toMutableList()
                    val last = updatedList.lastIndex
                    if (last >= 0 && updatedList[last].isStreaming) {
                        updatedList[last] = updatedList[last].copy(isStreaming = false)
                    }
                    state.copy(chatList = updatedList, isGeneratingResponse = false)
                }
            }

            is ChatUiEvent.AttachFile -> {
                _chatState.update {
                    it.copy(
                        attachedFileUri = event.uri,
                        attachedFileName = event.fileName,
                        attachedFileMimeType = event.mimeType
                    )
                }
            }

            is ChatUiEvent.RemoveAttachedFile -> {
                _chatState.update {
                    it.copy(
                        attachedFileUri = null,
                        attachedFileName = null,
                        attachedFileMimeType = null
                    )
                }
            }

            else -> {}
        }
    }

    fun clearError() {
        _chatState.update { it.copy(error = null) }
    }

    fun showWelcomeGuide() {
        _chatState.update {
            it.copy(showPromptSuggestions = true)
        }
    }

    fun dismissPromptSuggestions() {
        _chatState.update { it.copy(showPromptSuggestions = false) }
    }

    fun clearChatHistory() {
        ChatHistoryManager.clearChatHistory()
        _chatState.update {
            it.copy(chatList = mutableListOf(), showPromptSuggestions = true)
        }
    }

    private fun addPrompt(prompt: String, bitmap: Bitmap?) {
        _chatState.update {
            it.copy(
                chatList = it.chatList.toMutableList().apply {
                    add(Chat(prompt, bitmap, true))
                },
                prompt = "",
                bitmap = null,
                showPromptSuggestions = false,
                isGeneratingResponse = true
            )
        }
        saveChatHistory()
    }

    private fun getResponse(prompt: String, isRetry: Boolean = false) {
        streamingJob = viewModelScope.launch {
            try {
                val finalPrompt = buildPromptWithAttachedFile(prompt)
                val streamingChat = Chat(
                    prompt = "",
                    bitmap = null,
                    isFromUser = false,
                    modelUsed = ChatData.selected_model,
                    isStreaming = true
                )
                _chatState.update {
                    it.copy(chatList = it.chatList.toMutableList().apply { add(streamingChat) })
                }
                val list = _chatState.value.chatList
                val historySource = if (list.last().isStreaming) list.dropLast(1) else list
                val historyFiltered = if (isRetry) {
                    historySource.dropLastWhile { !it.isFromUser }
                } else historySource
                val conversationHistory = historyFiltered.filter { !it.isStreaming }
                // Go directly to Gemini
                val modelToUse = ChatData.selected_model.ifEmpty { "gemini-1.5-flash" }
                val fullResponse = StringBuilder()
                GeminiClient.generateContentStream(
                    prompt = finalPrompt,
                    modelName = modelToUse,
                    conversationHistory = conversationHistory,
                    onChunk = { chunk ->
                        fullResponse.append(chunk)
                        _chatState.update { state ->
                            val updatedList = state.chatList.toMutableList()
                            val last = updatedList.lastIndex
                            if (last >= 0) {
                                updatedList[last] = updatedList[last].copy(
                                    prompt = updatedList[last].prompt + chunk,
                                    isStreaming = true
                                )
                            }
                            state.copy(chatList = updatedList)
                        }
                    }
                )
                val finalChat = Chat(
                    prompt = fullResponse.toString(),
                    bitmap = null,
                    isFromUser = false,
                    modelUsed = modelToUse,
                    isStreaming = false
                )
                _chatState.update { state ->
                    val updatedList = state.chatList.toMutableList()
                    val last = updatedList.lastIndex
                    if (last >= 0) updatedList[last] = finalChat
                    state.copy(
                        chatList = updatedList,
                        isGeneratingResponse = false,
                        attachedFileUri = null,
                        attachedFileName = null,
                        attachedFileMimeType = null
                    )
                }
                saveChatHistory()
            } catch (e: Exception) {
                _chatState.update { state ->
                    state.copy(
                        chatList = state.chatList.toMutableList().apply {
                            val last = lastIndex
                            if (last >= 0 && !get(last).isFromUser && get(last).isStreaming) removeAt(
                                last
                            )
                        },
                        isGeneratingResponse = false
                    )
                }
                handleError(e, prompt, null)
            }
        }
    }

    private fun getResponseWithImage(prompt: String, bitmap: Bitmap) {
        streamingJob = viewModelScope.launch {
            try {
                val streamingChat = Chat(
                    prompt = "",
                    bitmap = null,
                    isFromUser = false,
                    modelUsed = ChatData.selected_model,
                    isStreaming = true
                )
                _chatState.update {
                    it.copy(chatList = it.chatList.toMutableList().apply { add(streamingChat) })
                }
                // All images are from Gemini
                val geminiModel = ChatData.selected_model.ifBlank { "gemini-1.5-flash" }
                val chat = GeminiClient.generateContentWithImage(
                    prompt = prompt,
                    bitmap = bitmap,
                    modelName = geminiModel
                )
                _chatState.update { state ->
                    val updatedList = state.chatList.toMutableList()
                    val last = updatedList.lastIndex
                    if (last >= 0 && updatedList[last].isStreaming) {
                        updatedList[last] = chat.copy(isStreaming = false)
                    }
                    state.copy(chatList = updatedList, isGeneratingResponse = false)
                }
                saveChatHistory()
            } catch (e: Exception) {
                _chatState.update { it.copy(isGeneratingResponse = false) }
                handleError(e, prompt, bitmap)
            }
        }
    }

    private suspend fun buildPromptWithAttachedFile(originalPrompt: String): String {
        val state = _chatState.value
        val fileUri = state.attachedFileUri ?: return originalPrompt
        val fileName = state.attachedFileName ?: return originalPrompt
        val mimeType = state.attachedFileMimeType ?: ""
        val isTextType = mimeType.startsWith("text/") ||
                mimeType == "application/json" ||
                mimeType == "application/xml"
        if (!isTextType) return originalPrompt
        return try {
            withContext(Dispatchers.IO) {
                state.attachedFileUri
            }
            """
$originalPrompt

---
附加檔案：$fileName
檔案內容：
${state.attachedFileUri}
            """.trimIndent()
        } catch (e: Exception) {
            originalPrompt
        }
    }

    private fun handleError(e: Exception, prompt: String, bitmap: Bitmap?) {
        val errorMessage = e.message ?: "Unknown error occurred"
        val parts = errorMessage.split("|", limit = 2)
        val errorCode = if (parts.size == 2) parts[0] else "UNKNOWN_ERROR"
        val errorDetails = if (parts.size == 2) parts[1] else errorMessage
        val formattedError = "❌ Error: $errorCode\n\n$errorDetails"
        Log.d("ChatViewModel", "ChatViewModel, handleError, formattedError: $formattedError")
        val errorChat = Chat(
            prompt = formattedError,
            bitmap = null,
            isFromUser = false,
            modelUsed = ChatData.selected_model,
            isStreaming = false,
            isError = true
        )
        _chatState.update {
            it.copy(
                chatList = it.chatList.toMutableList().apply { add(errorChat) },
                isGeneratingResponse = false
            )
        }
        saveChatHistory()
    }

    private fun saveChatHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            ChatHistoryManager.saveChatHistory(_chatState.value.chatList)
        }
    }
}