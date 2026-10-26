package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.DownloadStatus

class Converters {
  @TypeConverter
  fun fromDownloadStatus(status: DownloadStatus): String {
    return status.name
  }

  @TypeConverter
  fun toDownloadStatus(value: String): DownloadStatus {
    return try {
      DownloadStatus.valueOf(value)
    } catch (_: Exception) {
      DownloadStatus.QUEUED
    }
  }
}
