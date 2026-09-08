package com.skynet.monitoring.data.local.room.converter

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.skynet.monitoring.data.api.model.RepairPhoto
import com.skynet.monitoring.data.api.model.WorkLog

/**
 * TypeConverter Room untuk serialisasi/deserialisasi tipe kompleks menggunakan Gson.
 */
class Converters {
    private val gson = Gson()

    @TypeConverter
    fun fromWorkLogList(value: List<WorkLog>?): String? {
        if (value == null) return null
        return gson.toJson(value)
    }

    @TypeConverter
    fun toWorkLogList(value: String?): List<WorkLog>? {
        if (value.isNullOrBlank()) return null
        val type = object : TypeToken<List<WorkLog>>() {}.type
        return gson.fromJson(value, type)
    }

    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        if (value == null) return null
        return gson.toJson(value)
    }

    @TypeConverter
    fun toStringList(value: String?): List<String> {
        if (value.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }

    @TypeConverter
    fun fromRepairPhotoList(value: List<RepairPhoto>?): String? {
        if (value == null) return null
        return gson.toJson(value)
    }

    @TypeConverter
    fun toRepairPhotoList(value: String?): List<RepairPhoto> {
        if (value.isNullOrBlank()) return emptyList()
        val type = object : TypeToken<List<RepairPhoto>>() {}.type
        return gson.fromJson(value, type) ?: emptyList()
    }
}
