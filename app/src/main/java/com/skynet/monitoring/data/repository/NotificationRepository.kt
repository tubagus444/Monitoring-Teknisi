package com.skynet.monitoring.data.repository

import com.google.gson.Gson
import com.skynet.monitoring.data.api.ApiService
import com.skynet.monitoring.data.api.model.NotificationItem
import com.skynet.monitoring.util.safeApiCall
import javax.inject.Inject

interface NotificationRepository {
    suspend fun getNotifications(): Result<List<NotificationItem>>
    suspend fun markRead(id: Int): Result<Unit>
}

class NotificationRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val gson: Gson,
) : NotificationRepository {

    override suspend fun getNotifications(): Result<List<NotificationItem>> =
        safeApiCall(gson) { api.getNotifications() }.map { it.data }

    override suspend fun markRead(id: Int): Result<Unit> =
        safeApiCall(gson) { api.markNotificationRead(id) }.map { }
}
