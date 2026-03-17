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

    @Query("UPDATE conversation_events SET replyLatencyMs = :latencyMs WHERE id = :id")
    suspend fun updateReplyLatency(id: Long, latencyMs: Long)

    /** Most recent MESSAGE_READ event in a conversation — used to compute reply latency */
    @Query("""
        SELECT * FROM conversation_events
        WHERE matchEventId = :matchId AND eventType = 'MESSAGE_READ'
        ORDER BY timestampMs DESC LIMIT 1
    """)
    suspend fun lastReadEvent(matchId: Long): ConversationEvent?

    /** Average THEIR response time ms — how quickly they reply to your messages */
    @Query("""
        SELECT AVG(responseTimeMs)
        FROM conversation_events
        WHERE eventType = 'MESSAGE_RECEIVED'
          AND responseTimeMs IS NOT NULL
          AND appPackage = :pkg
    """)
    suspend fun avgTheirResponseTimeMs(pkg: String): Double?

    /** Average YOUR read latency ms — how quickly you open received messages */
    @Query("""
        SELECT AVG(readLatencyMs)
        FROM conversation_events
        WHERE eventType = 'MESSAGE_READ'
          AND readLatencyMs IS NOT NULL
          AND appPackage = :pkg
    """)
    suspend fun avgYourReadLatencyMs(pkg: String): Double?

    /** Average YOUR reply latency ms — how quickly you reply after reading */
    @Query("""
        SELECT AVG(replyLatencyMs)
        FROM conversation_events
        WHERE eventType = 'MESSAGE_READ'
          AND replyLatencyMs IS NOT NULL
          AND appPackage = :pkg
    """)
    suspend fun avgYourReplyLatencyMs(pkg: String): Double?

    /** Total messages you've sent across all matches */
    @Query("""
        SELECT COUNT(*) FROM conversation_events
        WHERE eventType = 'MESSAGE_SENT' AND appPackage = :pkg
    """)
    suspend fun totalMessagesSent(pkg: String): Int
}
