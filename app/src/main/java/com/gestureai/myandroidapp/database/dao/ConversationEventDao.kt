package com.gestureai.myandroidapp.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.gestureai.myandroidapp.database.entities.ConversationEvent
import com.gestureai.myandroidapp.database.entities.ConversationEventType
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversationEventDao {

    @Insert
    suspend fun insert(event: ConversationEvent): Long

    @Query("SELECT * FROM conversation_events ORDER BY timestampMs DESC LIMIT :limit")
    fun getRecent(limit: Int = 200): Flow<List<ConversationEvent>>

    @Query("SELECT COUNT(*) FROM conversation_events WHERE eventType = :type")
    suspend fun countByType(type: ConversationEventType): Int

    /** Average their response time in ms — how quickly they reply to your messages */
    @Query("""
        SELECT AVG(responseTimeMs)
        FROM conversation_events
        WHERE eventType = 'MESSAGE_RECEIVED'
          AND responseTimeMs IS NOT NULL
          AND appPackage = :pkg
    """)
    suspend fun avgResponseTimeMs(pkg: String): Double?

    /** Total messages you've sent across all matches */
    @Query("""
        SELECT COUNT(*) FROM conversation_events
        WHERE eventType = 'MESSAGE_SENT' AND appPackage = :pkg
    """)
    suspend fun totalMessagesSent(pkg: String): Int
}
