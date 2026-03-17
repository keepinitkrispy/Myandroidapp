package com.gestureai.myandroidapp.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SwipeDirection { LEFT, RIGHT, SUPER_LIKE, UP, UNKNOWN, ABORTED }

/**
 * A single swipe decision — the core training signal.
 * Each swipe links back to its ProfileView so we can pair decision with behavior.
 *
 * Aborted swipes (ABORTED direction) are the richest behavioral signal:
 * the user initiated a swipe, then reversed it. This captures the gap between
 * first instinct and the conscious decision that overrode it.
 */
@Entity(
    tableName = "swipe_events",
    foreignKeys = [
        ForeignKey(
            entity = ProfileView::class,
            parentColumns = ["id"],
            childColumns = ["profileViewId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("profileViewId")]
)
data class SwipeEvent(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** FK back to the ProfileView this swipe ended */
    val profileViewId: Long?,

    /** Which app */
    val appPackage: String,

    /** Epoch millis of the swipe */
    val timestampMs: Long,

    /** LEFT (nope/pass) | RIGHT (like) | SUPER_LIKE | UP (Tinder super) */
    val direction: SwipeDirection,

    /**
     * Whether this right-swipe eventually resulted in a mutual match.
     * Starts false, updated to true when a match event fires.
     */
    val resultedInMatch: Boolean = false,

    /** How the swipe was detected: "button_click", "gesture", "keyboard" */
    val detectionMethod: String = "button_click",

    /**
     * True when direction == UNKNOWN and the user started a swipe gesture
     * toward one direction then reversed and completed toward the opposite,
     * OR pressed Like then immediately tapped Undo.
     * The [abortedDirection] field records what the initial impulse was.
     */
    val wasAborted: Boolean = false,

    /**
     * For aborted swipes: the direction of the first (reversed) gesture.
     * e.g. abortedDirection=RIGHT + direction=UNKNOWN means "started to like,
     * pulled back, never completed." This is the hesitation signal.
     */
    val abortedDirection: SwipeDirection? = null,

    /**
     * For aborted swipes: how far (0.0–1.0) the swipe traveled before reversal.
     * Derived from gesture displacement if detectable, otherwise null.
     */
    val abortedProgress: Float? = null
)
