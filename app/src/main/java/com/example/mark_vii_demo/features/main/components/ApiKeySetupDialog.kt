package com.example.mark_vii_demo.features.main.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun ApiKeySetupDialog(
    initialUserName: String = "",
    initialGeminiApiKey: String = "",
    onConfirm: (userName: String, geminiApiKey: String) -> Unit
) {
    var userName by remember { mutableStateOf(initialUserName) }
    var geminiApiKey by remember { mutableStateOf(initialGeminiApiKey) }
    var apiKeyVisible by remember { mutableStateOf(false) }
    var submitted by remember { mutableStateOf(false) }

    val normalizedName = userName.trim()
    val normalizedApiKey = geminiApiKey.trim()
    val nameHasError = submitted && normalizedName.isBlank()
    val apiKeyHasError = submitted && normalizedApiKey.isBlank()

    AlertDialog(
        onDismissRequest = { },
        title = {
            Text(text = "Welcome to Mark VII")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Please enter your name and Gemini API Key to get started.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Your Name") },
                    singleLine = true,
                    isError = nameHasError,
                    supportingText = {
                        if (nameHasError) Text("Name cannot be blank")
                    }
                )

                OutlinedTextField(
                    value = geminiApiKey,
                    onValueChange = { geminiApiKey = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Gemini API Key") },
                    singleLine = true,
                    isError = apiKeyHasError,
                    visualTransformation = if (apiKeyVisible)
                        VisualTransformation.None
                    else
                        PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                            Icon(
                                imageVector = if (apiKeyVisible)
                                    Icons.Filled.Visibility
                                else
                                    Icons.Filled.VisibilityOff,
                                contentDescription = if (apiKeyVisible) "Hide" else "Show"
                            )
                        }
                    },
                    supportingText = {
                        if (apiKeyHasError) Text("Gemini API Key cannot be blank")
                    }
                )

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Your API Key is stored securely on this device only.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    submitted = true
                    if (normalizedName.isNotBlank() && normalizedApiKey.isNotBlank()) {
                        onConfirm(normalizedName, normalizedApiKey)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get Started")
            }
        }
    )
}