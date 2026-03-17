package com.gestureai.myandroidapp.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One "card" viewed in a dating app.
 * Opened when a new profile appears on screen, closed when a swipe or app exit occurs.
 */
@Entity(tableName = "profile_views")
data class ProfileView(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Package name of the app — com.tinder, com.bumble.app, co.hinge.app, etc. */
    val appPackage: String,

    /** Epoch millis when the profile became visible */
    val viewStartMs: Long,

    /** Epoch millis when the session ended (swipe or app exit). Null until then. */
    val viewEndMs: Long? = null,

    /** Total milliseconds spent looking at this profile. Derived on session end. */
    val durationMs: Long? = null,

    /** Number of scroll events (down) during this view — photo swiping, bio reading */
    val scrollDownCount: Int = 0,

    /** Number of scroll events (up) — scrolling back up */
    val scrollUpCount: Int = 0,

    /** Total scroll events (either direction) */
    val scrollTotalCount: Int = 0,

    /** Hour of day (0–23) when the profile was first seen */
    val timeOfDayHour: Int,

    /** Day of week: 1=Mon, 7=Sun (Calendar.DAY_OF_WEEK shifted) */
    val dayOfWeek: Int,

    /** Screen name / section within the app, if detectable */
    val screenContext: String? = null
)
