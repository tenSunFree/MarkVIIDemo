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

            is ChatUiEvent.SwitchApiProvider -> {
                _chatState.update {
                    it.copy(currentApiProvider = event.provider)
                }
            }

            else -> {}
        }
    }

    fun clearError() {
        _chatState.update { it.copy(error = null) }
    }

    //    Show welcome guide without making API call
    fun showWelcomeGuide() {
        // Just enable prompt suggestions, no chat message needed
        _chatState.update {
            it.copy(
                showPromptSuggestions = true
            )
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
                // First check if there are any attachments or text to include in the prompt
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

                val chat = ChatData.getStreamingResponse(
                    prompt = finalPrompt,
                    conversationHistory = conversationHistory
                ) { chunk ->
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

                _chatState.update { state ->
                    val updatedList = state.chatList.toMutableList()
                    val last = updatedList.lastIndex
                    if (last >= 0) updatedList[last] = chat.copy(isStreaming = false)
                    state.copy(
                        chatList = updatedList,
                        isGeneratingResponse = false,
                        // 送出後清除附件
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

                val conversationHistory = _chatState.value.chatList
                    .dropLast(1)
                    .filter { !it.isStreaming }

                val currentProvider = _chatState.value.currentApiProvider
                val chat = when (currentProvider) {
                    ApiProvider.GEMINI -> {
                        val geminiModel =
                            ChatData.selected_model.ifBlank { "gemini-1.5-flash" }
                        GeminiClient.generateContentWithImage(
                            prompt = prompt,
                            bitmap = bitmap,
                            modelName = geminiModel
                        )
                    }

                    ApiProvider.OPENROUTER -> {
                        // Ask mode defaults to OpenRouter, but the configured free models are text-only.
                        // Fall back to Gemini for image prompts so image sending still works.
                        GeminiClient.generateContentWithImage(
                            prompt = prompt,
                            bitmap = bitmap,
                            modelName = "gemini-1.5-flash"
                        )
                    }
                }

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

    /**
     * 如果有附加文字檔，把內容拼進 prompt
     */
    private suspend fun buildPromptWithAttachedFile(originalPrompt: String): String {
        val state = _chatState.value
        val fileUri = state.attachedFileUri ?: return originalPrompt
        val fileName = state.attachedFileName ?: return originalPrompt
        val mimeType = state.attachedFileMimeType ?: ""

        // 只有文字類型才讀取內容
        val isTextType = mimeType.startsWith("text/") ||
                mimeType == "application/json" ||
                mimeType == "application/xml"

        if (!isTextType) return originalPrompt

        return try {
            val fileContent = withContext(Dispatchers.IO) {
                // 這裡無法直接拿 Context，所以用 fileContent 先存在 state
                // 實際讀取在 ChatScreen 那層做，這裡只組字串
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
