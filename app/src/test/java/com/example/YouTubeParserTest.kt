package com.example

import com.example.data.network.YouTubeParser
import com.example.data.network.VideoDownloadManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YouTubeParserTest {

  @Test
  fun testExtractVideoId_variousFormats() {
    val standard = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"
    val short = "https://youtu.be/dQw4w9WgXcQ"
    val shorts = "https://www.youtube.com/shorts/dQw4w9WgXcQ"
    val embed = "https://www.youtube.com/embed/dQw4w9WgXcQ"
    val mobile = "https://m.youtube.com/watch?v=dQw4w9WgXcQ&feature=share"
    val rawId = "dQw4w9WgXcQ"

    assertEquals("dQw4w9WgXcQ", YouTubeParser.extractVideoId(standard))
    assertEquals("dQw4w9WgXcQ", YouTubeParser.extractVideoId(short))
    assertEquals("dQw4w9WgXcQ", YouTubeParser.extractVideoId(shorts))
    assertEquals("dQw4w9WgXcQ", YouTubeParser.extractVideoId(embed))
    assertEquals("dQw4w9WgXcQ", YouTubeParser.extractVideoId(mobile))
    assertEquals("dQw4w9WgXcQ", YouTubeParser.extractVideoId(rawId))
  }

  @Test
  fun testExtractBatchUrls() {
    val multiLineInput = """
      https://www.youtube.com/watch?v=aqz-KE-bpKQ
      https://youtu.be/kJQP7kiw5Fk
      https://www.youtube.com/shorts/9bZkp7q19f0
      
      invalid_random_string_that_is_not_an_id
    """.trimIndent()

    val urls = YouTubeParser.extractBatchUrls(multiLineInput)
    assertEquals(3, urls.size)
    assertTrue(urls.any { it.contains("aqz-KE-bpKQ") })
    assertTrue(urls.any { it.contains("kJQP7kiw5Fk") })
    assertTrue(urls.any { it.contains("9bZkp7q19f0") })
  }

  @Test
  fun testFormattingUtilities() {
    assertEquals("100 B", VideoDownloadManager.formatBytes(100))
    assertTrue(VideoDownloadManager.formatBytes(15 * 1024 * 1024).contains("MB"))
    assertTrue(VideoDownloadManager.formatBytes(2L * 1024 * 1024 * 1024).contains("GB"))

    assertEquals("0 KB/s", VideoDownloadManager.formatSpeed(0))
    assertTrue(VideoDownloadManager.formatSpeed(2 * 1024 * 1024).contains("MB/s"))
  }
}
