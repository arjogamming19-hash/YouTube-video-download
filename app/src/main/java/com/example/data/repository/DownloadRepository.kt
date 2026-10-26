package com.example.data.repository

import android.content.Context
import com.example.data.local.DownloadDao
import com.example.data.local.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.network.StorageStats
import com.example.data.network.VideoDownloadManager
import com.example.data.network.YouTubeParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import okhttp3.OkHttpClient
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class DownloadRepository(
  private val context: Context,
  private val downloadDao: DownloadDao
) {
  private val downloadManager = VideoDownloadManager(context)
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
  private val okHttpClient = OkHttpClient()

  private val _maxConcurrency = MutableStateFlow(2)
  val maxConcurrency: StateFlow<Int> = _maxConcurrency.asStateFlow()

  private var semaphore = Semaphore(_maxConcurrency.value)
  private val activeJobs = ConcurrentHashMap<Long, Job>()

  val queueDownloads: Flow<List<DownloadItem>> = downloadDao.getQueueDownloads()
  val completedDownloads: Flow<List<DownloadItem>> = downloadDao.getCompletedDownloads()
  val allDownloads: Flow<List<DownloadItem>> = downloadDao.getAllDownloads()

  private val _storageStats = MutableStateFlow(downloadManager.getStorageStats())
  val storageStats: StateFlow<StorageStats> = _storageStats.asStateFlow()

  fun refreshStorageStats() {
    _storageStats.value = downloadManager.getStorageStats()
  }

  fun setMaxConcurrency(limit: Int) {
    val clamped = limit.coerceIn(1, 5)
    _maxConcurrency.value = clamped
    semaphore = Semaphore(clamped)
  }

  suspend fun addBatch(
    urls: List<String>,
    formatType: String,
    quality: String
  ): Int {
    if (urls.isEmpty()) return 0
    val timeStamp = SimpleDateFormat("MMdd_HHmm", Locale.getDefault()).format(Date())
    val batchId = "batch_$timeStamp"

    val items = urls.map { url ->
      val videoId = YouTubeParser.extractVideoId(url) ?: ("id_" + Math.abs(url.hashCode()).toString().take(8))
      DownloadItem(
        batchId = batchId,
        originalUrl = url,
        videoId = videoId,
        title = "Loading title...",
        channelTitle = "YouTube Channel",
        thumbnailUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg",
        formatType = formatType,
        targetQuality = quality,
        status = DownloadStatus.QUEUED,
        fileName = "Video_$videoId"
      )
    }

    downloadDao.insertAll(items)
    triggerQueueProcessing()
    return items.size
  }

  fun triggerQueueProcessing() {
    scope.launch {
      val pendingItems = downloadDao.getPendingQueue()
      for (item in pendingItems) {
        if (!activeJobs.containsKey(item.id)) {
          startDownloadJob(item)
        }
      }
    }
  }

  private fun startDownloadJob(initialItem: DownloadItem) {
    val job = scope.launch {
      semaphore.acquire()
      try {
        // Step 1: Fetch metadata if not already done
        var item = downloadDao.getById(initialItem.id) ?: initialItem
        if (item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.CANCELED) {
          return@launch
        }

        downloadDao.update(item.copy(status = DownloadStatus.FETCHING_INFO))

        val parsed = YouTubeParser.fetchVideoDetails(okHttpClient, item.originalUrl, item.formatType)
        val safeFileName = downloadManager.sanitizeFileName(parsed.title)

        item = item.copy(
          title = parsed.title,
          channelTitle = parsed.channelTitle,
          thumbnailUrl = parsed.thumbnailUrl,
          totalBytes = parsed.estimatedSizeBytes,
          fileName = safeFileName,
          durationSeconds = parsed.durationSeconds,
          status = DownloadStatus.DOWNLOADING
        )
        downloadDao.update(item)

        // Step 2: Download stream to local file storage
        downloadManager.downloadItem(
          item = item,
          onProgressUpdate = { progress, downloadedBytes, totalBytes, speed ->
            downloadDao.update(
              item.copy(
                progress = progress,
                downloadedBytes = downloadedBytes,
                totalBytes = totalBytes,
                downloadSpeed = speed,
                status = DownloadStatus.DOWNLOADING
              )
            )
          },
          onComplete = { filePath, totalBytes ->
            val completedItem = item.copy(
              progress = 1f,
              downloadedBytes = totalBytes,
              totalBytes = totalBytes,
              downloadSpeed = "Done",
              status = DownloadStatus.COMPLETED,
              localFilePath = filePath,
              fileSizeBytes = totalBytes,
              completedAt = System.currentTimeMillis()
            )
            downloadDao.update(completedItem)
            refreshStorageStats()
            triggerQueueProcessing()
          },
          onError = { errorMsg ->
            downloadDao.update(
              item.copy(
                status = DownloadStatus.FAILED,
                errorMessage = errorMsg,
                downloadSpeed = "Failed"
              )
            )
            triggerQueueProcessing()
          }
        )
      } finally {
        semaphore.release()
        activeJobs.remove(initialItem.id)
      }
    }

    activeJobs[initialItem.id] = job
  }

  fun pauseItem(id: Long) {
    scope.launch {
      activeJobs[id]?.cancel()
      activeJobs.remove(id)
      downloadManager.pauseDownload(id)
      val item = downloadDao.getById(id)
      if (item != null && item.status == DownloadStatus.DOWNLOADING) {
        downloadDao.update(item.copy(status = DownloadStatus.PAUSED, downloadSpeed = "Paused"))
      }
    }
  }

  fun resumeItem(id: Long) {
    scope.launch {
      val item = downloadDao.getById(id)
      if (item != null) {
        downloadDao.update(item.copy(status = DownloadStatus.QUEUED, errorMessage = null))
        triggerQueueProcessing()
      }
    }
  }

  fun cancelItem(id: Long) {
    scope.launch {
      activeJobs[id]?.cancel()
      activeJobs.remove(id)
      val item = downloadDao.getById(id)
      if (item != null) {
        downloadManager.cancelDownload(id, item.videoId, item.title, item.formatType)
        downloadDao.deleteById(id)
      }
      triggerQueueProcessing()
    }
  }

  fun retryItem(id: Long) {
    scope.launch {
      val item = downloadDao.getById(id)
      if (item != null) {
        downloadDao.update(
          item.copy(
            status = DownloadStatus.QUEUED,
            progress = 0f,
            downloadedBytes = 0L,
            errorMessage = null,
            downloadSpeed = "0 KB/s"
          )
        )
        triggerQueueProcessing()
      }
    }
  }

  fun pauseAll() {
    scope.launch {
      activeJobs.forEach { (id, job) ->
        job.cancel()
        downloadManager.pauseDownload(id)
      }
      activeJobs.clear()
      val queue = downloadDao.getPendingQueue()
      for (item in queue) {
        downloadDao.update(item.copy(status = DownloadStatus.PAUSED, downloadSpeed = "Paused"))
      }
    }
  }

  fun resumeAll() {
    scope.launch {
      val all = downloadDao.getPendingQueue()
      for (item in all) {
        if (item.status == DownloadStatus.PAUSED) {
          downloadDao.update(item.copy(status = DownloadStatus.QUEUED))
        }
      }
      triggerQueueProcessing()
    }
  }

  fun deleteCompletedItem(item: DownloadItem, removeFileFromDisk: Boolean = true) {
    scope.launch {
      if (removeFileFromDisk) {
        downloadManager.deleteLocalFile(item.localFilePath)
      }
      downloadDao.deleteById(item.id)
      refreshStorageStats()
    }
  }

  fun clearQueue() {
    scope.launch {
      activeJobs.forEach { (id, job) ->
        job.cancel()
        downloadManager.pauseDownload(id)
      }
      activeJobs.clear()
      downloadDao.clearQueue()
    }
  }

  fun clearAllCompletedFiles() {
    scope.launch {
      val dir = downloadManager.getMediaStorageDir()
      if (dir.exists()) {
        dir.listFiles()?.forEach { file ->
          if (file.isFile) file.delete()
        }
      }
      downloadDao.deleteCompleted()
      refreshStorageStats()
    }
  }
}
