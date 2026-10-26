package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SmartDisplay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.local.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.network.VideoDownloadManager
import com.example.ui.theme.ErrorRed
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.WarningAmber

@Composable
fun DownloadItemCard(
  item: DownloadItem,
  onPause: (Long) -> Unit,
  onResume: (Long) -> Unit,
  onCancel: (Long) -> Unit,
  onRetry: (Long) -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("download_item_${item.id}"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surface
    ),
    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Thumbnail box
        Box(
          modifier = Modifier
            .size(width = 80.dp, height = 55.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
          contentAlignment = Alignment.Center
        ) {
          if (item.thumbnailUrl.isNotBlank()) {
            AsyncImage(
              model = item.thumbnailUrl,
              contentDescription = "Thumbnail",
              contentScale = ContentScale.Crop,
              modifier = Modifier.matchParentSize()
            )
          } else {
            Icon(
              imageVector = Icons.Default.SmartDisplay,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          // Format pill on bottom right of thumbnail
          Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color.Black.copy(alpha = 0.75f),
            modifier = Modifier
              .align(Alignment.BottomEnd)
              .padding(3.dp)
          ) {
            Text(
              text = if (item.formatType.contains("AUDIO")) "MP3" else "MP4",
              color = Color.White,
              fontSize = 9.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
            )
          }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Title and channel info
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = item.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = "${item.channelTitle} • ${item.targetQuality}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
          )

          Spacer(modifier = Modifier.height(4.dp))

          // Status & Original Stream Badge
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            StatusPill(status = item.status, errorMessage = item.errorMessage)

            if (item.isOriginalStream) {
              Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
              ) {
                Text(
                  text = "Original YT Stream",
                  color = MaterialTheme.colorScheme.primary,
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                )
              }
            }
          }
        }

        // Action Buttons
        Row(verticalAlignment = Alignment.CenterVertically) {
          when (item.status) {
            DownloadStatus.DOWNLOADING -> {
              IconButton(
                onClick = { onPause(item.id) },
                modifier = Modifier
                  .size(36.dp)
                  .testTag("pause_button_${item.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.Pause,
                  contentDescription = "Pause",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            DownloadStatus.PAUSED -> {
              IconButton(
                onClick = { onResume(item.id) },
                modifier = Modifier
                  .size(36.dp)
                  .testTag("resume_button_${item.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.PlayArrow,
                  contentDescription = "Resume",
                  tint = SkyBlue,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            DownloadStatus.FAILED -> {
              IconButton(
                onClick = { onRetry(item.id) },
                modifier = Modifier
                  .size(36.dp)
                  .testTag("retry_button_${item.id}")
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Retry",
                  tint = WarningAmber,
                  modifier = Modifier.size(20.dp)
                )
              }
            }
            else -> {}
          }

          IconButton(
            onClick = { onCancel(item.id) },
            modifier = Modifier
              .size(36.dp)
              .testTag("cancel_button_${item.id}")
          ) {
            Icon(
              imageVector = Icons.Default.Close,
              contentDescription = "Cancel",
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }

      // Progress bar and details if actively downloading or in queue
      if (item.status == DownloadStatus.DOWNLOADING || item.status == DownloadStatus.PAUSED || item.status == DownloadStatus.FETCHING_INFO) {
        Spacer(modifier = Modifier.height(8.dp))

        if (item.status == DownloadStatus.FETCHING_INFO) {
          LinearProgressIndicator(
            modifier = Modifier
              .fillMaxWidth()
              .height(4.dp)
              .clip(RoundedCornerShape(2.dp)),
            color = MaterialTheme.colorScheme.primary
          )
        } else {
          LinearProgressIndicator(
            progress = { item.progress },
            modifier = Modifier
              .fillMaxWidth()
              .height(5.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = if (item.status == DownloadStatus.PAUSED) WarningAmber else MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Progress text and speed
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          val progressPct = (item.progress * 100).toInt()
          Text(
            text = "${VideoDownloadManager.formatBytes(item.downloadedBytes)} / ${VideoDownloadManager.formatBytes(item.totalBytes)} ($progressPct%)",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
          )

          Text(
            text = if (item.status == DownloadStatus.PAUSED) "Paused" else item.downloadSpeed,
            style = MaterialTheme.typography.labelSmall,
            color = if (item.status == DownloadStatus.PAUSED) WarningAmber else SkyBlue,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp
          )
        }
      }
    }
  }
}

@Composable
fun StatusPill(status: DownloadStatus, errorMessage: String?) {
  val (bgColor, textColor, text) = when (status) {
    DownloadStatus.QUEUED -> Triple(WarningAmber.copy(alpha = 0.15f), WarningAmber, "In Queue")
    DownloadStatus.FETCHING_INFO -> Triple(SkyBlue.copy(alpha = 0.15f), SkyBlue, "Analyzing...")
    DownloadStatus.DOWNLOADING -> Triple(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), MaterialTheme.colorScheme.primary, "Downloading")
    DownloadStatus.PAUSED -> Triple(Color.Gray.copy(alpha = 0.2f), Color.LightGray, "Paused")
    DownloadStatus.COMPLETED -> Triple(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f), MaterialTheme.colorScheme.primary, "Completed")
    DownloadStatus.FAILED -> Triple(ErrorRed.copy(alpha = 0.15f), ErrorRed, errorMessage?.take(24) ?: "Failed")
    DownloadStatus.CANCELED -> Triple(Color.DarkGray, Color.Gray, "Canceled")
  }

  Surface(
    shape = RoundedCornerShape(4.dp),
    color = bgColor
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
      if (status == DownloadStatus.QUEUED) {
        Icon(
          imageVector = Icons.Default.HourglassEmpty,
          contentDescription = null,
          modifier = Modifier.size(10.dp),
          tint = textColor
        )
        Spacer(modifier = Modifier.width(3.dp))
      }
      Text(
        text = text,
        color = textColor,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
      )
    }
  }
}
