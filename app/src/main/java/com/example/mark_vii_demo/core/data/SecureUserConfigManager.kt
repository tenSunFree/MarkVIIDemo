package com.example.mark_vii_demo.core.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserApiKeyConfig(
    val userName: String = "",
    val openRouterApiKey: String = "",
    val isConfigured: Boolean = false
)

object SecureUserConfigManager {
    private const val PREFS_NAME = "secure_user_config"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_OPENROUTER_API_KEY = "openrouter_api_key"

    private lateinit var prefs: SharedPreferences

    private val _config = MutableStateFlow(UserApiKeyConfig())
    val config: StateFlow<UserApiKeyConfig> = _config.asStateFlow()

    fun init(context: Context) {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        loadConfig()
    }

    fun saveCredentials(userName: String, openRouterApiKey: String) {
        val normalizedName = userName.trim()
        val normalizedKey = openRouterApiKey.trim()

        prefs.edit()
            .putString(KEY_USER_NAME, normalizedName)
            .putString(KEY_OPENROUTER_API_KEY, normalizedKey)
            .apply()

        _config.value = UserApiKeyConfig(
            userName = normalizedName,
            openRouterApiKey = normalizedKey,
            isConfigured = hasValidCredentials(normalizedName, normalizedKey)
        )
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_USER_NAME)
            .remove(KEY_OPENROUTER_API_KEY)
            .apply()

        _config.value = UserApiKeyConfig()
    }

    fun getOpenRouterApiKey(): String = _config.value.openRouterApiKey

    fun getUserName(): String = _config.value.userName

    fun hasCredentials(): Boolean = _config.value.isConfigured

    private fun loadConfig() {
        val userName = prefs.getString(KEY_USER_NAME, "").orEmpty().trim()
        val apiKey = prefs.getString(KEY_OPENROUTER_API_KEY, "").orEmpty().trim()

        _config.value = UserApiKeyConfig(
            userName = userName,
            openRouterApiKey = apiKey,
            isConfigured = hasValidCredentials(userName, apiKey)
        )
    }

    private fun hasValidCredentials(userName: String, apiKey: String): Boolean {
        return userName.isNotBlank() && apiKey.startsWith("sk-or-v1-")
    }
}
