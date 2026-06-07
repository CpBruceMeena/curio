package com.curio.app.data.local

import android.content.Context
import android.content.SharedPreferences

class PreferencesHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var selectedCategories: Set<String>
        get() = prefs.getStringSet(KEY_SELECTED_CATEGORIES, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(KEY_SELECTED_CATEGORIES, value).apply()

    var hasCompletedOnboarding: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "curio_prefs"
        private const val KEY_SELECTED_CATEGORIES = "selected_categories"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"

        @Volatile
        private var instance: PreferencesHelper? = null

        fun getInstance(context: Context): PreferencesHelper {
            return instance ?: synchronized(this) {
                instance ?: PreferencesHelper(context.applicationContext).also { instance = it }
            }
        }
    }
}
