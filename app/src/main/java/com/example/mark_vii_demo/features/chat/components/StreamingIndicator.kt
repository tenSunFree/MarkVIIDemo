package com.example.mark_vii_demo.features.chat.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Displays an animated streaming indicator with a blinking and scaling cursor and an optional message.
 *
 * @param accentColor color used for the cursor
 * @param secondaryTextColor color used for the message text
 * @param modifier layout modifier for the component
 * @param cursorText character used as the cursor (default "●")
 * @param message optional status message shown next to the cursor
 * @param cursorFontSize cursor font size in sp
 * @param messageFontSize message font size in sp
 */
@Composable
fun StreamingIndicator(
    accentColor: Color,
    secondaryTextColor: Color,
    modifier: Modifier = Modifier,
    cursorText: String = "●",
    message: String = "Generating response...",
    cursorFontSize: Int = 16,
    messageFontSize: Int = 12,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "cursor_blink"
    )
    val cursorScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
        ),
        label = "cursor_scale"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .padding(end = 4.dp)
                .alpha(cursorAlpha)
                .graphicsLayer(scaleX = cursorScale, scaleY = cursorScale)
        ) {
            Text(
                text = cursorText,
                color = accentColor,
                fontSize = cursorFontSize.sp
            )
        }
        // Text(
        //     text = message,
        //     fontSize = messageFontSize.sp,
        //     color = secondaryTextColor
        // )
    }
}
