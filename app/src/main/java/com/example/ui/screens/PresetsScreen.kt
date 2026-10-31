package com.example.ui.screens

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Audiotrack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.network.YouTubeParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

@Composable
fun PresetsScreen(
  onDownloadPreset: (List<String>) -> Unit
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var inspectorUrl by remember { mutableStateOf("") }
  var inspectedTitle by remember { mutableStateOf<String?>(null) }
  var inspectedChannel by remember { mutableStateOf<String?>(null) }
  var inspectedThumb by remember { mutableStateOf<String?>(null) }
  var inspectedVideoId by remember { mutableStateOf<String?>(null) }
  var isInspecting by remember { mutableStateOf(false) }
  var inspectError by remember { mutableStateOf<String?>(null) }

  // Batch URL cleaner input/output
  var urlCleanerInput by remember { mutableStateOf("") }
  var cleanedUrlOutput by remember { mutableStateOf<List<String>>(emptyList()) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("inspector_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // 1. Header
    item {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(
              Brush.linearGradient(
                listOf(
                  MaterialTheme.colorScheme.primary,
                  MaterialTheme.colorScheme.secondary
                )
              )
            ),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.TravelExplore,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
          Text(
            text = "Link Inspector & Tools",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Preview YouTube videos, inspect streams & clean batch URLs",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // 2. Link Inspector Tool Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Inspect YouTube Link",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = inspectorUrl,
            onValueChange = {
              inspectorUrl = it
              inspectedTitle = null
              inspectError = null
            },
            placeholder = { Text("Paste any YouTube video or Shorts URL...") },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("inspector_url_field"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            ),
            trailingIcon = {
              OutlinedButton(
                onClick = {
                  try {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    val clip = clipboard?.primaryClip
                    if (clip != null && clip.itemCount > 0) {
                      val text = clip.getItemAt(0)?.text?.toString() ?: ""
                      if (text.isNotBlank()) {
                        inspectorUrl = text.trim()
                        inspectedTitle = null
                        inspectError = null
                      }
                    }
                  } catch (_: Exception) {}
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.padding(end = 4.dp)
              ) {
                Icon(Icons.Default.ContentPaste, contentDescription = "Paste", modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Paste", fontSize = 11.sp)
              }
            }
          )

          Spacer(modifier = Modifier.height(10.dp))

          Button(
            onClick = {
              val videoId = YouTubeParser.extractVideoId(inspectorUrl)
              if (videoId != null) {
                isInspecting = true
                inspectError = null
                scope.launch {
                  try {
                    val info = withContext(Dispatchers.IO) {
                      YouTubeParser.fetchVideoDetails(context, OkHttpClient(), inspectorUrl, "VIDEO_MP4")
                    }
                    inspectedTitle = info.title
                    inspectedChannel = info.channelTitle
                    inspectedThumb = info.thumbnailUrl
                    inspectedVideoId = info.videoId
                  } catch (e: Exception) {
                    inspectError = "Could not fetch details. Check link or connection."
                  } finally {
                    isInspecting = false
                  }
                }
              } else {
                inspectError = "Please enter a valid YouTube video or Shorts URL."
              }
            },
            enabled = inspectorUrl.isNotBlank() && !isInspecting,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("inspect_link_button")
          ) {
            if (isInspecting) {
              CircularProgressIndicator(
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text("Inspecting Video...")
            } else {
              Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Inspect Video Details")
            }
          }

          inspectError?.let { err ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = err,
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall
            )
          }

          // Inspected result card
          if (inspectedTitle != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  inspectedThumb?.let { thumb ->
                    AsyncImage(
                      model = thumb,
                      contentDescription = null,
                      contentScale = ContentScale.Crop,
                      modifier = Modifier
                        .size(width = 88.dp, height = 56.dp)
                        .clip(RoundedCornerShape(6.dp))
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                  }
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = inspectedTitle ?: "",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Bold,
                      maxLines = 2,
                      color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "${inspectedChannel ?: "YouTube Creator"} • ID: ${inspectedVideoId ?: ""}",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  Button(
                    onClick = {
                      onDownloadPreset(listOf(inspectorUrl))
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(Icons.Default.Movie, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download MP4", fontSize = 12.sp)
                  }

                  Button(
                    onClick = {
                      onDownloadPreset(listOf(inspectorUrl))
                    },
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                      containerColor = MaterialTheme.colorScheme.secondary
                    ),
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(Icons.Default.Audiotrack, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Download MP3", fontSize = 12.sp)
                  }
                }
              }
            }
          }
        }
      }
    }

    // 3. Batch URL Cleaner & Formatter Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.CleaningServices,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.secondary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Batch URL Cleaner & Formatter",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "Paste text with raw links to remove share tracking (?si=..., &feature=...), convert Shorts to standard links, and validate URLs for batch queueing.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(10.dp))

          OutlinedTextField(
            value = urlCleanerInput,
            onValueChange = {
              urlCleanerInput = it
              cleanedUrlOutput = YouTubeParser.extractBatchUrls(it)
            },
            placeholder = { Text("Paste messy links, messages, or text containing YouTube URLs...") },
            minLines = 3,
            maxLines = 6,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            )
          )

          Spacer(modifier = Modifier.height(10.dp))

          if (cleanedUrlOutput.isNotEmpty()) {
            Text(
              text = "Found ${cleanedUrlOutput.size} valid YouTube URL${if (cleanedUrlOutput.size != 1) "s" else ""}:",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Surface(
              shape = RoundedCornerShape(8.dp),
              color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(8.dp)) {
                cleanedUrlOutput.take(5).forEach { u ->
                  Text(
                    text = "• $u",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                  )
                }
                if (cleanedUrlOutput.size > 5) {
                  Text(
                    text = "+ ${cleanedUrlOutput.size - 5} more links...",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                  )
                }
              }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
              onClick = {
                onDownloadPreset(cleanedUrlOutput)
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Send ${cleanedUrlOutput.size} Clean URLs to Download Queue")
            }
          }
        }
      }
    }

    // 4. Info card about Local File Storage
    item {
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier.padding(14.dp),
          verticalAlignment = Alignment.Top
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = "Offline Local Media Storage",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "All downloaded videos and audio tracks are saved to device media storage (`Movies/TubeBatch`). They remain playable offline anytime with the built-in player and can be exported or shared via Android's native share sheet.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}
