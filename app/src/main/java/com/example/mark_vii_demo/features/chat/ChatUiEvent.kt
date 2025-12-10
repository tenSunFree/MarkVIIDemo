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
    
    // Authentication events
    object SignInWithGoogle : ChatUiEvent()
    object SignOut : ChatUiEvent()
    
    // Session management events
    object CreateNewSession : ChatUiEvent()
    data class SwitchSession(val sessionId: String) : ChatUiEvent()
    data class RenameSession(val sessionId: String, val newTitle: String) : ChatUiEvent()
    data class DeleteSession(val sessionId: String) : ChatUiEvent()
    object ToggleDrawer : ChatUiEvent()
}




