package com.skynet.monitoring.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.skynet.monitoring.data.local.room.entity.NotificationEntity

@Dao
interface NotificationDao {

    @Query("SELECT * FROM notifications ORDER BY id DESC")
    suspend fun getNotifications(): List<NotificationEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET is_read = 1 WHERE id = :id")
    suspend fun markAsRead(id: Int)

    @Query("DELETE FROM notifications WHERE id NOT IN (:activeIds)")
    suspend fun deleteNotIn(activeIds: List<Int>)

    @Query("DELETE FROM notifications")
    suspend fun clearAll()
}
