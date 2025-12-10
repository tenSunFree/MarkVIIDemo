package com.example.mark_vii_demo.core.data

import android.graphics.Bitmap
import java.util.UUID

data class Chat(
    val prompt: String,
    val bitmap: Bitmap?,
    val isFromUser: Boolean,
    val modelUsed: String = "", // Model identifier (e.g., "deepseek/deepseek-chat-v3.1")
    val isStreaming: Boolean = false, // Track if response is still streaming
    val id: String = UUID.randomUUID().toString(), // Unique identifier for each chat
    val isError: Boolean = false // Track if this is an error message
)





