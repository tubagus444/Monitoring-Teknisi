package com.skynet.monitoring.data.repository

import com.google.gson.Gson
import com.skynet.monitoring.data.api.ApiService
import com.skynet.monitoring.data.api.model.NotificationItem
import com.skynet.monitoring.data.local.room.dao.NotificationDao
import com.skynet.monitoring.data.local.room.entity.NotificationEntity
import com.skynet.monitoring.data.local.room.entity.toEntity
import com.skynet.monitoring.util.ApiException
import com.skynet.monitoring.util.safeApiCall
import javax.inject.Inject

interface NotificationRepository {
    suspend fun getNotifications(): Result<List<NotificationItem>>
    suspend fun markRead(id: Int): Result<Unit>
}

class NotificationRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val notificationDao: NotificationDao,
    private val gson: Gson,
) : NotificationRepository {

    override suspend fun getNotifications(): Result<List<NotificationItem>> {
        val networkResult = safeApiCall(gson) { api.getNotifications() }.map { it.data }
        return networkResult.fold(
            onSuccess = { notifications ->
                runCatching { syncNotificationsToCache(notifications.map { it.toEntity() }) }
                Result.success(notifications)
            },
            onFailure = { error ->
                if (!shouldFallbackToCache(error)) return Result.failure(error)

                val cached = runCatching { notificationDao.getNotifications().map { it.toDomain() } }.getOrNull().orEmpty()
                if (cached.isNotEmpty()) {
                    Result.success(cached)
                } else {
                    Result.failure(error)
                }
            }
        )
    }

    private suspend fun syncNotificationsToCache(incoming: List<NotificationEntity>) {
        notificationDao.insertNotifications(incoming)
        if (incoming.isNotEmpty()) {
            notificationDao.deleteNotIn(incoming.map { it.id })
        } else {
            notificationDao.clearAll()
        }
    }

    private fun shouldFallbackToCache(error: Throwable): Boolean {
        val apiException = error as? ApiException ?: return true
        val code = apiException.code ?: return true
        return code >= 500
    }

    override suspend fun markRead(id: Int): Result<Unit> {
        runCatching { notificationDao.markAsRead(id) }
        return safeApiCall(gson) { api.markNotificationRead(id) }.map { }
    }
}
