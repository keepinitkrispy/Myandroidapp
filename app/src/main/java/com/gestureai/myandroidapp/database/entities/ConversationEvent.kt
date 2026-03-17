package com.gestureai.myandroidapp.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ConversationEventType {
    MATCH_OPENED,       // User tapped into a match conversation
    MESSAGE_SENT,       // User sent a message
    MESSAGE_RECEIVED,   // App showed a new incoming message
    CONVERSATION_CLOSED // User left the conversation
}

/**
 * Engagement tracking inside conversations.
 * Lets us correlate "did I actually message this match?" with swipe behavior.
 * Also captures response times as a proxy for mutual interest.
 */
@Entity(
    tableName = "conversation_events",
    foreignKeys = [
        ForeignKey(
            entity = MatchEvent::class,
            parentColumns = ["id"],
            childColumns = ["matchEventId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("matchEventId")]
)
data class ConversationEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** FK to the match this conversation belongs to, if linkable */
    val matchEventId: Long?,

    val appPackage: String,

    val timestampMs: Long,

    val eventType: ConversationEventType,

    /**
     * For MESSAGE_RECEIVED: millis since the previous MESSAGE_SENT in this
     * conversation — their response time. Null for sent messages or first contact.
     */
    val responseTimeMs: Long? = null,

    /** Time in millis the conversation was open (for CONVERSATION_CLOSED) */
    val sessionDurationMs: Long? = null
)
