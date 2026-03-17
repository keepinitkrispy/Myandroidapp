package com.gestureai.myandroidapp.tracking

import android.content.Context
import android.util.Log
import org.json.JSONObject
import java.io.File

/**
 * Loads per-app detection patterns from JSON, not hardcoded strings.
 *
 * Pattern resolution order (highest priority first):
 *   1. /Android/data/com.gestureai.myandroidapp/files/detection_patterns.json
 *      → user-editable override; survives app updates
 *   2. assets/detection_patterns.json
 *      → shipped defaults; updated with app releases
 *
 * To update patterns without rebuilding:
 *   adb push detection_patterns.json \
 *       /sdcard/Android/data/com.gestureai.myandroidapp/files/detection_patterns.json
 *
 * Then restart the accessibility service (toggle off/on in settings).
 */
class AppDetector private constructor(private val config: Map<String, AppConfig>) {

    data class AppConfig(
        val displayName: String,
        val likeSignals: List<String>,
        val dislikeSignals: List<String>,
        val superLikeSignals: List<String>,
        val matchSignals: List<String>,
        val conversationSignals: List<String>,
        val messageSendSignals: List<String>,
        val messageReadSignals: List<String>
    )

    enum class ButtonSignal { LIKE, DISLIKE, SUPER_LIKE, MESSAGE_SEND, MESSAGE_READ, NONE }

    // ── Public API ────────────────────────────────────────────────────────────

    val supportedPackages: Set<String> get() = config.keys

    fun isSupported(pkg: String) = pkg in config

    fun displayName(pkg: String) = config[pkg]?.displayName ?: pkg

    fun classifyButtonClick(pkg: String, label: String): ButtonSignal {
        val c = config[pkg] ?: return ButtonSignal.NONE
        val lower = label.lowercase()
        return when {
            c.superLikeSignals.any   { lower.contains(it.lowercase()) } -> ButtonSignal.SUPER_LIKE
            c.likeSignals.any        { lower.contains(it.lowercase()) } -> ButtonSignal.LIKE
            c.dislikeSignals.any     { lower.contains(it.lowercase()) } -> ButtonSignal.DISLIKE
            c.messageSendSignals.any { lower.contains(it.lowercase()) } -> ButtonSignal.MESSAGE_SEND
            c.messageReadSignals.any { lower.contains(it.lowercase()) } -> ButtonSignal.MESSAGE_READ
            else -> ButtonSignal.NONE
        }
    }

    fun isMatchScreen(pkg: String, text: String): Boolean {
        val lower = text.lowercase()
        return config[pkg]?.matchSignals?.any { lower.contains(it.lowercase()) } == true
    }

    fun isConversationScreen(pkg: String, className: String): Boolean {
        val lower = className.lowercase()
        return config[pkg]?.conversationSignals?.any { lower.contains(it.lowercase()) } == true
    }

    fun isMessageReadSignal(pkg: String, label: String): Boolean {
        val lower = label.lowercase()
        return config[pkg]?.messageReadSignals?.any { lower.contains(it.lowercase()) } == true
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    companion object {
        private const val TAG = "AppDetector"
        private const val FILENAME = "detection_patterns.json"

        @Volatile
        private var INSTANCE: AppDetector? = null

        fun getInstance(context: Context): AppDetector =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: load(context).also { INSTANCE = it }
            }

        /** Force reload from disk — call after the user drops an override file */
        fun reload(context: Context): AppDetector {
            INSTANCE = null
            return getInstance(context)
        }

        private fun load(context: Context): AppDetector {
            val json = loadJson(context)
            val apps = json.getJSONObject("apps")
            val version = json.optInt("_version", 0)
            Log.i(TAG, "Loaded detection_patterns.json v$version (${apps.length()} apps)")

            val config = mutableMapOf<String, AppConfig>()
            apps.keys().forEach { pkg ->
                val app = apps.getJSONObject(pkg)
                config[pkg] = AppConfig(
                    displayName       = app.optString("displayName", pkg),
                    likeSignals       = app.getStringList("like"),
                    dislikeSignals    = app.getStringList("dislike"),
                    superLikeSignals  = app.getStringList("superLike"),
                    matchSignals      = app.getStringList("match"),
                    conversationSignals = app.getStringList("conversation"),
                    messageSendSignals  = app.getStringList("messageSend"),
                    messageReadSignals  = app.getStringList("messageRead")
                )
            }
            return AppDetector(config)
        }

        private fun loadJson(context: Context): JSONObject {
            // 1. User override file in external files dir
            val override = File(context.getExternalFilesDir(null), FILENAME)
            if (override.exists()) {
                try {
                    Log.i(TAG, "Loading override from ${override.absolutePath}")
                    return JSONObject(override.readText())
                } catch (e: Exception) {
                    Log.w(TAG, "Override file parse failed, falling back to assets: ${e.message}")
                }
            }
            // 2. Bundled asset
            Log.i(TAG, "Loading bundled $FILENAME from assets")
            return context.assets.open(FILENAME).bufferedReader().use {
                JSONObject(it.readText())
            }
        }

        private fun JSONObject.getStringList(key: String): List<String> {
            val arr = optJSONArray(key) ?: return emptyList()
            return (0 until arr.length()).map { arr.getString(it) }
        }
    }
}
