package io.github.artcraftandroid.pdfcraft

import androidx.core.content.edit

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import android.os.Build
import java.util.Locale

/** Interface language only: PDF contents, engine commands and speech voices stay independent. */
object JahfanLanguage {
    @Volatile var current: String = "en"
        private set

    fun wrap(context: Context): Context {
        val saved = if (Build.VERSION.SDK_INT >= 33) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales
                ?.takeUnless { it.isEmpty }?.get(0)?.language
        } else context.getSharedPreferences("jahfan_language", Context.MODE_PRIVATE)
            .getString("language", null)
        val language = saved?.takeIf { it == "ar" || it == "en" }
            ?: if (context.resources.configuration.locales[0].language == "ar") "ar" else "en"
        current = language
        val locale = Locale.forLanguageTag(language)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }

    fun select(activity: Activity, language: String) {
        if (language !in setOf("ar", "en") || language == current) return
        if (Build.VERSION.SDK_INT >= 33) {
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                LocaleList.forLanguageTags(language)
            return
        }
        activity.getSharedPreferences("jahfan_language", Context.MODE_PRIVATE)
            .edit { putString("language", language) }
        // Recreate only the Activity. Its ViewModel keeps open documents and unsaved work.
        activity.recreate()
    }
}

/** Resolve at the point of use so changing language never changes stored identifiers. */
fun jText(english: String, arabic: String): String =
    if (JahfanLanguage.current == "ar") arabic else english
