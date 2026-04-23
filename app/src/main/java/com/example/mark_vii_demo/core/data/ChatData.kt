package com.example.mark_vii_demo.core.data

import android.graphics.Bitmap
import android.util.Log

/**
 * Data class to hold model information
 */
data class ModelInfo(
    val displayName: String,
    val apiModel: String,
    val isAvailable: Boolean = true
)

object ChatData {

    // Currently selected model ID
    var selected_model = ""

    /**
     * Get streaming response using Gemini
     */
    suspend fun getStreamingResponse(
        prompt: String,
        conversationHistory: List<Chat> = emptyList(),
        onChunk: (String) -> Unit,
    ): Chat {
        Log.d("more", "ChatData, getStreamingResponse via Gemini, model: $selected_model")
        val modelToUse = selected_model.ifEmpty { "gemini-1.5-flash" }
        val fullResponse = StringBuilder()
        GeminiClient.generateContentStream(
            prompt = prompt,
            modelName = modelToUse,
            conversationHistory = conversationHistory,
            onChunk = { chunk ->
                fullResponse.append(chunk)
                onChunk(chunk)
            }
        )
        return Chat(
            prompt = fullResponse.toString(),
            bitmap = null,
            isFromUser = false,
            modelUsed = modelToUse
        )
    }

    /**
     * Streaming response with images (Gemini vision)
     */
    suspend fun getStreamingResponseWithImage(
        prompt: String,
        bitmap: Bitmap,
        conversationHistory: List<Chat> = emptyList(),
        onChunk: (String) -> Unit
    ): Chat {
        val modelToUse = selected_model.ifEmpty { "gemini-1.5-flash" }
        return GeminiClient.generateContentWithImage(
            prompt = prompt,
            bitmap = bitmap,
            modelName = modelToUse
        )
    }
}