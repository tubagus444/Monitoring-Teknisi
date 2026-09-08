package com.skynet.monitoring.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.skynet.monitoring.data.api.model.NotificationItem

/**
 * Entitas Room untuk tabel `notifications` sebagai cache lokal.
 */
@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "body") val body: String,
    @ColumnInfo(name = "type") val type: String? = null,
    @ColumnInfo(name = "related_id") val relatedId: Int? = null,
    @ColumnInfo(name = "is_read") val isRead: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String?,
) {
    fun toDomain(): NotificationItem = NotificationItem(
        id = id,
        title = title,
        body = body,
        type = type,
        relatedId = relatedId,
        isRead = isRead,
        createdAt = createdAt,
    )
}

fun NotificationItem.toEntity(): NotificationEntity = NotificationEntity(
    id = id,
    title = title,
    body = body,
    type = type,
    relatedId = relatedId,
    isRead = isRead,
    createdAt = createdAt,
)
