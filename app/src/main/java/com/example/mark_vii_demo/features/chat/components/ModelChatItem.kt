package com.example.mark_vii_demo.features.chat.components

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mark_vii_demo.features.chat.ApiProvider
import com.example.mark_vii_demo.R
import com.example.mark_vii_demo.core.data.ChatData
import com.example.mark_vii_demo.core.data.ModelInfo
import com.example.mark_vii_demo.ui.theme.LocalAppColors
import com.example.mark_vii_demo.utils.PdfGenerator
import kotlinx.coroutines.delay

/**
 * model chat text bubble
 * 抽出後的 ModelChatItem
 *
 * 關鍵調整：
 * - 不再直接依賴 MainActivity 的 textToSpeech / speakText / isTtsInitialized
 * - 改成由呼叫端注入：
 *   isTtsReady, isTtsSpeaking, onToggleTts
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ModelChatItem(
    response: String,
    userPrompt: String = "",
    modelUsed: String = "",
    onRetry: (String) -> Unit = {},
    isStreaming: Boolean = false,
    freeModels: List<ModelInfo> = emptyList(),
    geminiModels: List<ModelInfo> = emptyList(),
    currentApiProvider: ApiProvider = ApiProvider.GEMINI,
    hasImage: Boolean = false,
    isError: Boolean = false,
    onApiSwitch: (ApiProvider) -> Unit = {},

    // ✅ TTS 由外部注入
    isTtsReady: Boolean = false,
    isTtsSpeaking: Boolean = false,
    onToggleTts: (String) -> Unit = {},

    // ✅ 若你想把 PDF 行為也抽成外部注入
    //    可以改用 onExportSave/onExportShare
    //    但目前先保留 PdfGenerator 直接呼叫（最少改動）
) {
    val appColors = LocalAppColors.current
    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    var showModelSelector by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var selectedApiProvider by remember { mutableStateOf(currentApiProvider) }

    // Unified Smooth Streaming Engine
    var displayedText by remember { mutableStateOf("") }

    LaunchedEffect(isStreaming) {
        if (isStreaming) {
            displayedText = ""
        } else if (response.isNotEmpty()) {
            displayedText = response
        }
    }

    val currentResponse by rememberUpdatedState(response)

    LaunchedEffect(isStreaming) {
        if (isStreaming) {
            while (true) {
                val target = currentResponse
                val current = displayedText

                if (current.length < target.length) {
                    val diff = target.length - current.length

                    val (charsToProcess, delayMs) = when {
                        diff > 50 -> 5 to 5L
                        diff > 20 -> 2 to 10L
                        diff > 5 -> 1 to 15L
                        else -> 1 to 30L
                    }

                    val nextIndex =
                        (current.length + charsToProcess).coerceAtMost(target.length)
                    val newText = target.substring(0, nextIndex)
                    displayedText = newText

                    if (newText.length % 3 == 0) {
                        hapticFeedback.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove
                        )
                    }

                    delay(delayMs)
                } else {
                    delay(50)
                    if (!isStreaming && current.length == target.length) break
                }
            }
        }
    }

    val brandName = remember(modelUsed) {
        if (modelUsed.isNotEmpty()) {
            val brand = modelUsed.substringBefore("/")
            brand.split("-", "_").firstOrNull()?.replaceFirstChar { it.uppercase() }
                ?: brand.replaceFirstChar { it.uppercase() }
        } else ""
    }

    val headerText = remember(brandName) {
        if (brandName.isNotEmpty()) "Mark VII  x  $brandName" else "Mark VII"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 8.dp, top = 8.dp, bottom = 8.dp)
    ) {
        SelectionContainer {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = headerText,
                    fontSize = 18.sp,
                    fontFamily = FontFamily(Font(R.font.typographica)),
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (modelUsed.isNotEmpty()) {
                    Text(
                        text = modelUsed.replace(":free", ""),
                        fontSize = 12.sp,
                        color = appColors.textSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }

                if (isStreaming) {
                    val infiniteTransition =
                        rememberInfiniteTransition(label = "cursor")
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
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .alpha(cursorAlpha)
                                .graphicsLayer(
                                    scaleX = cursorScale,
                                    scaleY = cursorScale
                                )
                        ) {
                            Text(
                                text = "▋",
                                color = appColors.accent,
                                fontSize = 16.sp
                            )
                        }
                        Text(
                            text = "Generating response...",
                            fontSize = 12.sp,
                            color = appColors.textSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (displayedText.isNotEmpty()) {
                    if (isError) {
                        Text(
                            text = displayedText,
                            fontSize = 16.sp,
                            color = appColors.error,
                            fontFamily = FontFamily.Monospace
                        )
                    } else if (isStreaming) {
                        Text(
                            text = displayedText,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        MarkdownWithCodeCopy(
                            response = displayedText,
                            context = context
                        )
                    }
                }
            }
        }

        // Action buttons row
        if (!isStreaming && response.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .padding(start = 8.dp, top = 4.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Copy
                IconButton(
                    onClick = {
                        val cleanText = response
                            .replace("```[a-zA-Z]*\\n".toRegex(), "")
                            .replace("```", "")
                            .replace("**", "")
                            .replace("*", "")
                            .replace("##", "")
                            .replace("#", "")
                            .replace("`", "")
                            .replace("---", "")
                            .replace("- ", "• ")
                            .trim()

                        val clipboard =
                            context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("response", cleanText)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCopy,
                        contentDescription = "Copy",
                        tint = appColors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Speak (TTS)
                IconButton(
                    onClick = {
                        if (!isTtsReady) {
                            Toast.makeText(context, "Text-to-speech not ready", Toast.LENGTH_SHORT)
                                .show()
                        } else {
                            val cleanText = response
                                .replace("```[a-zA-Z]*\\n".toRegex(), "")
                                .replace("```", "")
                                .replace("**", "")
                                .replace("*", "")
                                .replace("#", "")
                                .replace("`", "")
                                .trim()

                            onToggleTts(cleanText)
                        }
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    TtsIcon(isSpeaking = isTtsSpeaking)
                }

                // Retry
                IconButton(
                    onClick = { showModelSelector = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Retry with different model",
                        tint = appColors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Export PDF
                IconButton(
                    onClick = { showExportDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PictureAsPdf,
                        contentDescription = "Export PDF",
                        tint = appColors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Share
                IconButton(
                    onClick = {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, response)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share response"))
                    },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Share,
                        contentDescription = "Share",
                        tint = appColors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Model selector dialog
        if (showModelSelector) {
            AlertDialog(
                onDismissRequest = { showModelSelector = false },
                containerColor = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = "Retry with Model",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Gemini
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selectedApiProvider == ApiProvider.GEMINI)
                                            appColors.accent.copy(alpha = 0.2f)
                                        else appColors.surfaceVariant
                                    )
                                    .clickable {
                                        selectedApiProvider = ApiProvider.GEMINI
                                        onApiSwitch(ApiProvider.GEMINI)
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Gemini",
                                    color = if (selectedApiProvider == ApiProvider.GEMINI)
                                        appColors.accent else appColors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedApiProvider == ApiProvider.GEMINI)
                                        FontWeight.SemiBold else FontWeight.Normal
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            // OpenRouter
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (selectedApiProvider == ApiProvider.OPENROUTER)
                                            appColors.accent.copy(alpha = 0.2f)
                                        else appColors.surfaceVariant
                                    )
                                    .clickable {
                                        if (hasImage) {
                                            Toast.makeText(
                                                context,
                                                "⚠️ OpenRouter doesn't support images. Please use Gemini for image queries.",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        } else {
                                            selectedApiProvider = ApiProvider.OPENROUTER
                                            onApiSwitch(ApiProvider.OPENROUTER)
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "OpenRouter",
                                    color = if (selectedApiProvider == ApiProvider.OPENROUTER)
                                        appColors.accent else appColors.textSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = if (selectedApiProvider == ApiProvider.OPENROUTER)
                                        FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .height(1.dp)
                                .background(appColors.divider)
                        )

                        val currentModels =
                            if (selectedApiProvider == ApiProvider.GEMINI) geminiModels else freeModels

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 400.dp)
                        ) {
                            if (currentModels.isEmpty()) {
                                item {
                                    Text(
                                        text = "Loading models...",
                                        color = appColors.textSecondary,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                            } else {
                                itemsIndexed(currentModels) { _, model ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(
                                                if (model.apiModel == modelUsed)
                                                    appColors.accent.copy(alpha = 0.1f)
                                                else Color.Transparent
                                            )
                                            .clickable {
                                                if (hasImage && selectedApiProvider == ApiProvider.OPENROUTER) {
                                                    Toast.makeText(
                                                        context,
                                                        "⚠️ Cannot use OpenRouter with images. Switch to Gemini.",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                } else {
                                                    ChatData.selected_model = model.apiModel
                                                    showModelSelector = false
                                                    onRetry(model.apiModel)
                                                }
                                            }
                                            .padding(12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(appColors.accent)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = model.displayName,
                                                color = if (model.apiModel == modelUsed)
                                                    appColors.accent
                                                else MaterialTheme.colorScheme.onSurface,
                                                fontSize = 15.sp,
                                                fontWeight = if (model.apiModel == modelUsed)
                                                    FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { showModelSelector = false },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = appColors.textSecondary
                        )
                    ) {
                        Text("Cancel", fontSize = 15.sp)
                    }
                }
            )
        }

        // Export Dialog (保留你原邏輯)
        if (showExportDialog) {
            AlertDialog(
                onDismissRequest = { showExportDialog = false },
                containerColor = MaterialTheme.colorScheme.surface,
                title = { Text("Export Response", color = MaterialTheme.colorScheme.onSurface) },
                text = {
                    Text(
                        "Choose how you want to export this response as PDF.",
                        color = appColors.textPrimary
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        showExportDialog = false
                        PdfGenerator.exportToPdf(
                            context,
                            response,
                            brandName,
                            modelUsed,
                            userPrompt
                        )
                    }) {
                        Text("Save to Device", color = appColors.accent)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        showExportDialog = false
                        PdfGenerator.sharePdf(context, response, brandName, modelUsed, userPrompt)
                    }) {
                        Text("Share PDF", color = appColors.accent)
                    }
                }
            )
        }
    }
}
