package com.osint.situationroom

import android.app.Application
import org.osmdroid.config.Configuration

class SituationRoomApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // OSMdroid requires user agent set before any map usage
        Configuration.getInstance().userAgentValue = "OSINT-SituationRoom/1.0 Android"
        Configuration.getInstance().load(this, getSharedPreferences("osmdroid", MODE_PRIVATE))
    }
}
