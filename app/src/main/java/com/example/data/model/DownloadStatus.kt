package com.example.data.model

enum class DownloadStatus {
  QUEUED,
  FETCHING_INFO,
  DOWNLOADING,
  PAUSED,
  COMPLETED,
  FAILED,
  CANCELED
}

enum class FormatType(val displayName: String, val extension: String, val mimeType: String) {
  VIDEO_MP4("Video (MP4)", "mp4", "video/mp4"),
  AUDIO_MP3("Audio (MP3)", "mp3", "audio/mpeg"),
  AUDIO_M4A("Audio (M4A)", "m4a", "audio/mp4")
}

data class QualityOption(
  val label: String,
  val resolution: String,
  val estimatedSizeMb: Float
)
