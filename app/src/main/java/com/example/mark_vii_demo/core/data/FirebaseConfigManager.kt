package com.example.mark_vii_demo.core.data

import android.annotation.SuppressLint
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manager class to handle Firebase configuration
 * Retain Gemini models and Gemini API key
 */
object FirebaseConfigManager {

    private const val TAG = "FirebaseConfigManager"
    private const val COLLECTION_CONFIG = "app_config"
    private const val DOC_GEMINI_MODELS = "gemini_models"
    private const val DOC_API_KEYS = "api_keys"

    @SuppressLint("StaticFieldLeak")
    private val firestore = FirebaseFirestore.getInstance()

    private val _configState = MutableStateFlow<ConfigState>(ConfigState.Loading)
    val configState: StateFlow<ConfigState> = _configState.asStateFlow()

    private val _geminiModels = MutableStateFlow<List<FirebaseModelInfo>>(emptyList())
    val geminiModels: StateFlow<List<FirebaseModelInfo>> = _geminiModels.asStateFlow()

    private val _geminiApiKey = MutableStateFlow<String>("")
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    sealed class ConfigState {
        object Loading : ConfigState()
        object Success : ConfigState()
        data class Error(val message: String) : ConfigState()
    }

    suspend fun ensureAuth() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            auth.signInAnonymously().await()
        }
    }

    /**
     * Initialize and fetch configuration from Firebase
     */
    suspend fun initialize() {
        Log.d("more", "FirebaseConfigManager, initialize")
        try {
            _configState.value = ConfigState.Loading
            fetchGeminiModels()
            fetchApiKeys()
            _configState.value = ConfigState.Success
            Log.d(TAG, "Firebase configuration loaded successfully")
        } catch (e: Exception) {
            _configState.value = ConfigState.Error(e.message ?: "Failed to load configuration")
            Log.e(TAG, "Error loading Firebase configuration", e)
            loadDefaultConfiguration()
        }
    }

    /**
     * Fetch Gemini models from Firebase Firestore
     */
    private suspend fun fetchGeminiModels() {
        try {
            val document = firestore.collection(COLLECTION_CONFIG)
                .document(DOC_GEMINI_MODELS)
                .get()
                .await()
            if (document.exists()) {
                val modelsList = mutableListOf<FirebaseModelInfo>()
                @Suppress("UNCHECKED_CAST")
                val modelsData = document.get("list") as? List<Map<String, Any>>
                modelsData?.forEach { modelMap ->
                    val model = FirebaseModelInfo(
                        displayName = modelMap["displayName"] as? String ?: "",
                        apiModel = modelMap["apiModel"] as? String ?: "",
                        isAvailable = modelMap["isAvailable"] as? Boolean ?: true,
                        order = (modelMap["order"] as? Long)?.toInt() ?: 0
                    )
                    modelsList.add(model)
                }
                _geminiModels.value = modelsList.sortedBy { it.order }
                Log.d(TAG, "Loaded ${modelsList.size} Gemini models from Firebase")
            } else {
                Log.w(TAG, "Gemini models document not found, using defaults")
                loadDefaultGeminiModels()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching Gemini models", e)
            loadDefaultGeminiModels()
        }
    }

    /**
     * Fetch API keys from Firebase Firestore
     * 只讀取 geminiApiKey，已移除 openrouterApiKey
     */
    private suspend fun fetchApiKeys() {
        Log.d("more", "FirebaseConfigManager, fetchApiKeys")
        try {
            val document = firestore.collection(COLLECTION_CONFIG)
                .document(DOC_API_KEYS)
                .get()
                .await()
            if (document.exists()) {
                val geminiKey = document.getString("geminiApiKey") ?: ""
                Log.d("more", "FirebaseConfigManager, fetchApiKeys, geminiKey loaded")
                _geminiApiKey.value = geminiKey
                Log.d(TAG, "Gemini API key loaded from Firebase")
            } else {
                Log.w(TAG, "API keys document not found")
                _geminiApiKey.value = ""
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching API keys", e)
            _geminiApiKey.value = ""
        }
    }

    private fun loadDefaultGeminiModels() {
        _geminiModels.value = listOf(
            FirebaseModelInfo("Gemini 2.0 Flash (Experimental)", "gemini-2.0-flash-exp", true, 1),
            FirebaseModelInfo("Gemini 1.5 Flash", "gemini-1.5-flash", true, 2),
            FirebaseModelInfo("Gemini 1.5 Flash-8B", "gemini-1.5-flash-8b", true, 3),
            FirebaseModelInfo("Gemini 1.5 Pro", "gemini-1.5-pro", true, 4),
            FirebaseModelInfo("Gemini 1.0 Pro", "gemini-1.0-pro", true, 5)
        )
        Log.w(TAG, "Using default Gemini models")
    }

    private fun loadDefaultConfiguration() {
        loadDefaultGeminiModels()
        _geminiApiKey.value = ""
    }

    suspend fun refresh() {
        initialize()
    }

    fun getCurrentApiKey(): String = _geminiApiKey.value
}