package com.example.mark_vii_demo.utils

import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import java.util.Locale

fun getPreferredLocale(): Locale {
    // If app supports in-app language switching(AppCompatDelegate.setApplicationLocales)
    // Use the app's language first; otherwise fall back to the system language
    val appLocales = AppCompatDelegate.getApplicationLocales()
    val appLocale = appLocales.takeIf { !it.isEmpty }?.get(0)
    return appLocale ?: run {
        // System locale(Android 13+ has LocaleManager, but using a general approach here)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // In multilingual setups this is still the currently preferred locale
            Locale.getDefault()
        } else {
            @Suppress("DEPRECATION")
            Locale.getDefault()
        }
    }
}

fun Locale.toRecognizerTag(): String {
    // Prefer BCP-47 format(e.g., zh-Hant-TW / en-US)
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        this.toLanguageTag()
    } else {
        // Legacy fallback: language-country
        if (this.country.isNullOrEmpty()) this.language else "${this.language}-${this.country}"
    }
}