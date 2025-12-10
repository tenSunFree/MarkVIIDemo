package com.example.mark_vii_demo.features.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.mark_vii_demo.ui.theme.LocalAppColors

@Composable
fun TtsIcon(
    isSpeaking: Boolean, modifier: Modifier = Modifier
) {
    val appColors = LocalAppColors.current
    Box(
        modifier = modifier.size(20.dp), contentAlignment = Alignment.Center
    ) {
        // Base layer: Speaker icon (to indicate "this is for text-to-speech")
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
            contentDescription = if (isSpeaking) "Pause speech" else "Speak",
            tint = if (isSpeaking) appColors.accent else appColors.textSecondary,
            modifier = Modifier.fillMaxSize()
        )
        // Corner: Small Play / Pause badge
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 8.dp)
                .size(12.dp)
                .clip(CircleShape)
                .background(
                    MaterialTheme.colorScheme.background.copy(alpha = 0.2f)
                ), // Add a base to prevent the badge from blending with the background
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isSpeaking) {
                    Icons.Rounded.Pause
                } else {
                    Icons.Rounded.PlayArrow
                },
                contentDescription = null,
                tint = if (isSpeaking) appColors.accent else appColors.textSecondary,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}