package com.example.mark_vii_demo.features.chat.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * ChatQuickActionsPanel
 *
 * A lightweight quick-actions section for the empty chat state:
 * - A header text ("What can I do for you?")
 * - Two centered rows of pill buttons (3 items per row)
 *
 * Design goals:
 * - Pills size themselves to content (wrap content)
 * - No business logic / data fetching inside this component (UI only)
 * - Emits the selected action label to the parent via [onActionClick]
 *
 * @param onActionClick Callback invoked when an action is tapped. Currently returns the action label.
 * @param modifier Modifier for external layout customization (spacing, alignment, etc.).
 */
@Composable
fun ChatQuickActionsPanel(
    onActionClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = remember {
        listOf(
            QuickAction("幫我寫", Icons.Outlined.Edit, Color(0xFFB18CFF)),
            QuickAction("分析資料", Icons.Outlined.BarChart, Color(0xFF62D0FF)),
            QuickAction("程式碼", Icons.Outlined.Code, Color(0xFF7C8DFF)),
            QuickAction("構思", Icons.Outlined.Lightbulb, Color(0xFFFFD54A)),
            QuickAction("總結文字", Icons.Outlined.Description, Color(0xFFFFA24A)),
            QuickAction("更多", null, Color(0xFFB0B0B0)),
        )
    }
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "我可以為你做什麼？",
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
        )
        Spacer(modifier = Modifier.height(28.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionPill(actions[0], onActionClick)
                ActionPill(actions[1], onActionClick)
                ActionPill(actions[2], onActionClick)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ActionPill(actions[3], onActionClick)
                ActionPill(actions[4], onActionClick)
                ActionPill(actions[5], onActionClick)
            }
        }
    }
}

private data class QuickAction(
    val label: String,
    val icon: ImageVector?,
    val iconTint: Color,
)

@Composable
private fun ActionPill(
    action: QuickAction,
    onClick: (String) -> Unit,
) {
    val bg = Color(0xFF141414)
    val border = Color(0xFF2A2A2A)
    val text = Color(0xFFBDBDBD)
    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, border),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = Modifier
            .height(52.dp)
            .wrapContentWidth()
            .clickable { onClick(action.label) }
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            action.icon?.let { icon ->
                Icon(
                    imageVector = icon,
                    contentDescription = action.label,
                    tint = action.iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = action.label,
                color = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
