package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.DownloadStatus

@Entity(tableName = "downloads")
data class DownloadItem(
  @PrimaryKey(autoGenerate = true)
  val id: Long = 0,
  val batchId: String,
  val originalUrl: String,
  val videoId: String,
  val title: String,
  val channelTitle: String = "YouTube Creator",
  val thumbnailUrl: String = "",
  val formatType: String = "VIDEO_MP4",
  val targetQuality: String = "720p HD",
  val status: DownloadStatus = DownloadStatus.QUEUED,
  val progress: Float = 0f,
  val downloadedBytes: Long = 0L,
  val totalBytes: Long = 0L,
  val downloadSpeed: String = "0 KB/s",
  val localFilePath: String? = null,
  val fileName: String = "",
  val fileSizeBytes: Long = 0L,
  val durationSeconds: Int = 180,
  val errorMessage: String? = null,
  val createdAt: Long = System.currentTimeMillis(),
  val completedAt: Long? = null
)
