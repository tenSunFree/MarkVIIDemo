package com.example.mark_vii_demo.features.main.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ApiKeySetupDialog(
    initialUserName: String = "",
    initialApiKey: String = "",
    onConfirm: (String, String) -> Unit
) {
    var userName by remember { mutableStateOf(initialUserName) }
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var showApiKey by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }
    var clipboardError by remember { mutableStateOf<String?>(null) }

    val clipboardManager = LocalClipboardManager.current
    val normalizedName = userName.trim()
    val normalizedKey = apiKey.trim()
    val nameHasError = submitted && normalizedName.isBlank()
    val keyHasError = submitted && !isValidOpenRouterKey(normalizedKey)

    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(text = "Set up OpenRouter")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Enter your name and paste the key you created in OpenRouter.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                    isError = nameHasError,
                    supportingText = {
                        if (nameHasError) {
                            Text("Name cannot be blank")
                        }
                    }
                )

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("OpenRouter Key") },
                    singleLine = true,
                    isError = keyHasError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (showApiKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    val pastedText =
                                        clipboardManager.getText()?.text.orEmpty().trim()
                                    if (pastedText.isBlank()) {
                                        clipboardError = "Clipboard is empty"
                                    } else {
                                        apiKey = pastedText
                                        clipboardError = null
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentPaste,
                                    contentDescription = "Paste key"
                                )
                            }
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Icon(
                                    imageVector = if (showApiKey) {
                                        Icons.Rounded.VisibilityOff
                                    } else {
                                        Icons.Rounded.Visibility
                                    },
                                    contentDescription = if (showApiKey) "Hide key" else "Show key"
                                )
                            }
                        }
                    },
                    supportingText = {
                        when {
                            keyHasError -> Text("Key must start with sk-or-v1-")
                            clipboardError != null -> Text(clipboardError.orEmpty())
                        }
                    }
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "You can create a key from your OpenRouter account settings.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitted = true
                    if (normalizedName.isNotBlank() && isValidOpenRouterKey(normalizedKey)) {
                        onConfirm(normalizedName, normalizedKey)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Confirm")
            }
        }
    )
}

private fun isValidOpenRouterKey(key: String): Boolean {
    return key.startsWith("sk-or-v1-")
}
