package com.example.mark_vii_demo.features.chat

import android.graphics.Bitmap
import com.example.mark_vii_demo.core.data.Chat
import com.example.mark_vii_demo.core.data.ErrorInfo

enum class ApiProvider {
    OPENROUTER,
    GEMINI
}

data class ChatState(
    val chatList: MutableList<Chat> = mutableListOf(),
    val prompt: String = "",
    val bitmap: Bitmap? = null,
    val error: ErrorInfo? = null,
    val isGeneratingResponse: Boolean = false,
    val showPromptSuggestions: Boolean = true,
    val currentApiProvider: ApiProvider = ApiProvider.OPENROUTER, // OpenRouter as default
    val hapticTrigger: Long = 0L,
    // Attached files
    val attachedFileUri: String? = null,
    val attachedFileName: String? = null,
    val attachedFileMimeType: String? = null
)





