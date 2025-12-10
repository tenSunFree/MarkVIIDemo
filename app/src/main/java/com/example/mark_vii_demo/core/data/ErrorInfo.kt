package com.example.mark_vii_demo.core.data

import android.graphics.Bitmap

/**
 * Data class to hold error information for display in error dialogs
 */
data class ErrorInfo(
    val title: String,
    val mainMessage: String,
    val fullDetails: String,
    val isRetryable: Boolean,
    val lastPrompt: String? = null,
    val lastBitmap: Bitmap? = null,
    val rawException: String? = null  // Store complete exception for debugging
)
