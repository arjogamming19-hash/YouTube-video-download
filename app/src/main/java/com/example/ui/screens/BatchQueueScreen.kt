package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.DownloadItem
import com.example.data.model.DownloadStatus
import com.example.data.model.FormatType
import com.example.ui.components.BatchInputCard
import com.example.ui.components.DownloadItemCard

@Composable
fun BatchQueueScreen(
  queueItems: List<DownloadItem>,
  inputText: String,
  onInputChanged: (String) -> Unit,
  selectedFormat: FormatType,
  onFormatSelected: (FormatType) -> Unit,
  selectedQuality: String,
  onQualitySelected: (String) -> Unit,
  maxConcurrency: Int,
  onConcurrencySelected: (Int) -> Unit,
  onStartBatch: () -> Unit,
  onPauseItem: (Long) -> Unit,
  onResumeItem: (Long) -> Unit,
  onCancelItem: (Long) -> Unit,
  onRetryItem: (Long) -> Unit,
  onPauseAll: () -> Unit,
  onResumeAll: () -> Unit,
  onClearQueue: () -> Unit,
  onLoadPresetBatch: (List<String>) -> Unit
) {
  val activeCount = queueItems.count { it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.FETCHING_INFO }
  val pausedCount = queueItems.count { it.status == DownloadStatus.PAUSED }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("batch_queue_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Batch Input Card
    item {
      BatchInputCard(
        inputText = inputText,
        onInputChanged = onInputChanged,
        selectedFormat = selectedFormat,
        onFormatSelected = onFormatSelected,
        selectedQuality = selectedQuality,
        onQualitySelected = onQualitySelected,
        maxConcurrency = maxConcurrency,
        onConcurrencySelected = onConcurrencySelected,
        onStartBatch = onStartBatch
      )
    }

    // 2. Queue Header and Control Actions
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "Active Download Queue",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "${queueItems.size} item${if (queueItems.size != 1) "s" else ""} • $activeCount active",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (queueItems.isNotEmpty()) {
          Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (activeCount > 0) {
              TextButton(
                onClick = onPauseAll,
                modifier = Modifier.testTag("pause_all_button")
              ) {
                Icon(
                  imageVector = Icons.Default.PauseCircle,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Pause", fontSize = 12.sp)
              }
            } else if (pausedCount > 0) {
              TextButton(
                onClick = onResumeAll,
                modifier = Modifier.testTag("resume_all_button")
              ) {
                Icon(
                  imageVector = Icons.Default.PlayCircle,
                  contentDescription = null,
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Resume", fontSize = 12.sp)
              }
            }

            TextButton(
              onClick = onClearQueue,
              modifier = Modifier.testTag("clear_queue_button")
            ) {
              Icon(
                imageVector = Icons.Default.ClearAll,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Clear", fontSize = 12.sp)
            }
          }
        }
      }
    }

    // 3. Queue Items List
    if (queueItems.isNotEmpty()) {
      items(queueItems, key = { it.id }) { item ->
        DownloadItemCard(
          item = item,
          onPause = onPauseItem,
          onResume = onResumeItem,
          onCancel = onCancelItem,
          onRetry = onRetryItem
        )
      }
    } else {
      // Empty Queue State
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_queue_card"),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
          )
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.DownloadDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(28.dp)
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "Download Queue is Empty",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = "Paste multiple YouTube URLs in the box above or load a quick demo batch to test multi-file downloading immediately.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
              onClick = {
                onLoadPresetBatch(
                  listOf(
                    "https://www.youtube.com/watch?v=aqz-KE-bpKQ",
                    "https://www.youtube.com/watch?v=kJQP7kiw5Fk",
                    "https://www.youtube.com/watch?v=9bZkp7q19f0"
                  )
                )
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("load_demo_batch_button")
            ) {
              Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("Load Demo Batch (3 Videos)", fontSize = 13.sp)
            }
          }
        }
      }
    }
  }
}
