package com.gestureai.myandroidapp.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class SwipeDirection { LEFT, RIGHT, SUPER_LIKE, UP, UNKNOWN }

/**
 * A single swipe decision — the core training signal.
 * Each swipe links back to its ProfileView so we can pair decision with behavior.
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
    val detectionMethod: String = "button_click"
)
