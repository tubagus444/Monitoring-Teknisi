package com.skynet.monitoring.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.skynet.monitoring.data.local.room.converter.Converters
import com.skynet.monitoring.data.local.room.dao.NotificationDao
import com.skynet.monitoring.data.local.room.dao.TaskDao
import com.skynet.monitoring.data.local.room.entity.NotificationEntity
import com.skynet.monitoring.data.local.room.entity.TaskEntity

@Database(
    entities = [
        TaskEntity::class,
        NotificationEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun notificationDao(): NotificationDao
}
