package com.example.mark_vii_demo.features.chat

import android.graphics.Bitmap

sealed class ChatUiEvent {
    data class UpdatePrompt(val newPrompt: String) : ChatUiEvent()
    data class SendPrompt(
        val prompt: String,
        val bitmap: Bitmap?
    ) : ChatUiEvent()

    data class RetryPrompt(
        val prompt: String,
        val bitmap: Bitmap?
    ) : ChatUiEvent()

    object StopStreaming : ChatUiEvent()

    data class SwitchApiProvider(val provider: ApiProvider) : ChatUiEvent()

    object SignOut : ChatUiEvent()

    // Attach file event
    data class AttachFile(
        val uri: String,
        val fileName: String,
        val mimeType: String?
    ) : ChatUiEvent()

    object RemoveAttachedFile : ChatUiEvent()
}




