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
    val geminiApiKey: String = "",
    val isConfigured: Boolean = false
)

object SecureUserConfigManager {
    private const val PREFS_NAME = "secure_user_config"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_GEMINI_API_KEY = "gemini_api_key"

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

    fun saveCredentials(userName: String, geminiApiKey: String) {
        val normalizedName = userName.trim()
        val normalizedKey = geminiApiKey.trim()

        prefs.edit()
            .putString(KEY_USER_NAME, normalizedName)
            .putString(KEY_GEMINI_API_KEY, normalizedKey)
            .apply()

        _config.value = UserApiKeyConfig(
            userName = normalizedName,
            geminiApiKey = normalizedKey,
            isConfigured = normalizedName.isNotBlank() && normalizedKey.isNotBlank()
        )
    }

    fun clearCredentials() {
        prefs.edit()
            .remove(KEY_USER_NAME)
            .remove(KEY_GEMINI_API_KEY)
            .apply()
        _config.value = UserApiKeyConfig()
    }

    fun getUserName(): String = _config.value.userName

    fun getGeminiApiKey(): String = _config.value.geminiApiKey

    fun hasCredentials(): Boolean = _config.value.isConfigured

    private fun loadConfig() {
        val userName = prefs.getString(KEY_USER_NAME, "").orEmpty().trim()
        val geminiApiKey = prefs.getString(KEY_GEMINI_API_KEY, "").orEmpty().trim()
        _config.value = UserApiKeyConfig(
            userName = userName,
            geminiApiKey = geminiApiKey,
            isConfigured = userName.isNotBlank() && geminiApiKey.isNotBlank()
        )
    }
}