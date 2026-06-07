package com.curio.app

import android.app.Application

class CurioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: CurioApp
            private set
    }
}
