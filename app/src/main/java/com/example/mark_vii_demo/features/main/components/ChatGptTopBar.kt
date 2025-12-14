@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.mark_vii_demo.features.main.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * A lightweight, UI-only top bar that mimics the ChatGPT header style.
 *
 * UI layout:
 * - Left: circular hamburger button
 * - Center: title text
 * - Right: pill-shaped action button (e.g., "Login")
 *
 * Design goals:
 * - Stateless / presentational component: no DrawerState, NavController, or coroutines inside.
 * - All interactions are exposed via callbacks to keep it reusable across screens.
 *
 * @param modifier Optional modifier for the root container
 * @param title Center title text
 * @param actionText Right action button label
 * @param onMenuClick Callback for the left hamburger button
 * @param onActionClick Callback for the right action button
 */
@Composable
fun ChatGptTopBar(
    modifier: Modifier = Modifier,
    title: String = "ChatGPT",
    actionText: String = "登入",
    onMenuClick: () -> Unit = {},
    onActionClick: () -> Unit = {},
) {
    val bg = Color(0xFF000000)
    val menuBg = Color(0xFF1A1A1A)
    val menuBorder = Color(0xFF333333)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(68.dp)
            .background(bg)
            .padding(horizontal = 16.dp),
    ) {
        // Left：circular hamburger menu
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .size(48.dp)
                .clip(CircleShape)
                .background(menuBg)
                .border(1.dp, menuBorder, CircleShape)
                .clickable(onClick = onMenuClick),
            contentAlignment = Alignment.Center
        ) {
            HamburgerIcon(color = Color.White)
        }
        // Center：Title
        Text(
            text = title,
            modifier = Modifier.align(Alignment.Center),
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold
        )
        // Right：Login capsule
        Surface(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .height(42.dp)
                .clickable(onClick = onActionClick),
            shape = RoundedCornerShape(999.dp),
            color = Color.White,
            tonalElevation = 0.dp,
            shadowElevation = 0.dp
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = actionText,
                    color = Color.Black,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun HamburgerIcon(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .width(22.dp)
                .height(2.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Box(
            Modifier
                .width(22.dp)
                .height(2.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Box(
            Modifier
                .width(22.dp)
                .height(2.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewChatGptTopBar() {
    MaterialTheme {
        ChatGptTopBar()
    }
}