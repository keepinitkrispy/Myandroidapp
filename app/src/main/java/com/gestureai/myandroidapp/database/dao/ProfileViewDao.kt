package com.gestureai.myandroidapp.database.dao

import androidx.room.*
import com.gestureai.myandroidapp.database.entities.ProfileView
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileViewDao {

    @Insert
    suspend fun insert(profileView: ProfileView): Long

    @Update
    suspend fun update(profileView: ProfileView)

    @Query("SELECT * FROM profile_views ORDER BY viewStartMs DESC LIMIT :limit")
    fun getRecent(limit: Int = 100): Flow<List<ProfileView>>

    @Query("SELECT * FROM profile_views WHERE id = :id")
    suspend fun getById(id: Long): ProfileView?

    @Query("""
        SELECT * FROM profile_views
        WHERE appPackage = :pkg
        ORDER BY viewStartMs DESC
        LIMIT :limit
    """)
    fun getByApp(pkg: String, limit: Int = 500): Flow<List<ProfileView>>

    @Query("SELECT COUNT(*) FROM profile_views")
    suspend fun totalCount(): Int

    @Query("SELECT COUNT(*) FROM profile_views WHERE appPackage = :pkg")
    suspend fun countByApp(pkg: String): Int

    /** Average view duration in ms, per app — quick sanity check */
    @Query("""
        SELECT AVG(durationMs)
        FROM profile_views
        WHERE appPackage = :pkg AND durationMs IS NOT NULL
    """)
    suspend fun avgDurationMs(pkg: String): Double?
}
