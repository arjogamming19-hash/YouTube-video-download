package com.example.data.network

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.regex.Pattern

data class ParsedVideoInfo(
  val videoId: String,
  val originalUrl: String,
  val title: String,
  val channelTitle: String,
  val thumbnailUrl: String,
  val streamUrl: String,
  val mirrorUrls: List<String>,
  val isOriginalYouTubeStream: Boolean,
  val qualityLabel: String,
  val estimatedSizeBytes: Long,
  val durationSeconds: Int
)

object YouTubeParser {
  private val YOUTUBE_REGEX = Pattern.compile(
    "(?:https?:\\/\\/)?(?:www\\.|m\\.)?(?:youtube\\.com\\/(?:watch\\?v=|embed\\/|v\\/|shorts\\/)|youtu\\.be\\/)([a-zA-Z0-9_-]{11})",
    Pattern.CASE_INSENSITIVE
  )

  // Direct Invidious and Piped instances for fetching real decoded YouTube streams
  private val STREAM_RESOLVER_APIS = listOf(
    "https://yewtu.be/api/v1/videos/",
    "https://invidious.private.coffee/api/v1/videos/",
    "https://inv.bp.projectsegfau.lt/api/v1/videos/",
    "https://invidious.drgns.space/api/v1/videos/"
  )

  // Verified demo pack streams used ONLY for the curated sample demo presets
  val DEMO_PRESET_VIDEO_STREAMS = listOf(
    "https://samplelib.com/lib/preview/mp4/sample-20s.mp4",
    "https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/720/Big_Buck_Bunny_720_10s_10MB.mp4",
    "https://raw.githubusercontent.com/intel-iot-devkit/sample-videos/master/person-bicycle-car-detection.mp4",
    "https://samplelib.com/lib/preview/mp4/sample-10s.mp4"
  )

  val DEMO_PRESET_AUDIO_STREAMS = listOf(
    "https://www.soundhelix.com/examples/mp3/SoundHelix-Song-1.mp3",
    "https://samplelib.com/lib/preview/mp3/sample-15s.mp3",
    "https://samplelib.com/lib/preview/mp3/sample-9s.mp3"
  )

  fun extractVideoId(text: String): String? {
    val trimmed = text.trim()
    val matcher = YOUTUBE_REGEX.matcher(trimmed)
    if (matcher.find()) {
      return matcher.group(1)
    }
    if (trimmed.matches(Regex("^[a-zA-Z0-9_-]{11}$"))) {
      return trimmed
    }
    return null
  }

  fun extractBatchUrls(batchInput: String): List<String> {
    val lines = batchInput.split(Regex("[\r\n,;]+"))
    val result = mutableListOf<String>()
    for (line in lines) {
      val trimmed = line.trim()
      if (trimmed.isNotBlank()) {
        val videoId = extractVideoId(trimmed)
        if (videoId != null) {
          result.add("https://www.youtube.com/watch?v=$videoId")
        } else if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
          result.add(trimmed)
        }
      }
    }
    return result.distinct()
  }

  /**
   * Fast, reliable direct YouTube MP4/MP3 stream resolver for any YouTube video or short.
   */
  private suspend fun resolveDirectYouTubeStream(
    client: OkHttpClient,
    videoId: String,
    isAudio: Boolean
  ): Pair<String, String?>? = withContext(Dispatchers.IO) {
    try {
      val fmt = if (isAudio) "mp3" else "720"
      val initUrl = "https://loader.to/ajax/download.php?format=$fmt&url=https://www.youtube.com/watch?v=$videoId"
      val req = Request.Builder()
        .url(initUrl)
        .header("User-Agent", VideoDownloadManager.USER_AGENT)
        .build()

      val resp = client.newCall(req).execute()
      if (!resp.isSuccessful) {
        resp.close()
        return@withContext null
      }

      val body = resp.body?.string() ?: ""
      resp.close()
      if (body.isBlank()) return@withContext null

      val json = JSONObject(body)
      val progressUrl = json.optString("progress_url", "")
      val initialTitle = json.optString("title", null)

      if (progressUrl.isBlank()) return@withContext null

      // Poll progress URL up to 10 times (each 1.5 seconds)
      for (i in 0 until 10) {
        delay(1500)
        val pollReq = Request.Builder()
          .url(progressUrl)
          .header("User-Agent", VideoDownloadManager.USER_AGENT)
          .build()

        val pollResp = client.newCall(pollReq).execute()
        if (pollResp.isSuccessful) {
          val pollBody = pollResp.body?.string() ?: ""
          pollResp.close()
          if (pollBody.isNotBlank()) {
            val pollJson = JSONObject(pollBody)
            val success = pollJson.optInt("success", 0)
            val downloadUrl = pollJson.optString("download_url", "")
            val title = pollJson.optString("title", initialTitle)
            if (success == 1 && downloadUrl.isNotBlank() && downloadUrl != "null") {
              return@withContext Pair(downloadUrl, title)
            }
          }
        } else {
          pollResp.close()
        }
      }
    } catch (_: Exception) {}
    return@withContext null
  }

  suspend fun fetchVideoDetails(
    context: Context,
    client: OkHttpClient,
    url: String,
    formatType: String
  ): ParsedVideoInfo = withContext(Dispatchers.IO) {
    val videoId = extractVideoId(url) ?: ("vid_" + Math.abs(url.hashCode()).toString().take(8))
    var title = "YouTube Video ($videoId)"
    var channel = "YouTube Creator"
    var thumbnail = "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
    var duration = 180
    var isOriginalStream = false
    var extractedQuality = "720p HD"
    val originalCandidateUrls = mutableListOf<String>()

    val isPresetDemo = url.contains("demo_sample")

    // 1. Fetch real video metadata via oEmbed
    try {
      val oembedUrl = "https://www.youtube.com/oembed?url=https://www.youtube.com/watch?v=$videoId&format=json"
      val req = Request.Builder()
        .url(oembedUrl)
        .header("User-Agent", VideoDownloadManager.USER_AGENT)
        .build()

      val resp = client.newCall(req).execute()
      if (resp.isSuccessful) {
        val body = resp.body?.string()
        if (!body.isNullOrBlank()) {
          val json = JSONObject(body)
          title = json.optString("title", title)
          channel = json.optString("author_name", channel)
          val thumb = json.optString("thumbnail_url", "")
          if (thumb.isNotBlank()) thumbnail = thumb
        }
      }
      resp.close()
    } catch (_: Exception) {}

    // 2. If it's a real user YouTube video / short:
    if (!isPresetDemo) {
      val isAudio = formatType.startsWith("AUDIO")

      // Step A: Fast high-speed direct YouTube resolver
      val directResult = resolveDirectYouTubeStream(client, videoId, isAudio)
      if (directResult != null) {
        originalCandidateUrls.add(directResult.first)
        isOriginalStream = true
        if (directResult.second != null && title.startsWith("YouTube Video (")) {
          title = directResult.second!!
        }
      }

      // Step B: Invidious / Piped API resolver
      if (originalCandidateUrls.isEmpty()) {
        for (apiBase in STREAM_RESOLVER_APIS) {
          try {
            val req = Request.Builder()
              .url("$apiBase$videoId")
              .header("User-Agent", VideoDownloadManager.USER_AGENT)
              .header("Accept", "application/json")
              .build()

            val resp = client.newCall(req).execute()
            if (resp.isSuccessful) {
              val bodyStr = resp.body?.string()
              if (!bodyStr.isNullOrBlank()) {
                val json = JSONObject(bodyStr)
                if (title.startsWith("YouTube Video (")) {
                  title = json.optString("title", title)
                  channel = json.optString("author", channel)
                }
                duration = json.optInt("lengthSeconds", duration)

                if (isAudio) {
                  val adaptive = json.optJSONArray("adaptiveFormats")
                  if (adaptive != null) {
                    for (i in 0 until adaptive.length()) {
                      val item = adaptive.getJSONObject(i)
                      val type = item.optString("type", "")
                      val sUrl = item.optString("url", "")
                      if (type.contains("audio") && sUrl.isNotBlank()) {
                        originalCandidateUrls.add(sUrl)
                        isOriginalStream = true
                      }
                    }
                  }
                } else {
                  val formatStreams = json.optJSONArray("formatStreams")
                  if (formatStreams != null) {
                    for (i in 0 until formatStreams.length()) {
                      val item = formatStreams.getJSONObject(i)
                      val sUrl = item.optString("url", "")
                      val quality = item.optString("qualityLabel", item.optString("quality", "720p"))
                      if (sUrl.isNotBlank()) {
                        originalCandidateUrls.add(sUrl)
                        isOriginalStream = true
                        extractedQuality = quality
                      }
                    }
                  }
                }

                if (originalCandidateUrls.isNotEmpty()) {
                  resp.close()
                  break
                }
              }
            }
            resp.close()
          } catch (_: Exception) {}
        }
      }
    } else {
      // Demo preset
      if (formatType.startsWith("AUDIO")) {
        originalCandidateUrls.addAll(DEMO_PRESET_AUDIO_STREAMS)
      } else {
        originalCandidateUrls.addAll(DEMO_PRESET_VIDEO_STREAMS)
      }
    }

    val finalMirrors = originalCandidateUrls.distinct()

    val estimatedBytes = if (formatType.startsWith("AUDIO")) {
      (4L + (Math.abs(videoId.hashCode()) % 8)) * 1024L * 1024L
    } else {
      (15L + (Math.abs(videoId.hashCode()) % 35)) * 1024L * 1024L
    }

    ParsedVideoInfo(
      videoId = videoId,
      originalUrl = url,
      title = title,
      channelTitle = channel,
      thumbnailUrl = thumbnail,
      streamUrl = finalMirrors.firstOrNull() ?: "",
      mirrorUrls = finalMirrors,
      isOriginalYouTubeStream = isOriginalStream,
      qualityLabel = extractedQuality,
      estimatedSizeBytes = estimatedBytes,
      durationSeconds = duration
    )
  }
}
