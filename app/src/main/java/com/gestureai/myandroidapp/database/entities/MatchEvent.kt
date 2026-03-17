package com.gestureai.myandroidapp.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Fired when the app shows a "It's a Match!" / "You matched" overlay.
 * Links back to the SwipeEvent that caused it (best-effort — timing-based).
 */
@Entity(
    tableName = "match_events",
    foreignKeys = [
        ForeignKey(
            entity = SwipeEvent::class,
            parentColumns = ["id"],
            childColumns = ["swipeEventId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("swipeEventId")]
)
data class MatchEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** FK to the swipe that preceded this match, if linkable */
    val swipeEventId: Long?,

    val appPackage: String,

    val timestampMs: Long,

    /** Text snippet from the match overlay — helps with app-specific debugging */
    val matchTextSnippet: String? = null
)
