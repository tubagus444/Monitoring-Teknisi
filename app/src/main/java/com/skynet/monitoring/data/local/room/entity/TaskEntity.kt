package com.skynet.monitoring.data.local.room.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.skynet.monitoring.data.api.model.RepairPhoto
import com.skynet.monitoring.data.api.model.Task
import com.skynet.monitoring.data.api.model.WorkLog

/**
 * Entitas Room untuk tabel `tasks` sebagai cache lokal.
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey
    @ColumnInfo(name = "id") val id: Int,
    @ColumnInfo(name = "report_id") val reportId: Int,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "customer") val customer: String?,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "damage_type") val damageType: String? = null,
    @ColumnInfo(name = "notes") val notes: String?,
    @ColumnInfo(name = "assigned_at") val assignedAt: String?,
    @ColumnInfo(name = "work_logs") val workLogs: List<WorkLog>? = null,
    @ColumnInfo(name = "category") val category: String? = null,
    @ColumnInfo(name = "headline") val headline: String? = null,
    @ColumnInfo(name = "phone") val phone: String? = null,
    @ColumnInfo(name = "customer_code") val customerCode: String? = null,
    @ColumnInfo(name = "ip_address") val ipAddress: String? = null,
    @ColumnInfo(name = "subscription_package") val subscriptionPackage: String? = null,
    @ColumnInfo(name = "house_photos") val housePhotos: List<String> = emptyList(),
    @ColumnInfo(name = "repair_photos") val repairPhotos: List<RepairPhoto> = emptyList(),
) {
    fun toDomain(): Task = Task(
        id = id,
        reportId = reportId,
        status = status,
        customer = customer,
        address = address,
        damageType = damageType,
        notes = notes,
        assignedAt = assignedAt,
        workLogs = workLogs,
        category = category,
        headline = headline,
        phone = phone,
        customerCode = customerCode,
        ipAddress = ipAddress,
        subscriptionPackage = subscriptionPackage,
        housePhotos = housePhotos,
        repairPhotos = repairPhotos,
    )
}

fun Task.toEntity(): TaskEntity = TaskEntity(
    id = id,
    reportId = reportId,
    status = status,
    customer = customer,
    address = address,
    damageType = damageType,
    notes = notes,
    assignedAt = assignedAt,
    workLogs = workLogs,
    category = category,
    headline = headline,
    phone = phone,
    customerCode = customerCode,
    ipAddress = ipAddress,
    subscriptionPackage = subscriptionPackage,
    housePhotos = housePhotos,
    repairPhotos = repairPhotos,
)
