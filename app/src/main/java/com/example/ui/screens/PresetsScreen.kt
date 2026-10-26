package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Headphones
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.MovieFilter
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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

data class BatchPreset(
  val title: String,
  val description: String,
  val icon: ImageVector,
  val gradientColors: List<Color>,
  val urls: List<String>,
  val formatNote: String
)

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
  var isInspecting by remember { mutableStateOf(false) }

  val presets = listOf(
    BatchPreset(
      title = "4K Nature & Wildlife Showcase",
      description = "3 majestic landscape & wildlife documentary video streams in full 1080p/720p HD.",
      icon = Icons.Default.Landscape,
      gradientColors = listOf(Color(0xFF065F46), Color(0xFF047857)),
      urls = listOf(
        "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
        "https://www.youtube.com/watch?v=kJQP7kiw5Fk",
        "https://www.youtube.com/watch?v=9bZkp7q19f0"
      ),
      formatNote = "3 Videos • ~68 MB estimated"
    ),
    BatchPreset(
      title = "Lo-Fi Beats & Chillhop Bundle",
      description = "Calming ambient study beats and relaxing instrumentals optimized for MP3 audio storage.",
      icon = Icons.Default.Headphones,
      gradientColors = listOf(Color(0xFF581C87), Color(0xFF7E22CE)),
      urls = listOf(
        "https://www.youtube.com/watch?v=jfKfPfyJRdk",
        "https://www.youtube.com/watch?v=5qap5aO4i9A",
        "https://www.youtube.com/watch?v=DWcJFNfaw9c"
      ),
      formatNote = "3 Audio Tracks • ~18 MB estimated"
    ),
    BatchPreset(
      title = "Tech Innovation & Future Gadgets",
      description = "High dynamic range tech hardware trailers and robotic engineering showcases.",
      icon = Icons.Default.Memory,
      gradientColors = listOf(Color(0xFF1E3A8A), Color(0xFF2563EB)),
      urls = listOf(
        "https://www.youtube.com/watch?v=3JZ_D3ELwOQ",
        "https://www.youtube.com/watch?v=ysz5S6PUM-U",
        "https://www.youtube.com/watch?v=jNQXAC9IVRw"
      ),
      formatNote = "3 Videos • ~52 MB estimated"
    ),
    BatchPreset(
      title = "Shorts & Viral Highlights",
      description = "Fast-paced short-form video clips designed for rapid batch downloading and offline replay.",
      icon = Icons.Default.MovieFilter,
      gradientColors = listOf(Color(0xFF831843), Color(0xFFBE185D)),
      urls = listOf(
        "https://www.youtube.com/shorts/aqz-KE-bpKQ",
        "https://www.youtube.com/shorts/kJQP7kiw5Fk"
      ),
      formatNote = "2 Shorts • ~28 MB estimated"
    )
  )

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("presets_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Screen Title
    item {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
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
            imageVector = Icons.Default.AutoAwesome,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "Batch Presets & Tools",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "One-tap sample packs & link inspector",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    // Link Inspector Tool Card
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
              text = "YouTube Link Inspector",
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
            },
            placeholder = { Text("Paste any YouTube URL to test...") },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("inspector_url_field"),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
              unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            )
          )

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                val videoId = YouTubeParser.extractVideoId(inspectorUrl)
                if (videoId != null) {
                  isInspecting = true
                  scope.launch {
                    val info = withContext(Dispatchers.IO) {
                      YouTubeParser.fetchVideoDetails(context, OkHttpClient(), inspectorUrl, "VIDEO_MP4")
                    }
                    inspectedTitle = info.title
                    inspectedChannel = info.channelTitle
                    inspectedThumb = info.thumbnailUrl
                    isInspecting = false
                  }
                }
              },
              enabled = inspectorUrl.isNotBlank() && !isInspecting,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("inspect_link_button")
            ) {
              Text(if (isInspecting) "Inspecting..." else "Analyze Link")
            }

            if (inspectedTitle != null) {
              Button(
                onClick = {
                  onDownloadPreset(listOf(inspectorUrl))
                },
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.secondary
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                  .weight(1f)
                  .testTag("download_inspected_button")
              ) {
                Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Download")
              }
            }
          }

          if (inspectedTitle != null) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                .padding(8.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              inspectedThumb?.let { thumb ->
                AsyncImage(
                  model = thumb,
                  contentDescription = null,
                  contentScale = ContentScale.Crop,
                  modifier = Modifier
                    .size(width = 64.dp, height = 44.dp)
                    .clip(RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(8.dp))
              }
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = inspectedTitle ?: "",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = inspectedChannel ?: "",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          }
        }
      }
    }

    // Section Header
    item {
      Text(
        text = "Curated Batch Demo Packs",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    }

    // Preset cards
    items(presets) { preset ->
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("preset_card_${preset.title.take(10).replace(" ", "_")}")
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(40.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(Brush.linearGradient(preset.gradientColors)),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = preset.icon,
                  contentDescription = null,
                  tint = Color.White,
                  modifier = Modifier.size(22.dp)
                )
              }

              Spacer(modifier = Modifier.width(12.dp))

              Column {
                Text(
                  text = preset.title,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                  text = preset.formatNote,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                  fontWeight = FontWeight.SemiBold
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = preset.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = { onDownloadPreset(preset.urls) },
            modifier = Modifier
              .fillMaxWidth()
              .height(44.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.primary
            )
          ) {
            Icon(
              imageVector = Icons.Default.CloudDownload,
              contentDescription = null,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Download This Batch (${preset.urls.size} items)", fontWeight = FontWeight.Bold)
          }
        }
      }
    }

    // Info card about Local File Storage
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
              text = "About Local File Storage",
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "All downloaded videos and MP3 audio files are stored in your device's persistent app media storage (`Movies/TubeBatch`). They remain playable offline without consuming cellular data and can be shared to any external app.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }
  }
}
