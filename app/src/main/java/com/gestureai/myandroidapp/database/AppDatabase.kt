package com.gestureai.myandroidapp.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.gestureai.myandroidapp.database.dao.*
import com.gestureai.myandroidapp.database.entities.*

class Converters {
    @TypeConverter fun fromSwipeDirection(v: SwipeDirection): String = v.name
    @TypeConverter fun toSwipeDirection(v: String): SwipeDirection = SwipeDirection.valueOf(v)

    @TypeConverter fun fromConvEventType(v: ConversationEventType): String = v.name
    @TypeConverter fun toConvEventType(v: String): ConversationEventType = ConversationEventType.valueOf(v)
}

@Database(
    entities = [
        ProfileView::class,
        SwipeEvent::class,
        MatchEvent::class,
        ConversationEvent::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun profileViewDao(): ProfileViewDao
    abstract fun swipeEventDao(): SwipeEventDao
    abstract fun matchEventDao(): MatchEventDao
    abstract fun conversationEventDao(): ConversationEventDao

    companion object {
        private const val DB_NAME = "swipe_tracker.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
