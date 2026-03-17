package com.gestureai.myandroidapp.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.gestureai.myandroidapp.database.AppDatabase
import com.gestureai.myandroidapp.database.entities.SwipeDirection
import com.gestureai.myandroidapp.tracking.AppDetector
import com.gestureai.myandroidapp.tracking.AppDetector.ButtonSignal
import com.gestureai.myandroidapp.tracking.SessionManager

/**
 * Passive screen observer for dating apps.
 *
 * What it captures (zero interaction with the apps, read-only):
 *   • Profile view duration — how long you look before deciding
 *   • Swipe direction — LEFT / RIGHT / SUPER_LIKE
 *   • Scroll behavior — how much you read/browse before swiping
 *   • Match detection — "It's a Match!" overlays
 *   • Conversation events — open, messages sent, messages received, close
 *   • Time of day + day of week on every event
 *
 * Nothing is sent anywhere. Everything goes to a local SQLite DB.
 *
 * To enable: Settings → Accessibility → SwipeTracker → Toggle ON
 */
class SwipeTrackerService : AccessibilityService() {

    companion object {
        private const val TAG = "SwipeTracker"
    }

    private lateinit var session: SessionManager
    private var currentApp: String? = null
    private var inConversation: Boolean = false

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()

        session = SessionManager(AppDatabase.getInstance(applicationContext))

        // Dynamically configure which events and packages to listen to.
        // This is belt-and-suspenders — the XML config also sets these, but
        // doing it here ensures we survive config changes at runtime.
        serviceInfo = serviceInfo.also { info ->
            info.eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED

            info.packageNames = AppDetector.SUPPORTED_PACKAGES.toTypedArray()
            info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                         AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            info.notificationTimeout = 100
        }

        Log.i(TAG, "SwipeTracker connected — watching: ${AppDetector.SUPPORTED_PACKAGES}")
    }

    override fun onInterrupt() {
        Log.w(TAG, "SwipeTracker interrupted")
    }

    // ── Event dispatch ────────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (!AppDetector.isSupported(pkg)) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED    -> onWindowStateChanged(pkg, event)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED  -> onContentChanged(pkg, event)
            AccessibilityEvent.TYPE_VIEW_CLICKED            -> onViewClicked(pkg, event)
            AccessibilityEvent.TYPE_VIEW_SCROLLED           -> onViewScrolled(pkg, event)
        }
    }

    // ── Window state changes ──────────────────────────────────────────────────

    private fun onWindowStateChanged(pkg: String, event: AccessibilityEvent) {
        val className = event.className?.toString() ?: ""
        val text = collectEventText(event)

        Log.v(TAG, "[${AppDetector.displayName(pkg)}] window → $className")

        // Check for match overlay first — highest priority
        if (AppDetector.isMatchScreen(pkg, text)) {
            Log.d(TAG, "[${AppDetector.displayName(pkg)}] MATCH detected")
            session.onMatch(pkg, textSnippet = text.take(80))
            return
        }

        val wasInConversation = inConversation
        inConversation = AppDetector.isConversationScreen(pkg, className)

        when {
            inConversation && !wasInConversation -> {
                // Entered a conversation
                session.onConversationOpened(pkg)
            }
            !inConversation && wasInConversation -> {
                // Left a conversation
                session.onConversationClosed(pkg)
            }
            !inConversation -> {
                // New profile card appeared — start a view session.
                // We treat every non-conversation window change as a new profile.
                if (pkg != currentApp || currentApp == null) {
                    currentApp = pkg
                }
                session.onProfileAppeared(pkg)
            }
        }
    }

    // ── Content changes ───────────────────────────────────────────────────────

    private fun onContentChanged(pkg: String, event: AccessibilityEvent) {
        // Watch for incoming messages inside conversations
        if (!inConversation) return

        // Heuristic: if content changed in a conversation screen and we didn't
        // just send a message, it's likely a received message.
        // This is imperfect — will over-fire — but gives us a signal.
        val text = collectEventText(event)
        if (text.isNotBlank()) {
            session.onMessageReceived(pkg)
        }
    }

    // ── Button clicks ─────────────────────────────────────────────────────────

    private fun onViewClicked(pkg: String, event: AccessibilityEvent) {
        val label = buildClickLabel(event)
        if (label.isBlank()) return

        when (AppDetector.classifyButtonClick(pkg, label)) {
            ButtonSignal.LIKE        -> {
                Log.d(TAG, "[${AppDetector.displayName(pkg)}] RIGHT swipe")
                session.onSwipe(pkg, SwipeDirection.RIGHT)
            }
            ButtonSignal.DISLIKE     -> {
                Log.d(TAG, "[${AppDetector.displayName(pkg)}] LEFT swipe")
                session.onSwipe(pkg, SwipeDirection.LEFT)
            }
            ButtonSignal.SUPER_LIKE  -> {
                Log.d(TAG, "[${AppDetector.displayName(pkg)}] SUPER LIKE")
                session.onSwipe(pkg, SwipeDirection.SUPER_LIKE)
            }
            ButtonSignal.MESSAGE_SEND -> {
                Log.d(TAG, "[${AppDetector.displayName(pkg)}] message sent")
                session.onMessageSent(pkg)
            }
            ButtonSignal.NONE -> { /* unrelated click — ignore */ }
        }
    }

    // ── Scroll events ─────────────────────────────────────────────────────────

    private fun onViewScrolled(pkg: String, event: AccessibilityEvent) {
        if (inConversation) return // don't track scroll inside conversations

        // fromIndex/toIndex encode the scroll position.
        // A larger toIndex than fromIndex = scrolled down (user reading more).
        val from = event.fromIndex
        val to   = event.toIndex
        val dir  = if (to >= from) SessionManager.ScrollDirection.DOWN
                   else            SessionManager.ScrollDirection.UP
        session.onScroll(dir)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Gather all text from an event's text list + source node label */
    private fun collectEventText(event: AccessibilityEvent): String {
        val parts = mutableListOf<String>()
        event.text?.forEach { if (!it.isNullOrBlank()) parts.add(it.toString()) }
        event.contentDescription?.let { if (it.isNotBlank()) parts.add(it.toString()) }
        return parts.joinToString(" ")
    }

    /** Build a label string from a click event to classify against signal maps */
    private fun buildClickLabel(event: AccessibilityEvent): String {
        val parts = mutableListOf<String>()
        event.contentDescription?.let { parts.add(it.toString()) }
        event.text?.forEach { parts.add(it.toString()) }

        // Also walk source node for content description (some apps only set it there)
        try {
            event.source?.let { node ->
                node.contentDescription?.let { parts.add(it.toString()) }
                node.text?.let { parts.add(it.toString()) }
                // Check parent for button labels
                node.parent?.let { parent ->
                    parent.contentDescription?.let { parts.add(it.toString()) }
                }
                node.recycle()
            }
        } catch (_: Exception) { /* node may have been recycled already */ }

        return parts.filter { it.isNotBlank() }.joinToString(" ")
    }
}
