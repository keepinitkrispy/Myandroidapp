package com.gestureai.myandroidapp

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.gestureai.myandroidapp.accessibility.SwipeTrackerService

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var btnOpenSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)

        btnOpenSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        val active = isAccessibilityServiceEnabled()
        tvStatus.text = if (active) getString(R.string.status_active)
                        else        getString(R.string.status_inactive)
        tvStatus.setTextColor(
            if (active) getColor(android.R.color.holo_green_dark)
            else        getColor(android.R.color.holo_red_dark)
        )
        btnOpenSettings.visibility = if (active) View.GONE else View.VISIBLE
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val service = "$packageName/${SwipeTrackerService::class.java.canonicalName}"
        val enabled = try {
            Settings.Secure.getInt(contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED)
        } catch (_: Exception) { 0 }
        if (enabled != 1) return false

        val services = Settings.Secure.getString(
            contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        return TextUtils.SimpleStringSplitter(':').apply { setString(services) }
            .asSequence()
            .any { it.equals(service, ignoreCase = true) }
    }
}
