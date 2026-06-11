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

    var isDarkTheme: Boolean
        get() = prefs.getBoolean(KEY_DARK_THEME, true)
        set(value) = prefs.edit().putBoolean(KEY_DARK_THEME, value).apply()

    val bookmarkedContentIds: Set<Long>
        get() = prefs.getStringSet(KEY_BOOKMARKS, emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet() ?: emptySet()

    fun toggleBookmark(contentId: Long): Boolean {
        val current = bookmarkedContentIds.toMutableSet()
        return if (current.contains(contentId)) {
            current.remove(contentId)
            saveBookmarks(current)
            false // removed bookmark
        } else {
            current.add(contentId)
            saveBookmarks(current)
            true // added bookmark
        }
    }

    fun isBookmarked(contentId: Long): Boolean {
        return bookmarkedContentIds.contains(contentId)
    }

    private fun saveBookmarks(ids: Set<Long>) {
        prefs.edit().putStringSet(KEY_BOOKMARKS, ids.map { it.toString() }.toSet()).apply()
    }

    /**
     * A persistent unique device identifier.
     *
     * Generated once via [UUID.randomUUID] on first access and stored in
     * SharedPreferences. Survives app restarts and is used as [device_id]
     * for profile and device-info API calls.
     */
    val deviceUuid: String
        get() {
            val stored = prefs.getString(KEY_DEVICE_UUID, null)
            if (stored != null) return stored
            val uuid = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_UUID, uuid).apply()
            return uuid
        }

    /** Whether device info has already been submitted to the backend. */
    var deviceInfoSubmitted: Boolean
        get() = prefs.getBoolean(KEY_DEVICE_INFO_SUBMITTED, false)
        set(value) = prefs.edit().putBoolean(KEY_DEVICE_INFO_SUBMITTED, value).apply()

    fun clear() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val PREFS_NAME = "curio_prefs"
        private const val KEY_SELECTED_CATEGORIES = "selected_categories"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_DARK_THEME = "dark_theme"
        private const val KEY_BOOKMARKS = "bookmarked_content_ids"
        private const val KEY_DEVICE_UUID = "device_uuid"
        private const val KEY_DEVICE_INFO_SUBMITTED = "device_info_submitted"

        @Volatile
        private var instance: PreferencesHelper? = null

        fun getInstance(context: Context): PreferencesHelper {
            return instance ?: synchronized(this) {
                instance ?: PreferencesHelper(context.applicationContext).also { instance = it }
            }
        }
    }
}
