package com.yahyaoui.prayermode

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.util.Log
import java.util.*

object LocaleHelper {
    private const val TAG = "LocaleHelper"

    fun setLocale(context: Context, localeTag: String): Context {
        if (BuildConfig.DEBUG) Log.d(TAG, "Attempting to set app locale to: $localeTag")
        val locale = Locale.forLanguageTag(localeTag)
        Locale.setDefault(locale)

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        val newContext = context.createConfigurationContext(config)
        if (BuildConfig.DEBUG) Log.d(TAG, "Actual locale applied to new context: $newContext.resources.configuration.locales[0]")
        return newContext
    }

    fun getPersistedLocale(): String {
        val systemLocale: Locale = Resources.getSystem().configuration.locales[0]
        val localeTag = systemLocale.toLanguageTag()
        if (BuildConfig.DEBUG) Log.d(TAG, "System locale BCP 47 tag: $localeTag")
        val finalLocaleTag = when (localeTag) {
            "in" -> "id"
            else -> localeTag
        }
        if (BuildConfig.DEBUG) Log.d(TAG, "Mapped locale tag for resources: $finalLocaleTag")
        return finalLocaleTag
    }
}