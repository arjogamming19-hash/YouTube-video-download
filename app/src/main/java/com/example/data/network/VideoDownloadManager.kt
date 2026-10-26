package com.example.data.network

import android.content.Context
import android.os.Environment
import com.example.data.local.DownloadItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.DecimalFormat
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

class VideoDownloadManager(private val context: Context) {

  private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
    .connectTimeout(20, TimeUnit.SECONDS)
    .readTimeout(45, TimeUnit.SECONDS)
    .followRedirects(true)
    .followSslRedirects(true)
    .build()

  private val activeJobs = ConcurrentHashMap<Long, Boolean>()

  companion object {
    const val USER_AGENT =
      "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"

    fun formatBytes(bytes: Long): String {
      if (bytes <= 0) return "0 B"
      val df = DecimalFormat("#,##0.#")
      return when {
        bytes >= 1024L * 1024L * 1024L -> "${df.format(bytes.toDouble() / (1024.0 * 1024.0 * 1024.0))} GB"
        bytes >= 1024L * 1024L -> "${df.format(bytes.toDouble() / (1024.0 * 1024.0))} MB"
        bytes >= 1024L -> "${df.format(bytes.toDouble() / 1024.0)} KB"
        else -> "$bytes B"
      }
    }

    fun formatSpeed(bytesPerSec: Long): String {
      if (bytesPerSec <= 0) return "0 KB/s"
      val df = DecimalFormat("#,##0.#")
      return when {
        bytesPerSec >= 1024L * 1024L -> "${df.format(bytesPerSec.toDouble() / (1024.0 * 1024.0))} MB/s"
        bytesPerSec >= 1024L -> "${df.format(bytesPerSec.toDouble() / 1024.0)} KB/s"
        else -> "$bytesPerSec B/s"
      }
    }
  }

  fun getMediaStorageDir(): File {
    val externalDir = context.getExternalFilesDir(Environment.DIRECTORY_MOVIES)
    val dir = if (externalDir != null && externalDir.canWrite()) {
      File(externalDir, "TubeBatch")
    } else {
      File(context.filesDir, "tubebatch_media")
    }
    if (!dir.exists()) {
      dir.mkdirs()
    }
    return dir
  }

  fun sanitizeFileName(name: String): String {
    return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
      .trim()
      .take(60)
  }

  suspend fun downloadItem(
    item: DownloadItem,
    streamUrls: List<String>,
    onProgressUpdate: suspend (progress: Float, downloadedBytes: Long, totalBytes: Long, speed: String) -> Unit,
    onComplete: suspend (filePath: String, totalBytes: Long) -> Unit,
    onError: suspend (errorMessage: String) -> Unit
  ) = withContext(Dispatchers.IO) {
    activeJobs[item.id] = true

    if (streamUrls.isEmpty()) {
      onError("Could not extract original YouTube stream. Video may be private or age-restricted.")
      activeJobs.remove(item.id)
      return@withContext
    }

    val storageDir = getMediaStorageDir()
    val ext = if (item.formatType.contains("MP3")) "mp3" else if (item.formatType.contains("M4A")) "m4a" else "mp4"
    val safeTitle = sanitizeFileName(item.title.ifBlank { "Video_${item.videoId}" })
    val targetFile = File(storageDir, "${safeTitle}_${item.videoId}.$ext")
    val tempFile = File(storageDir, "${safeTitle}_${item.videoId}.$ext.part")

    var existingBytes = 0L
    if (tempFile.exists()) {
      existingBytes = tempFile.length()
    }

    try {
      val urlsToTry = streamUrls

      var successfulResponse: Response? = null
      var lastError: String = "Download stream unreachable"

      // Attempt download from candidate streams with YouTube browser headers
      for (candidateUrl in urlsToTry) {
        if (!coroutineContext.isActive || activeJobs[item.id] != true) {
          return@withContext
        }

        try {
          val requestBuilder = Request.Builder()
            .url(candidateUrl)
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://www.youtube.com/")
            .header("Origin", "https://www.youtube.com")
            .header("Accept", "*/*")
            .header("Accept-Encoding", "identity")

          if (existingBytes > 0) {
            requestBuilder.header("Range", "bytes=$existingBytes-")
          }

          val response = okHttpClient.newCall(requestBuilder.build()).execute()
          if (response.isSuccessful || response.code == 206) {
            successfulResponse = response
            break
          } else {
            response.close()
            // If Range was not satisfiable (416) or Forbidden (403), reset existingBytes and try next
            if (response.code == 416 || response.code == 403) {
              tempFile.delete()
              existingBytes = 0L
            }
            lastError = "HTTP ${response.code}: ${response.message}"
          }
        } catch (e: Exception) {
          lastError = e.localizedMessage ?: "Connection error"
        }
      }

      if (successfulResponse == null) {
        onError(lastError)
        return@withContext
      }

      val response = successfulResponse
      val body = response.body ?: throw Exception("Empty response body")
      val contentLength = body.contentLength()
      val totalBytes = if (contentLength > 0) existingBytes + contentLength else if (item.totalBytes > 0) item.totalBytes else 20L * 1024L * 1024L

      val inputStream: InputStream = body.byteStream()
      val outputStream = FileOutputStream(tempFile, existingBytes > 0)

      val buffer = ByteArray(16 * 1024)
      var bytesRead: Int
      var totalDownloaded = existingBytes
      var lastProgressTime = System.currentTimeMillis()
      var bytesSinceLastCheck = 0L

      try {
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
          if (!coroutineContext.isActive || activeJobs[item.id] != true) {
            outputStream.flush()
            outputStream.close()
            inputStream.close()
            response.close()
            return@withContext
          }

          outputStream.write(buffer, 0, bytesRead)
          totalDownloaded += bytesRead
          bytesSinceLastCheck += bytesRead

          val now = System.currentTimeMillis()
          val elapsed = now - lastProgressTime
          if (elapsed >= 300 || totalDownloaded >= totalBytes) {
            val speedBytesPerSec = if (elapsed > 0) (bytesSinceLastCheck * 1000) / elapsed else 0L
            val speedString = formatSpeed(speedBytesPerSec)
            val progress = if (totalBytes > 0) (totalDownloaded.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f) else 0.5f

            onProgressUpdate(progress, totalDownloaded, totalBytes, speedString)
            lastProgressTime = now
            bytesSinceLastCheck = 0L
          }
        }

        outputStream.flush()
        outputStream.close()
        inputStream.close()
        response.close()

        if (tempFile.exists()) {
          if (targetFile.exists()) {
            targetFile.delete()
          }
          tempFile.renameTo(targetFile)
        }

        onComplete(targetFile.absolutePath, targetFile.length())
      } finally {
        try { outputStream.close() } catch (_: Exception) {}
        try { inputStream.close() } catch (_: Exception) {}
        try { response.close() } catch (_: Exception) {}
      }
    } catch (e: CancellationException) {
      // Normal coroutine cancellation
    } catch (e: Exception) {
      onError(e.localizedMessage ?: "Download failed")
    } finally {
      activeJobs.remove(item.id)
    }
  }

  fun pauseDownload(itemId: Long) {
    activeJobs.remove(itemId)
  }

  fun cancelDownload(itemId: Long, videoId: String, title: String, formatType: String) {
    activeJobs.remove(itemId)
    try {
      val storageDir = getMediaStorageDir()
      val ext = if (formatType.contains("MP3")) "mp3" else if (formatType.contains("M4A")) "m4a" else "mp4"
      val safeTitle = sanitizeFileName(title.ifBlank { "Video_$videoId" })
      val tempFile = File(storageDir, "${safeTitle}_$videoId.$ext.part")
      if (tempFile.exists()) {
        tempFile.delete()
      }
    } catch (_: Exception) {}
  }

  fun deleteLocalFile(filePath: String?): Boolean {
    if (filePath == null) return false
    return try {
      val file = File(filePath)
      if (file.exists()) {
        file.delete()
      } else false
    } catch (_: Exception) {
      false
    }
  }

  fun getStorageStats(): StorageStats {
    val dir = getMediaStorageDir()
    var totalSize = 0L
    var count = 0
    if (dir.exists()) {
      dir.listFiles()?.forEach { file ->
        if (file.isFile && !file.name.endsWith(".part")) {
          totalSize += file.length()
          count++
        }
      }
    }
    val freeSpace = dir.usableSpace
    val totalDeviceSpace = dir.totalSpace

    return StorageStats(
      totalDownloadedBytes = totalSize,
      fileCount = count,
      freeDiskBytes = freeSpace,
      totalDiskBytes = totalDeviceSpace,
      storageDirectoryPath = dir.absolutePath
    )
  }
}

data class StorageStats(
  val totalDownloadedBytes: Long,
  val fileCount: Int,
  val freeDiskBytes: Long,
  val totalDiskBytes: Long,
  val storageDirectoryPath: String
)
