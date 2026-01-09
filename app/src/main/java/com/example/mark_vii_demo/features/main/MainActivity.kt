package com.example.mark_vii_demo.features.main

import android.content.Intent
import android.graphics.Color
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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.example.mark_vii_demo.features.chat.ChatScreen
import com.example.mark_vii_demo.features.main.components.DrawerContent
import com.example.mark_vii_demo.features.main.components.InfoSetting
import com.example.mark_vii_demo.R
import com.example.mark_vii_demo.core.data.AppTheme
import com.example.mark_vii_demo.core.data.AuthManager
import com.example.mark_vii_demo.core.data.ChatHistoryManager
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

    override fun onActivityResult(
        requestCode: Int, resultCode: Int, data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AuthManager.RC_SIGN_IN) {
            lifecycleScope.launch {
                val result = AuthManager.handleSignInResult(data)
                result.onSuccess {
                    // Sign-in successful, state will update automatically
                    isSigningInState.value = false
                    runOnUiThread {
                        Toast.makeText(this@MainActivity, "Sign in successful!", Toast.LENGTH_SHORT)
                            .show()
                    }
                }.onFailure { error ->
                    // Sign-in failed, reset loading state
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
//        Thread.sleep(1000) // splash screen delay
        installSplashScreen()  // splash screen ui

        // Initialize ChatHistoryManager
        ChatHistoryManager.init(applicationContext)

        // Initialize ThemePreferences
        ThemePreferences.init(applicationContext)

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsInitialized = true
                // Language will be set dynamically based on detected content
            }
        }

        setContent {
            // Observe theme changes
            val currentTheme by ThemePreferences.currentTheme.collectAsState()
            val darkTheme = when (currentTheme) {
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
                AppTheme.SYSTEM_DEFAULT -> isSystemInDarkTheme()
            }

            // Set window colors based on theme
            val backgroundColor =
                if (darkTheme) Color.BLACK else Color.WHITE
            val statusBarColor =
                if (darkTheme) Color.parseColor("#1A1A2E") else Color.parseColor(
                    "#F5F5F5"
                )

            SideEffect {
                window.decorView.setBackgroundColor(backgroundColor)
                window.statusBarColor = statusBarColor
                window.navigationBarColor = backgroundColor
            }

            MarkVIITheme(darkTheme = darkTheme) {
                var opentimes by remember { mutableIntStateOf(0) }
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.Companion.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                )

                {
//                    for switching between from home screen to infoTab
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = "home", builder = {
                        composable("home") {
                            opentimes++

                            // ViewModel needs to be at this scope to be accessible by both topBar and content
                            val chaViewModel = viewModel<ChatViewModel>()
                            val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
                            val coroutineScope = rememberCoroutineScope()
                            var showSettings by remember { mutableStateOf(false) } // Settings screen state
                            val isSigningIn by isSigningInState.collectAsState() // Sign-in loading state from Activity
                            val chatState by chaViewModel.chatState.collectAsState()
                            val appColors = LocalAppColors.current // Get theme colors

                            Box(modifier = Modifier.Companion.fillMaxSize()) {
                                ModalNavigationDrawer(
                                    drawerState = drawerState, drawerContent = {
                                        ModalDrawerSheet {
                                            DrawerContent(
                                                chatViewModel = chaViewModel,
                                                onDismiss = {
                                                    coroutineScope.launch {
                                                        drawerState.close()
                                                    }
                                                },
                                                onSettingsClick = {
                                                    coroutineScope.launch {
                                                        drawerState.close()
                                                    }
                                                    showSettings = true
                                                },
                                                onSigningInChanged = { signing ->
                                                    isSigningInState.value = signing
                                                })
                                        }
                                    }, gesturesEnabled = true
                                ) {
                                    Scaffold(
//                                top bar items
                                        topBar = {
                                            MainTopBar(
                                                drawerState = drawerState,
                                                navController = navController,
                                                title = "ChatGPT",
                                                actionText = "登入",
                                            )
                                        },
                                        // Show welcome guide once when app opens (no API call)
                                        bottomBar = {
                                            if (opentimes == 1) {
                                                chaViewModel.showWelcomeGuide()
                                            }
                                        }) {
                                        ChatScreen(
                                            paddingValues = it,
                                            chatViewModel = chaViewModel,
                                            uriState = uriState,
                                            voiceInputState = voiceInputState,
                                            imagePicker = imagePicker,
                                            voiceInputLauncher = voiceInputLauncher,
                                            isTtsInitialized = isTtsInitialized,
                                            textToSpeech = textToSpeech,
                                            onSpeakText = { text -> speakText(text) })  // starting chat screen ui
                                    }
                                } // End ModalNavigationDrawer

                                // Settings screen overlay
                                if (showSettings) {
                                    // Handle back button when settings is open
                                    BackHandler {
                                        showSettings = false
                                    }

                                    SettingsScreen(
                                        onBackClick = { showSettings = false },
                                        onSignOut = {
                                            chaViewModel.onEvent(ChatUiEvent.SignOut)
                                            showSettings = false
                                        },
                                        onThemeChanged = { /* Theme change is handled via StateFlow */ })
                                }

                                // Loading overlay during sign-in - covers entire app
                                if (isSigningIn) {
                                    Box(
                                        modifier = Modifier.Companion
                                            .fillMaxSize()
                                            .background(
                                                androidx.compose.ui.graphics.Color.Companion.Black.copy(
                                                    alpha = 0.7f
                                                )
                                            )
                                            .clickable(enabled = false) { },
                                        contentAlignment = Alignment.Companion.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.Companion.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.Companion.size(56.dp),
                                                color = appColors.accent,
                                                strokeWidth = 5.dp
                                            )
                                            Text(
                                                text = "Signing in with Google...",
                                                fontSize = 18.sp,
                                                color = MaterialTheme.colorScheme.onSurface,
                                                fontWeight = FontWeight.Companion.Medium
                                            )
                                        }
                                    }
                                }

                            } // Box

                        }
                        composable("info_screen") {
                            InfoSetting() // starting infoTab ui (About section)
                        }

                    })


                }

            }
        }
    }

    override fun onDestroy() {
        // Shutdown TextToSpeech to free resources
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onDestroy()
    }

    private fun speakText(text: String) {
        if (textToSpeech == null) return

        // Detect language using MLKit
        val languageIdentifier = LanguageIdentification.getClient(
            LanguageIdentificationOptions.Builder().setConfidenceThreshold(0.34f).build()
        )

        languageIdentifier.identifyLanguage(text).addOnSuccessListener { languageCode ->
            if (languageCode != "und") {
                // Map MLKit language codes to Locale
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

                // Set language if available
                val result = textToSpeech?.setLanguage(locale)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    // Fallback to English if language not supported
                    textToSpeech?.setLanguage(Locale.US)
                }
            }

            // Speak the text after setting language
            speakTextWithLanguage(text)
        }.addOnFailureListener {
            // If detection fails, use English as fallback
            textToSpeech?.setLanguage(Locale.US)
            speakTextWithLanguage(text)
        }
    }

    private fun speakTextWithLanguage(text: String) {
        if (textToSpeech == null) return

        val maxLength = TextToSpeech.getMaxSpeechInputLength() - 100 // Buffer

        if (text.length <= maxLength) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
            return
        }

        // Flush existing
        textToSpeech?.speak("", TextToSpeech.QUEUE_FLUSH, null, null)

        var remainingText = text
        while (remainingText.isNotEmpty()) {
            val chunk = if (remainingText.length > maxLength) {
                // Find a good break point
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