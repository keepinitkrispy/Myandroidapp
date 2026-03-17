package com.gestureai.myandroidapp.database.dao

import androidx.room.*
import com.gestureai.myandroidapp.database.entities.SwipeDirection
import com.gestureai.myandroidapp.database.entities.SwipeEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface SwipeEventDao {

    @Insert
    suspend fun insert(swipeEvent: SwipeEvent): Long

    @Query("UPDATE swipe_events SET resultedInMatch = 1 WHERE id = :id")
    suspend fun markAsMatch(id: Long)

    /** Most recent swipe on a given app — used to link match events */
    @Query("""
        SELECT * FROM swipe_events
        WHERE appPackage = :pkg AND direction != 'LEFT'
        ORDER BY timestampMs DESC
        LIMIT 1
    """)
    suspend fun lastRightSwipe(pkg: String): SwipeEvent?

    @Query("SELECT * FROM swipe_events ORDER BY timestampMs DESC LIMIT :limit")
    fun getRecent(limit: Int = 200): Flow<List<SwipeEvent>>

    @Query("SELECT COUNT(*) FROM swipe_events WHERE direction = :dir")
    suspend fun countByDirection(dir: SwipeDirection): Int

    @Query("SELECT COUNT(*) FROM swipe_events WHERE resultedInMatch = 1")
    suspend fun totalMatches(): Int

    @Query("SELECT COUNT(*) FROM swipe_events WHERE direction = 'RIGHT'")
    suspend fun totalRightSwipes(): Int

    /** Match rate = matches / right swipes */
    @Query("""
        SELECT CAST(SUM(CASE WHEN resultedInMatch = 1 THEN 1 ELSE 0 END) AS FLOAT)
             / NULLIF(COUNT(*), 0)
        FROM swipe_events
        WHERE direction = 'RIGHT' AND appPackage = :pkg
    """)
    suspend fun matchRate(pkg: String): Float?

    @Query("""
        SELECT se.*, pv.durationMs, pv.scrollTotalCount
        FROM swipe_events se
        LEFT JOIN profile_views pv ON se.profileViewId = pv.id
        WHERE se.appPackage = :pkg
        ORDER BY se.timestampMs DESC
        LIMIT :limit
    """)
    @RewriteQueriesToDropUnusedColumns
    fun getSwipesWithViewData(pkg: String, limit: Int = 500): Flow<List<SwipeEvent>>
}
