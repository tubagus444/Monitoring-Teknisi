package com.skynet.monitoring.data.api.model

import com.google.gson.annotations.SerializedName

data class NotificationItem(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("body") val body: String,
    @SerializedName("is_read") val isRead: Boolean,
    @SerializedName("created_at") val createdAt: String?,
)

data class NotificationListResponse(
    @SerializedName("data") val data: List<NotificationItem>,
)
