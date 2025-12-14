package com.example.mark_vii_demo.features.main.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import androidx.compose.material3.DrawerState
import kotlinx.coroutines.launch

/**
 * Top bar for the Main screen (container / composition layer).
 *
 * Responsibilities:
 * - Encapsulates Main-screen specific behaviors: toggling the Drawer (open/close)
 *   and handling navigation (defaults to navigating to "info_screen").
 * - Passes behaviors into [ChatGptTopBar] via callbacks, keeping [ChatGptTopBar]
 *   purely UI-focused (stateless / presentational).
 *
 * Why wrap another layer?
 * - Prevents [ChatGptTopBar] from depending on DrawerState, CoroutineScope,
 *   NavController, or other framework/stateful concerns.
 * - Keeps MainActivity / screen code clean: callers only need to use MainTopBar.
 *
 * Parameters:
 * @param drawerState State created by ModalNavigationDrawer, used to open/close the drawer
 * @param navController Used for navigation; if onActionClickOverride is not provided,
 *        the default behavior navigates to "info_screen"
 * @param title Center title text
 * @param actionText Right action button text
 * @param onActionClickOverride Optional override for the right action behavior
 *        (e.g., trigger Google Sign-In instead of navigation)
 */
@Composable
fun MainTopBar(
    drawerState: DrawerState,
    navController: NavHostController,
    title: String = "ChatGPT",
    actionText: String = "登入",
    onActionClickOverride: (() -> Unit)? = null,
) {
    val scope = rememberCoroutineScope()
    ChatGptTopBar(
        title = title, actionText = actionText,
        onMenuClick = {
            scope.launch {
                if (drawerState.isOpen) drawerState.close() else drawerState.open()
            }
        },
        onActionClick = {
            (onActionClickOverride ?: { navController.navigate("info_screen") }).invoke()
        },
    )
}