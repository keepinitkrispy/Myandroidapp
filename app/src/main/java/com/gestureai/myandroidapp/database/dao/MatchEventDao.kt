package com.gestureai.myandroidapp.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gestureai.myandroidapp.database.entities.MatchEvent
import kotlinx.coroutines.flow.Flow

@Dao
interface MatchEventDao {

    @Insert
    suspend fun insert(matchEvent: MatchEvent): Long

    @Query("SELECT * FROM match_events ORDER BY timestampMs DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): Flow<List<MatchEvent>>

    @Query("SELECT COUNT(*) FROM match_events WHERE appPackage = :pkg")
    suspend fun countByApp(pkg: String): Int

    @Query("""
        SELECT * FROM match_events
        WHERE timestampMs BETWEEN :fromMs AND :toMs
        ORDER BY timestampMs DESC
    """)
    suspend fun getInRange(fromMs: Long, toMs: Long): List<MatchEvent>
}
