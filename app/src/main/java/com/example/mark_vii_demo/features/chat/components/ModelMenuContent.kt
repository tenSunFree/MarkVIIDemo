package com.example.mark_vii_demo.features.chat.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.mark_vii_demo.core.data.ModelInfo
import com.example.mark_vii_demo.features.chat.ApiProvider
import com.example.mark_vii_demo.ui.theme.AppColors

@Composable
fun ModelMenuContent(
    currentModels: List<ModelInfo>,
    currentApiProvider: ApiProvider,
    promptItemPosition: Int,
    appColors: AppColors, // custom LocalAppColors type
    onReloadModels: (() -> Unit)?,
    onSelectModel: (index: Int, model: ModelInfo) -> Unit,
) {
    // Log.d("more", "ModelMenuContent, currentModels: ${currentModels.size}")
    // Log.d("more", "ModelMenuContent, currentModels[0].displayName: ${currentModels[0].displayName}")
    // Log.d("more", "ModelMenuContent, currentModels[1].displayName: ${currentModels[1].displayName}")
    // Log.d("more", "ModelMenuContent, currentModels[2].displayName: ${currentModels[2].displayName}")
    if (currentModels.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "No models available",
                color = appColors.textSecondary,
                fontSize = 14.sp,
                style = MaterialTheme.typography.bodyMedium
            )
            // Show Reload only for OpenRouter
            if (currentApiProvider == ApiProvider.OPENROUTER && onReloadModels != null) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(appColors.accent.copy(alpha = 0.15f))
                        .clickable { onReloadModels() }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reload models",
                            tint = appColors.accent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Reload Models",
                            color = appColors.accent,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    } else {
        currentModels.forEachIndexed { index, model ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                DropdownMenuItem(
                    text = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (model.isAvailable) appColors.accent
                                        else appColors.textSecondary
                                    )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = model.displayName,
                                color = if (promptItemPosition == index) appColors.accent
                                else MaterialTheme.colorScheme.onSurface,
                                fontSize = 15.sp,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (promptItemPosition == index) FontWeight.SemiBold
                                else FontWeight.Normal
                            )
                        }
                    },
                    onClick = { onSelectModel(index, model) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (promptItemPosition == index) appColors.accent.copy(alpha = 0.1f)
                            else appColors.surfaceVariant
                        ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 12.dp),
                    colors = MenuDefaults.itemColors(
                        textColor = MaterialTheme.colorScheme.onSurface,
                        leadingIconColor = MaterialTheme.colorScheme.onSurface,
                        trailingIconColor = MaterialTheme.colorScheme.onSurface,
                        disabledTextColor = appColors.textSecondary,
                        disabledLeadingIconColor = appColors.textSecondary,
                        disabledTrailingIconColor = appColors.textSecondary
                    )
                )
            }
        }
    }
}