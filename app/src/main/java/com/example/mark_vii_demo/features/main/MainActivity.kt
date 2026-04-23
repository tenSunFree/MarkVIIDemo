package com.example.mark_vii_demo.features.main

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mark_vii_demo.features.chat.ChatScreen
import com.example.mark_vii_demo.features.main.components.ApiKeySetupDialog
import com.example.mark_vii_demo.features.main.components.InfoSetting
import com.example.mark_vii_demo.core.data.AppTheme
import com.example.mark_vii_demo.core.data.AuthManager
import com.example.mark_vii_demo.core.data.ChatHistoryManager
import com.example.mark_vii_demo.core.data.GeminiClient
import com.example.mark_vii_demo.core.data.SecureUserConfigManager
import com.example.mark_vii_demo.core.data.ThemePreferences
import com.example.mark_vii_demo.features.chat.ChatUiEvent
import com.example.mark_vii_demo.features.chat.ChatViewModel
import com.example.mark_vii_demo.features.main.components.MainTopBar
import com.example.mark_vii_demo.features.setting.SettingsScreen
import com.example.mark_vii_demo.ui.theme.LocalAppColors
import com.example.mark_vii_demo.ui.theme.MarkVIITheme
import com.google.mlkit.nl.languageid.LanguageIdentification
import com.google.mlkit.nl.languageid.LanguageIdentificationOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val uriState = MutableStateFlow("")
    private val voiceInputState = MutableStateFlow("")
    private val isSigningInState = MutableStateFlow(false)

    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = false

    private val imagePicker = registerForActivityResult<PickVisualMediaRequest, Uri?>(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            uriState.update { uri.toString() }
        }
    }

    private val voiceInputLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val spokenText = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            spokenText?.let {
                if (it.isNotEmpty()) {
                    voiceInputState.update { _ -> it[0] }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == AuthManager.RC_SIGN_IN) {
            lifecycleScope.launch {
                val result = AuthManager.handleSignInResult(data)
                result.onSuccess {
                    isSigningInState.value = false
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Sign in successful!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }.onFailure { error ->
                    isSigningInState.value = false
                    runOnUiThread {
                        Toast.makeText(
                            this@MainActivity,
                            "Sign in failed: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        ChatHistoryManager.init(applicationContext)
        ThemePreferences.init(applicationContext)
        // Initialize SecureUserConfigManager (store only userName, no longer store OpenRouter key)
        SecureUserConfigManager.init(applicationContext)
        GeminiClient.updateApiKey(SecureUserConfigManager.getGeminiApiKey())
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
            }
        }
        setContent {
            val currentTheme by ThemePreferences.currentTheme.collectAsState()
            val darkTheme = when (currentTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM_DEFAULT -> isSystemInDarkTheme()
            }
            val backgroundColor = if (darkTheme) Color.Black else Color.White
            SideEffect {
                window.decorView.setBackgroundColor(backgroundColor.toArgb())
                window.statusBarColor = Color.Black.toArgb()
                window.navigationBarColor = backgroundColor.toArgb()
                WindowCompat.getInsetsController(window, window.decorView)
                    .isAppearanceLightStatusBars = !darkTheme
            }
            MarkVIITheme(darkTheme = darkTheme) {
                var opentimes by remember { mutableIntStateOf(0) }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home") {
                        composable("home") {
                            opentimes++
                            val chaViewModel = viewModel<ChatViewModel>()
                            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                            val coroutineScope = rememberCoroutineScope()
                            var showSettings by remember { mutableStateOf(false) }
                            val isSigningIn by isSigningInState.collectAsState()
                            val chatState by chaViewModel.chatState.collectAsState()
                            val userConfig by SecureUserConfigManager.config.collectAsState()
                            val appColors = LocalAppColors.current

                            Box(modifier = Modifier.fillMaxSize()) {
                                ModalNavigationDrawer(
                                    drawerState = drawerState,
                                    drawerContent = { ModalDrawerSheet { } },
                                    gesturesEnabled = true
                                ) {
                                    Scaffold(
                                        topBar = {
                                            MainTopBar(
                                                drawerState = drawerState,
                                                navController = navController,
                                                title = "ChatGPT",
                                                actionText = "登入",
                                            )
                                        },
                                        bottomBar = {
                                            if (opentimes == 1) {
                                                chaViewModel.showWelcomeGuide()
                                            }
                                        }
                                    ) {
                                        ChatScreen(
                                            paddingValues = it,
                                            chatViewModel = chaViewModel,
                                            uriState = uriState,
                                            voiceInputState = voiceInputState,
                                            imagePicker = imagePicker,
                                            voiceInputLauncher = voiceInputLauncher,
                                            isTtsInitialized = isTtsInitialized,
                                            textToSpeech = textToSpeech,
                                            onSpeakText = { text -> speakText(text) }
                                        )
                                    }
                                }

                                if (showSettings) {
                                    BackHandler { showSettings = false }
                                    SettingsScreen(
                                        onBackClick = { showSettings = false },
                                        onSignOut = {
                                            chaViewModel.onEvent(ChatUiEvent.SignOut)
                                            showSettings = false
                                        },
                                        onThemeChanged = { }
                                    )
                                }
                                if (isSigningIn) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.7f))
                                            .clickable(enabled = false) { },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(56.dp),
                                                color = appColors.accent,
                                                strokeWidth = 5.dp
                                            )
                                            Text(
                                                text = "Signing in with Google...",
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    }
                                }
                                // The settings dialog box is only displayed if userName is not set.
                                if (!userConfig.isConfigured) {
                                    ApiKeySetupDialog(
                                        initialUserName = userConfig.userName,
                                        initialGeminiApiKey = userConfig.geminiApiKey,
                                        onConfirm = { name, apiKey ->
                                            SecureUserConfigManager.saveCredentials(name, apiKey)
                                            GeminiClient.updateApiKey(apiKey)
                                        }
                                    )
                                }
                            }
                        }
                        composable("info_screen") {
                            InfoSetting()
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    private fun speakText(text: String) {
        if (textToSpeech == null) return
        val languageIdentifier = LanguageIdentification.getClient(
            LanguageIdentificationOptions.Builder().setConfidenceThreshold(0.34f).build()
        )
        languageIdentifier.identifyLanguage(text).addOnSuccessListener { languageCode ->
            if (languageCode != "und") {
                val locale = when (languageCode) {
                    "zh" -> Locale.SIMPLIFIED_CHINESE
                    "zh-Hant" -> Locale.TRADITIONAL_CHINESE
                    "ja" -> Locale.JAPANESE
                    "ko" -> Locale.KOREAN
                    "es" -> Locale("es")
                    "fr" -> Locale.FRENCH
                    "de" -> Locale.GERMAN
                    "it" -> Locale.ITALIAN
                    "pt" -> Locale("pt")
                    "ru" -> Locale("ru")
                    "ar" -> Locale("ar")
                    "hi" -> Locale("hi")
                    "en" -> Locale.US
                    else -> Locale.US
                }
                val result = textToSpeech?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    textToSpeech?.setLanguage(Locale.US)
                }
            }
            speakTextWithLanguage(text)
        }.addOnFailureListener {
            textToSpeech?.setLanguage(Locale.US)
            speakTextWithLanguage(text)
        }
    }

    private fun speakTextWithLanguage(text: String) {
        if (textToSpeech == null) return
        val maxLength = TextToSpeech.getMaxSpeechInputLength() - 100
        if (text.length <= maxLength) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            return
        }
        textToSpeech?.speak("", TextToSpeech.QUEUE_FLUSH, null, null)
        var remainingText = text
        while (remainingText.isNotEmpty()) {
            val chunk = if (remainingText.length > maxLength) {
                val splitIndex = remainingText.lastIndexOf('.', maxLength).takeIf { it > 0 }
                    ?: remainingText.lastIndexOf(' ', maxLength).takeIf { it > 0 } ?: maxLength
                remainingText.substring(0, splitIndex)
            } else {
                remainingText
            }
            textToSpeech?.speak(chunk, TextToSpeech.QUEUE_ADD, null, null)
            remainingText = if (chunk.length < remainingText.length) {
                remainingText.substring(chunk.length).trim()
            } else {
                ""
            }
        }
    }
}