package com.example.mark_vii_demo.features.chat.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mark_vii_demo.R

@Composable
fun AttachmentMenuPopup(
    modifier: Modifier,
    visible: Boolean,
    onPickPhoto: () -> Unit,
    onScanPhoto: () -> Unit,
    onPickFile: () -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        modifier = modifier,
        visible = visible,
        enter = fadeIn(tween(160)) + slideInVertically(
            initialOffsetY = { it / 3 },
            animationSpec = tween(200)
        ),
        exit = fadeOut(tween(120)) + slideOutVertically(
            targetOffsetY = { it / 3 },
            animationSpec = tween(140)
        )
    ) {
        Column(
            modifier = Modifier
                .width(210.dp)
                .shadow(12.dp, RoundedCornerShape(16.dp))
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1C1C1E))
        ) {
            AttachmentMenuItem(
                label = "附加照片",
                iconRes = R.drawable.ic_attach_photo,
                onClick = { onPickPhoto(); onDismiss() }
            )
            MenuDivider()
            AttachmentMenuItem(
                label = "掃描照片",
                iconRes = R.drawable.ic_scan_photo,
                onClick = { onScanPhoto(); onDismiss() }
            )
            MenuDivider()
            AttachmentMenuItem(
                label = "附加文件",
                iconRes = R.drawable.ic_attach_file,
                onClick = { onPickFile(); onDismiss() }
            )
        }
    }
}

@Composable
private fun AttachmentMenuItem(
    label: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 16.sp
        )
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun MenuDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color(0xFF3A3A3C))
    )
}