package com.curio.app

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.curio.app.data.local.PreferencesHelper

class CurioApp : Application() {
    lateinit var prefs: PreferencesHelper
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = PreferencesHelper.getInstance(this)
        // Always dark — light theme will be polished later
        darkThemeEnabled = true
    }

    companion object {
        lateinit var instance: CurioApp
            private set

        /** Always dark for now — light theme will be added later */
        var darkThemeEnabled by mutableStateOf(true)
            private set
    }
}
