package com.skynet.monitoring.data.local.room.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.skynet.monitoring.data.local.room.entity.TaskEntity

@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks ORDER BY id DESC")
    suspend fun getTasks(): List<TaskEntity>

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    suspend fun getTaskById(id: Int): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTasks(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE id NOT IN (:activeIds)")
    suspend fun deleteNotIn(activeIds: List<Int>)

    @Query("DELETE FROM tasks")
    suspend fun clearAll()
}
