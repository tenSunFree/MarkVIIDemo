package com.example.mark_vii_demo.core.data

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.ResponseBody
import retrofit2.HttpException
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Data class to hold model information
 */
data class ModelInfo(
    val displayName: String,
    val apiModel: String,
    val isAvailable: Boolean = true
)

object ChatData {

    // API key loaded ONLY from Firebase - no local fallback
    var openrouter_api_key: String = ""

    var selected_model = ""

    // Cache for free models so we do not re-fetch on view recreation
    var cachedFreeModels: List<ModelInfo> = emptyList()
    var cachedFreeModelsKey: String = ""

    private fun ResponseBody?.safeString(): String? = try {
        this?.string()
    } catch (_: Exception) {
        null
    }

    /**
     * Update API key from Firebase
     * This is the ONLY way to set the API key
     */
    fun updateApiKey(newKey: String) {
        if (newKey.isNotEmpty()) {
            openrouter_api_key = newKey
            OpenRouterClient.updateApiKey(openrouter_api_key)
        }
    }

    private fun isOpenRouterTimeout(e: Throwable, body: String?): Boolean {
        // HTTP 408
        if (e is HttpException && e.code() == 408) return true
        // Response body contains common OpenRouter/Cloudflare timeout phrases
        val msg = (body ?: e.message).orEmpty()
        return msg.contains("Operation timed out", ignoreCase = true) ||
                msg.contains("timed out", ignoreCase = true)
    }

    /**
     * Fetch all available models from OpenRouter
     * Returns list of models with pricing info
     */
    suspend fun fetchAvailableModels(): List<ModelData> {
        return withContext(Dispatchers.IO) {
            var lastError: Throwable? = null
            var lastBody: String? = null
            repeat(2) { attempt -> // Up to 2 attempts: initial try + one retry
                try {
                    val response = OpenRouterClient.api.getModels()
                    return@withContext response.data ?: emptyList()
                } catch (e: Exception) {
                    lastError = e
                    lastBody =
                        if (e is HttpException) e.response()?.errorBody().safeString() else null
                    val shouldRetry = isOpenRouterTimeout(e, lastBody) && attempt == 0
                    Log.w(
                        "more",
                        "ChatData, fetchAvailableModels failed attempt=${attempt + 1}, " +
                                "shouldRetry=$shouldRetry, err=${e.javaClass.simpleName}, " +
                                "code=${(e as? HttpException)?.code()}, body=$lastBody"
                    )
                    if (shouldRetry) {
                        // Small delay to avoid immediately hitting the same congestion
                        delay(500)
                        return@repeat
                    } else {
                        return@withContext emptyList()
                    }
                }
            }
            // Shouldn't reach here in theory, but keep as a safeguard
            Log.e(
                "more",
                "ChatData, fetchAvailableModels retry exhausted: ${lastError?.message}, body=$lastBody"
            )
            emptyList()
        }
        // return try {
        //     val response = withContext(Dispatchers.IO) {
        //         OpenRouterClient.api.getModels()
        //     }
        //     response.data ?: emptyList()
        // } catch (e: Exception) {
        //     emptyList()
        // }
    }

    /**
     * Fetch only FREE models from OpenRouter
     * A model is considered free if prompt and completion prices are "0"
     */
    suspend fun fetchFreeModels(): List<ModelInfo> {
        return try {
            // First, ensure exception models are loaded from Firebase
            val exceptionModelsMap = FirebaseConfigManager.exceptionModels.value
            Log.d(
                "more",
                "ChatData, fetchFreeModels, exceptionModelsMap.size: ${exceptionModelsMap.size}"
            );
            Log.d("more", "ChatData, fetchFreeModels, exceptionModelsMap: ${exceptionModelsMap}");

            val allModels = fetchAvailableModels()
            Log.d("more", "ChatData, fetchFreeModels, allModels.size: ${allModels.size}")
            // Log.d("more", "ChatData, fetchFreeModels, allModels: ${allModels}");
            allModels.forEach {
                Log.d("more", "ChatData, fetchFreeModels, displayName: ${it.name}");
                Log.d("more", "ChatData, fetchFreeModels, it: ${it}");
            }

            // Use a map to deduplicate models by base ID (without :free suffix)
            val uniqueModels = mutableMapOf<String, ModelInfo>()

            allModels.filter { model ->
                val pricing = model.pricing
                val promptPrice = pricing?.prompt?.toDoubleOrNull() ?: 1.0
                val completionPrice = pricing?.completion?.toDoubleOrNull() ?: 1.0

                // Free models have 0 cost for both prompt and completion
                promptPrice == 0.0 && completionPrice == 0.0
            }.forEach { model ->
                // Clean up display name
                val cleanDisplayName = (model.name ?: model.id)
                    .replace("(free)", "", ignoreCase = true)
                    .replace("  ", " ")
                    .trim()

                // Get base model ID without :free suffix for deduplication
                val modelIdWithoutFree = model.id.replace(":free", "", ignoreCase = true)

                // Check if this model (without :free) is in exception list
                val isInExceptionList = exceptionModelsMap.keys.any { exceptionId ->
                    val exceptionIdWithoutFree = exceptionId.replace(":free", "", ignoreCase = true)
                    exceptionIdWithoutFree.equals(modelIdWithoutFree, ignoreCase = true)
                }

                // Determine final API model ID
                val cleanApiModel = if (isInExceptionList) {
                    // Keep or add :free postfix for exception models
                    if (model.id.endsWith(":free", ignoreCase = true)) {
                        model.id
                    } else {
                        "$modelIdWithoutFree:free"
                    }
                } else {
                    // Remove :free for non-exception models
                    modelIdWithoutFree
                }

                // Only add if not already present (deduplication by base ID)
                if (!uniqueModels.containsKey(modelIdWithoutFree)) {
                    uniqueModels[modelIdWithoutFree] = ModelInfo(
                        displayName = cleanDisplayName,
                        apiModel = cleanApiModel,
                        isAvailable = true
                    )
                }
            }

            uniqueModels.values.toList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Get cached free models, or fetch them if not already loaded
     */
    suspend fun getOrFetchFreeModels(cacheKey: String? = null): List<ModelInfo> {
        Log.d("more", "ChatData, getOrFetchFreeModels, cacheKey: $cacheKey");
        Log.d("more", "ChatData, getOrFetchFreeModels, cachedFreeModels: $cachedFreeModels");
        Log.d("more", "ChatData, getOrFetchFreeModels, cachedFreeModelsKey: $cachedFreeModelsKey");
        // Use cached data if available (also when cacheKey is null)
        if (cachedFreeModels.isNotEmpty() && (cacheKey == null || cacheKey == cachedFreeModelsKey)) {
            return cachedFreeModels
        }
        val models = fetchFreeModels()
        Log.d("more", "ChatData, getOrFetchFreeModels, models: ${models.size}");
        models.forEach {
            Log.d("more", "ChatData, getOrFetchFreeModels, displayName: ${it.displayName}");
        }

        cachedFreeModels = models
        cachedFreeModelsKey = cacheKey ?: ""
        return models
    }

    private suspend fun ensureFreeModelsLoadedIfNeeded(cacheKey: String? = null) {
        if (cachedFreeModels.isNotEmpty()) return
        try {
            withTimeout(8_000) { // 8 seconds, adjust as needed
                getOrFetchFreeModels(cacheKey)
            }
            Log.d(
                "more",
                "ChatData, ensureFreeModelsLoadedIfNeeded ok, size=${cachedFreeModels.size}"
            )
        } catch (e: TimeoutCancellationException) {
            Log.w("more", "ChatData, ensureFreeModelsLoadedIfNeeded timeout")
        } catch (e: Exception) {
            Log.w("more", "ChatData, ensureFreeModelsLoadedIfNeeded failed: ${e.message}")
        }
    }

    /**
     * Remove :free postfix from model ID
     */
    private fun String.normalizeModelId(): String =
        this.replace(":free", "", ignoreCase = true).trim()

    /**
     * Get streaming response from AI model with conversation history
     * Yields partial responses as they are generated
     */
    suspend fun getStreamingResponse(
        prompt: String,
        conversationHistory: List<Chat> = emptyList(),
        retry404: Boolean = false,        // Only allow one retry
        excludeModelId: String? = null,   // Exclude the model that just returned 404 when retrying
        onChunk: (String) -> Unit,
    ): Chat = withContext(Dispatchers.IO) {
        Log.d("more", "ChatData, getStreamingResponse, prompt: $prompt")
        Log.d("more", "ChatData, getStreamingResponse, selected_model: $selected_model")
        // Log.d("more", "ChatData, getStreamingResponse, conversationHistory.size: ${conversationHistory.size}")
        // Log.d("more", "ChatData, getStreamingResponse, conversationHistory: $conversationHistory")
        var modelToUse = ""
        try {
            // Check if API key is loaded
            if (openrouter_api_key.isEmpty()) {
                throw Exception("API_KEY_MISSING|API key is not configured")
            }
            // Ensure cached free models are loaded (use exceptionModels hash as cacheKey so updates invalidate the cache)
            ensureFreeModelsLoadedIfNeeded(
                cacheKey = FirebaseConfigManager.exceptionModels.value.hashCode().toString()
            )
            // Randomize selected_model on every request if we have cached free models
            if (cachedFreeModels.isNotEmpty()) {
                // val excluded = listOf("deepseek", "gemma", "gemini")
                val excluded = listOf(
                    "deepseek-r1t-chimera",
                    "gemma-3n-e2b-it",
                    "deepseek-r1t-chimera",
                    "gemini-2.0-flash-exp",
                    "gemma-3-4b-it",
                    "gemma-3-27b-it",
                    "qwen3-4b",
                    "qwen-2.5-vl-7b-instruct",
                    "lfm-2.5-1.2b-thinking",
                    "lfm-2.5-1.2b-instruct",
                    "molmo-2-8b"
                )
                val excludeNormalized = excludeModelId?.normalizeModelId()
                val picked = cachedFreeModels
                    .filter { m ->
                        // excluded.none { kw ->
                        //     m.apiModel.contains(
                        //         kw,
                        //         ignoreCase = true
                        //     )
                        // }
                        // Original keyword exclusion
                        excluded.none { kw -> m.apiModel.contains(kw, ignoreCase = true) } &&
                                // Also exclude the model that just returned 404 (ignore :free suffix differences)
                                (excludeNormalized == null || m.apiModel.normalizeModelId() != excludeNormalized)
                    }
                    .randomOrNull()
                // If no other options remain after filtering, set selected_model to empty so the default model will be used
                selected_model = picked?.apiModel.orEmpty()
                Log.d(
                    "more",
                    "ChatData, getStreamingResponse, pickedModel: ${picked?.displayName} / ${picked?.apiModel}"
                )
            }
            modelToUse = selected_model.ifEmpty {
                "anthropic/claude-3-5-sonnet-20241022"
            }
            Log.d("more", "ChatData, getStreamingResponse, modelToUse(random): $modelToUse")
            // Build messages array from conversation history
            val messages = mutableListOf<Message>()
            // Add conversation history (limit to last 6 messages for faster response)
            // Skip known model-not-found error messages in history
            conversationHistory.takeLast(6).forEach { chat ->
                val text = chat.prompt
                // Skip known model-not-found error messages in history
                if (text.contains("Error: MODEL_NOT_FOUND", ignoreCase = true)) {
                    Log.d("more", "ChatData, getStreamingResponse, skip error history: $text")
                    return@forEach
                }
                Log.d("more", "ChatData, getStreamingResponse, chat.prompt: $text")
                messages.add(
                    Message(
                        role = if (chat.isFromUser) "user" else "assistant",
                        content = listOf(
                            Content(
                                type = "text",
                                text = text
                            )
                        )
                    )
                )
            }
            // Add current prompt as the latest user message
            messages.add(
                Message(
                    role = "user",
                    content = listOf(
                        Content(
                            type = "text",
                            text = prompt
                        )
                    )
                )
            )
            // If the prompt is an empty string, throw an error immediately (to avoid wasting a request)
            if (prompt.isBlank()) {
                throw Exception("BAD_REQUEST|Prompt is empty")
            }
            val request = OpenRouterRequest(
                model = modelToUse,
                messages = messages,
                max_tokens = 3000,
                // max_tokens = 1536,
                temperature = 0.7,
                stream = true
            )
            val responseBody = OpenRouterClient.api.chatCompletionStream(request)
            val fullResponse = StringBuilder()
            // Read SSE stream with better error handling
            try {
                responseBody.byteStream().bufferedReader().use { reader ->
                    reader.lineSequence().forEach { line ->
                        if (line.startsWith("data: ")) {
                            val data = line.substring(6)
                            if (data == "[DONE]") return@forEach
                            try {
                                val json = Gson().fromJson(data, JsonObject::class.java)
                                val delta = json.getAsJsonArray("choices")
                                    ?.get(0)?.asJsonObject
                                    ?.getAsJsonObject("delta")
                                    ?.get("content")?.asString
                                if (delta != null) {
                                    fullResponse.append(delta)
                                    withContext(Dispatchers.Main) {
                                        onChunk(delta)
                                    }
                                }
                            } catch (e: Exception) {
                                // Skip malformed chunks
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                Log.d("more", "ChatData, getStreamingResponse, responseBody, catch, e: $e")
                // If we got some response before connection error, return what we have
                if (fullResponse.isNotEmpty()) {
                    return@withContext Chat(
                        prompt = fullResponse.toString(),
                        bitmap = null,
                        isFromUser = false,
                        modelUsed = modelToUse
                    )
                }
                throw Exception("NETWORK_ERROR|Connection interrupted: ${e.message ?: "Network error"}")
            }
            return@withContext Chat(
                prompt = fullResponse.toString(),
                bitmap = null,
                isFromUser = false,
                modelUsed = modelToUse
            )
        } catch (e: Exception) {
            Log.d("more", "ChatData, getStreamingResponse, catch, e: $e")
            // Re-throw if already formatted
            if (e.message?.contains("|") == true) {
                throw e
            }
            // Handle HTTP errors with specific codes
            val errorMessage = when {
                e is HttpException -> {
                    when (e.code()) {
                        400 -> "BAD_REQUEST|Invalid request parameters or CORS issue"
                        401 -> "UNAUTHORIZED|Invalid API key or expired session"
                        402 -> "INSUFFICIENT_CREDITS|Your account has insufficient credits"
                        403 -> "CONTENT_FLAGGED|Your input was flagged by moderation"
                        404, 429 -> {
                            // Model not found - retry once with a different random model
                            // {"message":"No endpoints found for xxx/yyy.","code":404}
                            if (!retry404) {
                                Log.w(
                                    "more",
                                    "ChatData, 404 MODEL_NOT_FOUND, retry once with a new random model. failed=$modelToUse"
                                )
                                return@withContext getStreamingResponse(
                                    prompt = prompt,
                                    conversationHistory = conversationHistory,
                                    onChunk = onChunk,
                                    retry404 = true,
                                    // Exclude the model that just returned 404 to ensure a different one is chosen next time
                                    excludeModelId = modelToUse,
                                )
                            } else {
                                "MODEL_NOT_FOUND|No endpoints found for model: $modelToUse"
                            }
                            // Model not found - try adding :free postfix
                            // val modelToUse = when {
                            //     selected_model.isNotEmpty() -> selected_model
                            //     else -> "anthropic/claude-3-5-sonnet-20241022"
                            // }
                            // If model doesn't have :free, add it to exception list and retry
                            // if (!modelToUse.endsWith(":free", ignoreCase = true)) {
                            //     val fixedModel = handle404Error(modelToUse)
                            //     "MODEL_404_RETRY|Model not found. Retrying with corrected ID: $fixedModel"
                            // } else {
                            //     "MODEL_NOT_FOUND|Model not available: $modelToUse"
                            // }
                        }

                        408 -> "REQUEST_TIMEOUT|Your request timed out. Try again"
                        // Error occurred
                        502 -> "MODEL_DOWN|Model is currently unavailable or returned invalid response"
                        503 -> "NO_PROVIDER|No available model provider meets your requirements"
                        else -> "HTTP_ERROR|Error ${e.code()}: ${e.message()}"
                    }
                }

                e is SocketTimeoutException -> "TIMEOUT|Request timed out. Check your connection"
                e is UnknownHostException -> "NO_INTERNET|No internet connection available"
                e is ConnectException -> "CONNECTION_FAILED|Could not connect to server"
                e is IOException -> "NETWORK_ERROR|Network error: ${e.message}"
                else -> "UNKNOWN_ERROR|${e.message ?: "An unexpected error occurred"}"
            }
            throw Exception(errorMessage)
        }
    }

    /**
     * Handle 404 model not found error
     * Automatically adds model to exception list with ":free" postfix
     */
    private suspend fun handle404Error(modelId: String): String {
        return withContext(Dispatchers.IO) {
            try {
                // Check if model already has :free postfix
                if (modelId.endsWith(":free", ignoreCase = true)) {
                    return@withContext modelId
                }

                // Add :free postfix to model ID
                val modelWithFree = "$modelId:free"

                // Get model name from available models list
                val modelName = try {
                    val allModels = fetchAvailableModels()
                    val foundModel = allModels.find { it.id == modelWithFree || it.id == modelId }
                    (foundModel?.name ?: modelId.substringAfterLast("/").replace("-", " "))
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                } catch (e: Exception) {
                    modelId.substringAfterLast("/").replace("-", " ")
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }

                // Add to Firebase exception list with model name (this will await and save to Firestore)
                FirebaseConfigManager.addExceptionModel(modelWithFree, modelName)

                // Update selected model
                selected_model = modelWithFree

                return@withContext modelWithFree
            } catch (e: Exception) {
                return@withContext modelId
            }
        }
    }
}

