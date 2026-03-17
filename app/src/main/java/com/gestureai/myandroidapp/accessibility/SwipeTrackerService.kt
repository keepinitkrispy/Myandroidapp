package com.gestureai.myandroidapp.accessibility

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.gestureai.myandroidapp.database.AppDatabase
import com.gestureai.myandroidapp.database.entities.SwipeDirection
import com.gestureai.myandroidapp.tracking.AppDetector
import com.gestureai.myandroidapp.tracking.AppDetector.ButtonSignal
import com.gestureai.myandroidapp.tracking.SessionManager

/**
 * Passive screen observer for dating apps.
 *
 * Captures (read-only — zero interaction with the monitored apps):
 *   • Profile view duration     — how long you look before deciding
 *   • Swipe direction           — LEFT / RIGHT / SUPER_LIKE
 *   • Aborted swipes            — started a swipe then reversed (hesitation signal)
 *   • Scroll behavior           — how much you read/browse before swiping
 *   • Match detection           — "It's a Match!" overlays
 *   • Conversation events       — open, message sent, message received, message
 *                                 read (with read+reply latency), close
 *   • Time of day + day of week on every event
 *
 * Everything stays in local SQLite. Nothing is sent anywhere.
 *
 * To enable: Settings → Accessibility → SwipeTracker → Toggle ON
 */
class SwipeTrackerService : AccessibilityService() {

    companion object {
        private const val TAG = "SwipeTracker"
    }

    private lateinit var session: SessionManager
    private lateinit var detector: AppDetector
    private var currentApp: String? = null
    private var inConversation: Boolean = false

    // Gesture state for aborted-swipe detection.
    // Dating apps (Tinder especially) fire TYPE_VIEW_SCROLLED with horizontal
    // displacement as the card is dragged. We track direction from that.
    private var gestureDir: SwipeDirection? = null
    private var gesturePeakProgress: Float = 0f
    private var gestureScrollEvents: Int = 0
    private val GESTURE_THRESHOLD_EVENTS = 3   // min scroll events before we count as "started"
    private val ABORT_PROGRESS_THRESHOLD = 0.15f // must have traveled at least 15% to count

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onServiceConnected() {
        super.onServiceConnected()

        val db = AppDatabase.getInstance(applicationContext)
        session  = SessionManager(db)
        detector = AppDetector.getInstance(applicationContext)

        serviceInfo = serviceInfo.also { info ->
            info.eventTypes =
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_VIEW_CLICKED or
                AccessibilityEvent.TYPE_VIEW_SCROLLED or
                AccessibilityEvent.TYPE_VIEW_FOCUSED

            info.packageNames = detector.supportedPackages.toTypedArray()
            info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS or
                         AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
            info.notificationTimeout = 100
        }

        Log.i(TAG, "SwipeTracker connected — watching: ${detector.supportedPackages}")
    }

    override fun onInterrupt() {
        Log.w(TAG, "SwipeTracker interrupted")
    }

    // ── Event dispatch ────────────────────────────────────────────────────────

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val pkg = event.packageName?.toString() ?: return
        if (!detector.isSupported(pkg)) return

        when (event.eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED   -> onWindowStateChanged(pkg, event)
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> onContentChanged(pkg, event)
            AccessibilityEvent.TYPE_VIEW_CLICKED           -> onViewClicked(pkg, event)
            AccessibilityEvent.TYPE_VIEW_SCROLLED          -> onViewScrolled(pkg, event)
        }
    }

    // ── Window state changes ──────────────────────────────────────────────────

    private fun onWindowStateChanged(pkg: String, event: AccessibilityEvent) {
        val className = event.className?.toString() ?: ""
        val text = collectEventText(event)

        Log.v(TAG, "[${detector.displayName(pkg)}] window → $className")

        // Check for match overlay — highest priority
        if (detector.isMatchScreen(pkg, text)) {
            Log.d(TAG, "[${detector.displayName(pkg)}] MATCH detected")
            session.onMatch(pkg, textSnippet = text.take(80))
            return
        }

        val wasInConversation = inConversation
        inConversation = detector.isConversationScreen(pkg, className)

        when {
            inConversation && !wasInConversation -> {
                session.onConversationOpened(pkg)
                // Entering the conversation = reading the received message
                session.onMessageRead(pkg)
            }
            !inConversation && wasInConversation -> {
                session.onConversationClosed(pkg)
            }
            !inConversation -> {
                if (pkg != currentApp || currentApp == null) currentApp = pkg
                session.onProfileAppeared(pkg)
                resetGestureState()
            }
        }
    }

    // ── Content changes ───────────────────────────────────────────────────────

    private fun onContentChanged(pkg: String, event: AccessibilityEvent) {
        if (!inConversation) return
        val text = collectEventText(event)
        if (text.isNotBlank()) {
            session.onMessageReceived(pkg)
        }
    }

    // ── Button clicks ─────────────────────────────────────────────────────────

    private fun onViewClicked(pkg: String, event: AccessibilityEvent) {
        val label = buildClickLabel(event)
        if (label.isBlank()) return

        when (detector.classifyButtonClick(pkg, label)) {
            ButtonSignal.LIKE -> {
                Log.d(TAG, "[${detector.displayName(pkg)}] RIGHT")
                flushGestureIfAborted(pkg, resolvedDir = SwipeDirection.RIGHT)
                session.onSwipe(pkg, SwipeDirection.RIGHT)
            }
            ButtonSignal.DISLIKE -> {
                Log.d(TAG, "[${detector.displayName(pkg)}] LEFT")
                flushGestureIfAborted(pkg, resolvedDir = SwipeDirection.LEFT)
                session.onSwipe(pkg, SwipeDirection.LEFT)
            }
            ButtonSignal.SUPER_LIKE -> {
                Log.d(TAG, "[${detector.displayName(pkg)}] SUPER LIKE")
                flushGestureIfAborted(pkg, resolvedDir = SwipeDirection.SUPER_LIKE)
                session.onSwipe(pkg, SwipeDirection.SUPER_LIKE)
            }
            ButtonSignal.MESSAGE_SEND -> {
                Log.d(TAG, "[${detector.displayName(pkg)}] message sent")
                session.onMessageSent(pkg)
            }
            ButtonSignal.MESSAGE_READ -> {
                session.onMessageRead(pkg)
            }
            ButtonSignal.NONE -> { /* unrelated click */ }
        }
    }

    // ── Scroll events (swipe gesture + aborted swipe tracking) ────────────────

    private fun onViewScrolled(pkg: String, event: AccessibilityEvent) {
        if (inConversation) return

        val scrollX = event.scrollX
        val scrollY = event.scrollY

        // Horizontal scrolling = card swipe gesture in progress
        // Vertical scrolling = browsing photos / bio
        if (kotlin.math.abs(scrollX) > kotlin.math.abs(scrollY)) {
            handleHorizontalScroll(pkg, scrollX)
        } else {
            val dir = if (scrollY >= 0) SessionManager.ScrollDirection.DOWN
                      else              SessionManager.ScrollDirection.UP
            session.onScroll(dir)
        }
    }

    /**
     * Horizontal scroll = the card drag gesture.
     * scrollDelta > 0: dragging right (toward LIKE)
     * scrollDelta < 0: dragging left (toward NOPE)
     *
     * We estimate progress as abs(delta) / screenWidth (rough, but correlates
     * with how far the card has traveled).
     */
    private fun handleHorizontalScroll(pkg: String, scrollDelta: Int) {
        val inferredDir = if (scrollDelta > 0) SwipeDirection.RIGHT else SwipeDirection.LEFT
        val screenWidth = resources.displayMetrics.widthPixels.toFloat().coerceAtLeast(1f)
        val progress = (kotlin.math.abs(scrollDelta) / screenWidth).coerceIn(0f, 1f)

        gestureScrollEvents++

        if (gestureDir == null) {
            // New gesture started
            gestureDir = inferredDir
            gesturePeakProgress = progress
            session.onGestureStart(pkg, inferredDir, progress)
        } else if (gestureDir != inferredDir) {
            // Direction reversal mid-gesture = hesitation / pullback
            if (gestureScrollEvents >= GESTURE_THRESHOLD_EVENTS
                && gesturePeakProgress >= ABORT_PROGRESS_THRESHOLD) {
                Log.d(TAG, "[${detector.displayName(pkg)}] ABORTED ${gestureDir} swipe (peak=${gesturePeakProgress})")
                session.onGestureCancelled(pkg)
            }
            gestureDir = inferredDir
            gesturePeakProgress = progress
            gestureScrollEvents = 0
            session.onGestureStart(pkg, inferredDir, progress)
        } else {
            gesturePeakProgress = maxOf(gesturePeakProgress, progress)
            session.onGestureProgress(gesturePeakProgress)
        }
    }

    /**
     * Called right before recording a confirmed swipe.
     * If the in-flight gesture direction differs from the resolved swipe,
     * the gesture was in the opposite direction and then completed via button —
     * that counts as an aborted gesture followed by a button-confirmed swipe.
     */
    private fun flushGestureIfAborted(pkg: String, resolvedDir: SwipeDirection) {
        val gDir = gestureDir ?: return
        if (gDir != resolvedDir
            && gestureScrollEvents >= GESTURE_THRESHOLD_EVENTS
            && gesturePeakProgress >= ABORT_PROGRESS_THRESHOLD) {
            session.onGestureCancelled(pkg)
        }
        resetGestureState()
    }

    private fun resetGestureState() {
        gestureDir = null
        gesturePeakProgress = 0f
        gestureScrollEvents = 0
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun collectEventText(event: AccessibilityEvent): String {
        val parts = mutableListOf<String>()
        event.text?.forEach { if (!it.isNullOrBlank()) parts.add(it.toString()) }
        event.contentDescription?.let { if (it.isNotBlank()) parts.add(it.toString()) }
        return parts.joinToString(" ")
    }

    private fun buildClickLabel(event: AccessibilityEvent): String {
        val parts = mutableListOf<String>()
        event.contentDescription?.let { parts.add(it.toString()) }
        event.text?.forEach { parts.add(it.toString()) }
        try {
            event.source?.let { node ->
                node.contentDescription?.let { parts.add(it.toString()) }
                node.text?.let { parts.add(it.toString()) }
                node.parent?.contentDescription?.let { parts.add(it.toString()) }
                node.recycle()
            }
        } catch (_: Exception) {}
        return parts.filter { it.isNotBlank() }.joinToString(" ")
    }
}
