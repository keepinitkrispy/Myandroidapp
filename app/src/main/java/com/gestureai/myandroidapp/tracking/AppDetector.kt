package com.gestureai.myandroidapp.tracking

/**
 * Knows which accessibility events to care about for each supported dating app,
 * and what those events mean (like, dislike, super-like, match, message).
 *
 * All string matching is case-insensitive. These lists were derived from
 * accessibility tree inspection — they'll evolve as apps update.
 */
object AppDetector {

    // ── Supported apps ────────────────────────────────────────────────────────

    val SUPPORTED_PACKAGES = setOf(
        "com.tinder",
        "com.bumble.app",
        "co.hinge.app",
        "com.okcupid.okcupid",
        "com.badoo.mobile"
    )

    fun isSupported(packageName: String) = packageName in SUPPORTED_PACKAGES

    fun displayName(packageName: String) = when (packageName) {
        "com.tinder"          -> "Tinder"
        "com.bumble.app"      -> "Bumble"
        "co.hinge.app"        -> "Hinge"
        "com.okcupid.okcupid" -> "OkCupid"
        "com.badoo.mobile"    -> "Badoo"
        else                  -> packageName
    }

    // ── Signal maps ───────────────────────────────────────────────────────────
    // Content descriptions and button text that indicate a like/dislike action.
    // These are what shows up in AccessibilityNodeInfo.contentDescription / text.

    private val LIKE_SIGNALS = mapOf(
        "com.tinder"          to listOf("Like", "Like Profile"),
        "com.bumble.app"      to listOf("Yes", "Extend"),
        "co.hinge.app"        to listOf("Like", "Send a like"),
        "com.okcupid.okcupid" to listOf("Like", "Double Take"),
        "com.badoo.mobile"    to listOf("Like", "Yes")
    )

    private val DISLIKE_SIGNALS = mapOf(
        "com.tinder"          to listOf("Nope", "Pass"),
        "com.bumble.app"      to listOf("No", "Pass"),
        "co.hinge.app"        to listOf("Pass"),
        "com.okcupid.okcupid" to listOf("Pass", "No Thanks"),
        "com.badoo.mobile"    to listOf("No", "Nope")
    )

    private val SUPER_LIKE_SIGNALS = mapOf(
        "com.tinder"          to listOf("Super Like", "Super like"),
        "com.bumble.app"      to listOf("SuperSwipe"),
        "co.hinge.app"        to listOf("Rose"),
        "com.okcupid.okcupid" to listOf("Quickmatch"),
        "com.badoo.mobile"    to listOf("Super Like")
    )

    /**
     * Text snippets that appear in match overlay screens.
     * Checked against full text content of window-state-changed events.
     */
    val MATCH_SIGNALS = mapOf(
        "com.tinder"          to listOf("It's a Match", "You matched"),
        "com.bumble.app"      to listOf("You matched", "Match!"),
        "co.hinge.app"        to listOf("It's a Match", "You matched"),
        "com.okcupid.okcupid" to listOf("Mutual Like", "You both liked"),
        "com.badoo.mobile"    to listOf("You both liked", "It's a match")
    )

    /**
     * Screens/activities that represent the conversation/inbox view.
     * Matched against AccessibilityEvent.className.
     */
    val CONVERSATION_SCREEN_SIGNALS = mapOf(
        "com.tinder"          to listOf("ConversationActivity", "ChatActivity", "Messages"),
        "com.bumble.app"      to listOf("ChatActivity", "ConversationActivity"),
        "co.hinge.app"        to listOf("ConversationActivity", "ChatView"),
        "com.okcupid.okcupid" to listOf("MessageActivity", "ConversationActivity"),
        "com.badoo.mobile"    to listOf("ChatActivity", "ConversationActivity")
    )

    val MESSAGE_SENT_SIGNALS = mapOf(
        "com.tinder"          to listOf("Send", "Send message"),
        "com.bumble.app"      to listOf("Send", "Send message"),
        "co.hinge.app"        to listOf("Send"),
        "com.okcupid.okcupid" to listOf("Send"),
        "com.badoo.mobile"    to listOf("Send")
    )

    // ── Matching helpers ──────────────────────────────────────────────────────

    enum class ButtonSignal { LIKE, DISLIKE, SUPER_LIKE, MESSAGE_SEND, NONE }

    /**
     * Given the text/content-description of a clicked node, classify what
     * action just happened.
     */
    fun classifyButtonClick(pkg: String, label: String): ButtonSignal {
        val lower = label.lowercase()
        return when {
            SUPER_LIKE_SIGNALS[pkg]?.any { lower.contains(it.lowercase()) } == true -> ButtonSignal.SUPER_LIKE
            LIKE_SIGNALS[pkg]?.any { lower.contains(it.lowercase()) } == true       -> ButtonSignal.LIKE
            DISLIKE_SIGNALS[pkg]?.any { lower.contains(it.lowercase()) } == true    -> ButtonSignal.DISLIKE
            MESSAGE_SENT_SIGNALS[pkg]?.any { lower.contains(it.lowercase()) } == true -> ButtonSignal.MESSAGE_SEND
            else -> ButtonSignal.NONE
        }
    }

    fun isMatchScreen(pkg: String, text: String): Boolean {
        val lower = text.lowercase()
        return MATCH_SIGNALS[pkg]?.any { lower.contains(it.lowercase()) } == true
    }

    fun isConversationScreen(pkg: String, className: String): Boolean {
        val lower = className.lowercase()
        return CONVERSATION_SCREEN_SIGNALS[pkg]?.any { lower.contains(it.lowercase()) } == true
    }
}
