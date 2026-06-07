package com.curio.app

import android.app.Application
import com.curio.app.data.local.PreferencesHelper

class CurioApp : Application() {
    lateinit var prefs: PreferencesHelper
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = PreferencesHelper.getInstance(this)
    }

    companion object {
        lateinit var instance: CurioApp
            private set
    }
}
