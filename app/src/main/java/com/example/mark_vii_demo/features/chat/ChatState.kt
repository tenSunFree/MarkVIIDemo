package com.example.mark_vii_demo.features.chat

import android.graphics.Bitmap
import com.example.mark_vii_demo.core.data.Chat
import com.example.mark_vii_demo.core.data.ErrorInfo
import com.example.mark_vii_demo.core.data.ChatSession
import com.google.firebase.auth.FirebaseUser

enum class ApiProvider {
    OPENROUTER,
    GEMINI
}

data class ChatState (
    val chatList: MutableList<Chat> = mutableListOf(),
    val prompt: String = "",
    val bitmap: Bitmap? = null,
    val error: ErrorInfo? = null,
    val isGeneratingResponse: Boolean = false,
    val showPromptSuggestions: Boolean = true,
    val currentApiProvider: ApiProvider = ApiProvider.OPENROUTER, // OpenRouter as default
    val hapticTrigger: Long = 0L, // Timestamp to trigger haptic feedback on chunk arrival
    
    // Authentication and session management
    val currentUser: FirebaseUser? = null,
    val currentSessionId: String? = null,
    val chatSessions: List<ChatSession> = emptyList(),
    val isDrawerOpen: Boolean = false
)





