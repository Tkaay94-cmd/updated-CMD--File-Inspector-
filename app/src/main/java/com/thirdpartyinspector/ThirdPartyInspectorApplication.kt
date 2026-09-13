package com.thirdpartyinspector

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class ThirdPartyInspectorApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize logging, analytics (NONE by default), or other on-start components here.
    }
}
