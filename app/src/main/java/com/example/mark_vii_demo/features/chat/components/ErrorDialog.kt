package com.example.mark_vii_demo.features.chat.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mark_vii_demo.R
import com.example.mark_vii_demo.ui.theme.LocalAppColors

@Composable
fun ErrorDialog(
    errorTitle: String,
    errorMessage: String,
    errorDetails: String,
    isRetryable: Boolean,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null
) {
    val appColors = LocalAppColors.current
    var showDetails by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_pin),
                        contentDescription = "Error",
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = errorTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B6B)
                    )
                }
                Text(
                    text = errorMessage,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 32.dp)
                )
            }
        },
        text = {
            Column {
                if (showDetails) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(appColors.surfaceTertiary)
                            .border(
                                width = 1.dp,
                                color = appColors.divider,
                                shape = RoundedCornerShape(12.dp)
                            )
                    ) {
                        val scrollState = rememberScrollState()
                        SelectionContainer {
                            Text(
                                text = errorDetails,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 13.sp,
                                color = appColors.textSecondary,
                                lineHeight = 18.sp,
                                modifier = Modifier
                                    .verticalScroll(scrollState)
                                    .padding(12.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isRetryable && onRetry != null) {
                        onRetry()
                    }
                    onDismiss()
                },
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = if (isRetryable) appColors.accent else MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(
                    text = if (isRetryable) "Retry" else "OK",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { showDetails = !showDetails },
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF8E8E93)
                )
            ) {
                Text(
                    text = if (showDetails) "Hide Details" else "Details",
                    fontSize = 15.sp
                )
            }
        }
    )
}