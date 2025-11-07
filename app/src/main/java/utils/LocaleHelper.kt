package com.example.tpfinal_pablovelazquez.utils

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import java.util.Locale

object LocaleHelper {

    fun setLocale(context: Context, languageCode: String) {
        saveLanguagePreference(context, languageCode)
        updateResources(context, languageCode)
    }

    private fun updateResources(context: Context, languageCode: String) {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)

        context.createConfigurationContext(config)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun getLanguageCode(context: Context): String {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        return prefs.getString("language_code", "es") ?: "es"
    }

    private fun saveLanguagePreference(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        prefs.edit().putString("language_code", languageCode).apply()
    }

    fun getLanguageName(context: Context, languageCode: String): String {
        return when (languageCode) {
            "es" -> "Español"
            "en" -> "English"
            else -> "Español"
        }
    }
}