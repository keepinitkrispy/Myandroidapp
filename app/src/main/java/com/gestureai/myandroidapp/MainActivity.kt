package com.gestureai.myandroidapp

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.gestureai.myandroidapp.accessibility.SwipeTrackerService
import com.gestureai.myandroidapp.database.AppDatabase
import com.gestureai.myandroidapp.tracking.AppDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvStats: TextView
    private lateinit var btnOpenSettings: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tvStatus = findViewById(R.id.tvStatus)
        tvStats = findViewById(R.id.tvStats)
        btnOpenSettings = findViewById(R.id.btnOpenSettings)

        btnOpenSettings.setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatusBanner()
        loadStats()
    }

    private fun updateStatusBanner() {
        val active = isAccessibilityServiceEnabled()
        tvStatus.text = if (active) getString(R.string.status_active)
                        else        getString(R.string.status_inactive)
        tvStatus.setTextColor(
            if (active) getColor(android.R.color.holo_green_dark)
            else        getColor(android.R.color.holo_red_dark)
        )
        btnOpenSettings.visibility = if (active) View.GONE else View.VISIBLE
    }

    private fun loadStats() {
        val db = AppDatabase.getInstance(this)
        lifecycleScope.launch {
            val sb = StringBuilder()
            withContext(Dispatchers.IO) {
                val totalViews  = db.profileViewDao().totalCount()
                val totalSwipes = db.swipeEventDao().countByDirection(
                    com.gestureai.myandroidapp.database.entities.SwipeDirection.RIGHT
                ) + db.swipeEventDao().countByDirection(
                    com.gestureai.myandroidapp.database.entities.SwipeDirection.LEFT
                )
                val totalRight  = db.swipeEventDao().totalRightSwipes()
                val totalMatch  = db.swipeEventDao().totalMatches()

                sb.appendLine("Profiles viewed  : $totalViews")
                sb.appendLine("Total swipes     : $totalSwipes")
                sb.appendLine("Right swipes     : $totalRight")
                sb.appendLine("Matches          : $totalMatch")

                for (pkg in AppDetector.SUPPORTED_PACKAGES) {
                    val count = db.profileViewDao().countByApp(pkg)
                    if (count > 0) {
                        val name = AppDetector.displayName(pkg)
                        val rate = db.swipeEventDao().matchRate(pkg)
                        val rateStr = rate?.let { "%.1f%%".format(it * 100) } ?: "n/a"
                        val avgMs = db.profileViewDao().avgDurationMs(pkg)
                        val avgSec = avgMs?.let { "%.1fs".format(it / 1000.0) } ?: "n/a"
                        sb.appendLine()
                        sb.appendLine("[$name]")
                        sb.appendLine("  views      : $count")
                        sb.appendLine("  match rate : $rateStr")
                        sb.appendLine("  avg look   : $avgSec")
                    }
                }
            }
            tvStats.text = sb.toString().trimEnd()
        }
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
