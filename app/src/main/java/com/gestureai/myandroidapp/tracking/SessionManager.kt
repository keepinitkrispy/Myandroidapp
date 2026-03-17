package com.gestureai.myandroidapp.tracking

import com.gestureai.myandroidapp.database.AppDatabase
import com.gestureai.myandroidapp.database.entities.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Stateful session tracker sitting between the AccessibilityService and the DB.
 *
 * Lifecycle:
 *   onProfileAppeared()  → opens a ProfileView row, starts timing
 *   onScroll()           → increments scroll counters on the open session
 *   onSwipe()            → closes the ProfileView, writes a SwipeEvent
 *   onMatch()            → writes MatchEvent, back-fills SwipeEvent.resultedInMatch
 *   onConversationEvent()→ writes ConversationEvent
 */
class SessionManager(private val db: AppDatabase) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── In-flight session state ───────────────────────────────────────────────

    private var openProfileViewId: Long = -1L
    private var profileViewStart: Long = 0L
    private var scrollDown: Int = 0
    private var scrollUp: Int = 0

    // For conversation tracking
    private var conversationMatchId: Long? = null
    private var conversationOpenedAt: Long = 0L
    private var lastMessageSentAt: Long? = null

    // ── Profile view ──────────────────────────────────────────────────────────

    fun onProfileAppeared(appPackage: String) {
        // If a session was open without a swipe (app backgrounded etc.), close it
        if (openProfileViewId != -1L) {
            closeOpenSession(appPackage, swipedAway = false)
        }

        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply { timeInMillis = now }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val dow = ((cal.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7) + 1 // 1=Mon

        profileViewStart = now
        scrollDown = 0
        scrollUp = 0

        scope.launch {
            openProfileViewId = db.profileViewDao().insert(
                ProfileView(
                    appPackage = appPackage,
                    viewStartMs = now,
                    timeOfDayHour = hour,
                    dayOfWeek = dow
                )
            )
        }
    }

    fun onScroll(direction: ScrollDirection) {
        when (direction) {
            ScrollDirection.DOWN -> scrollDown++
            ScrollDirection.UP   -> scrollUp++
        }
    }

    enum class ScrollDirection { UP, DOWN }

    // ── Swipe ─────────────────────────────────────────────────────────────────

    fun onSwipe(appPackage: String, direction: SwipeDirection, detectionMethod: String = "button_click") {
        val now = System.currentTimeMillis()
        val viewId = openProfileViewId.takeIf { it != -1L }
        val duration = if (profileViewStart > 0) now - profileViewStart else null

        // Close the profile view
        scope.launch {
            if (viewId != null) {
                val view = db.profileViewDao().getById(viewId)
                if (view != null) {
                    db.profileViewDao().update(
                        view.copy(
                            viewEndMs = now,
                            durationMs = duration,
                            scrollDownCount = scrollDown,
                            scrollUpCount = scrollUp,
                            scrollTotalCount = scrollDown + scrollUp
                        )
                    )
                }
            }

            db.swipeEventDao().insert(
                SwipeEvent(
                    profileViewId = viewId,
                    appPackage = appPackage,
                    timestampMs = now,
                    direction = direction,
                    detectionMethod = detectionMethod
                )
            )
        }

        // Reset session
        openProfileViewId = -1L
        profileViewStart = 0L
        scrollDown = 0
        scrollUp = 0
    }

    private fun closeOpenSession(appPackage: String, swipedAway: Boolean) {
        if (!swipedAway) {
            val now = System.currentTimeMillis()
            val viewId = openProfileViewId
            val duration = if (profileViewStart > 0) now - profileViewStart else null
            scope.launch {
                val view = db.profileViewDao().getById(viewId) ?: return@launch
                db.profileViewDao().update(
                    view.copy(
                        viewEndMs = now,
                        durationMs = duration,
                        scrollDownCount = scrollDown,
                        scrollUpCount = scrollUp,
                        scrollTotalCount = scrollDown + scrollUp
                    )
                )
            }
        }
        openProfileViewId = -1L
        profileViewStart = 0L
    }

    // ── Match ─────────────────────────────────────────────────────────────────

    fun onMatch(appPackage: String, textSnippet: String? = null) {
        val now = System.currentTimeMillis()
        scope.launch {
            // Find the most recent right-swipe to link this match to
            val lastSwipe = db.swipeEventDao().lastRightSwipe(appPackage)

            val matchId = db.matchEventDao().insert(
                MatchEvent(
                    swipeEventId = lastSwipe?.id,
                    appPackage = appPackage,
                    timestampMs = now,
                    matchTextSnippet = textSnippet
                )
            )

            // Back-fill the swipe event
            lastSwipe?.let { db.swipeEventDao().markAsMatch(it.id) }

            conversationMatchId = matchId
        }
    }

    // ── Conversation ──────────────────────────────────────────────────────────

    fun onConversationOpened(appPackage: String) {
        conversationOpenedAt = System.currentTimeMillis()
        lastMessageSentAt = null
        scope.launch {
            db.conversationEventDao().insert(
                ConversationEvent(
                    matchEventId = conversationMatchId,
                    appPackage = appPackage,
                    timestampMs = conversationOpenedAt,
                    eventType = ConversationEventType.MATCH_OPENED
                )
            )
        }
    }

    fun onMessageSent(appPackage: String) {
        val now = System.currentTimeMillis()
        lastMessageSentAt = now
        scope.launch {
            db.conversationEventDao().insert(
                ConversationEvent(
                    matchEventId = conversationMatchId,
                    appPackage = appPackage,
                    timestampMs = now,
                    eventType = ConversationEventType.MESSAGE_SENT
                )
            )
        }
    }

    fun onMessageReceived(appPackage: String) {
        val now = System.currentTimeMillis()
        val responseTime = lastMessageSentAt?.let { now - it }
        scope.launch {
            db.conversationEventDao().insert(
                ConversationEvent(
                    matchEventId = conversationMatchId,
                    appPackage = appPackage,
                    timestampMs = now,
                    eventType = ConversationEventType.MESSAGE_RECEIVED,
                    responseTimeMs = responseTime
                )
            )
        }
    }

    fun onConversationClosed(appPackage: String) {
        val now = System.currentTimeMillis()
        val duration = if (conversationOpenedAt > 0) now - conversationOpenedAt else null
        scope.launch {
            db.conversationEventDao().insert(
                ConversationEvent(
                    matchEventId = conversationMatchId,
                    appPackage = appPackage,
                    timestampMs = now,
                    eventType = ConversationEventType.CONVERSATION_CLOSED,
                    sessionDurationMs = duration
                )
            )
        }
        conversationMatchId = null
        conversationOpenedAt = 0L
        lastMessageSentAt = null
    }
}
