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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import com.example.data.network.StorageStats
import com.example.ui.components.LocalStorageItemCard
import com.example.ui.components.StorageStatsCard

@Composable
fun LocalStorageScreen(
  storageStats: StorageStats,
  filteredItems: List<DownloadItem>,
  searchQuery: String,
  onSearchQueryChanged: (String) -> Unit,
  storageFilter: String,
  onFilterChanged: (String) -> Unit,
  selectedItemIds: Set<Long>,
  onToggleSelect: (Long) -> Unit,
  onSelectAll: (List<DownloadItem>) -> Unit,
  onClearSelection: () -> Unit,
  onDeleteSelected: () -> Unit,
  onPlayMedia: (DownloadItem) -> Unit,
  onDeleteSingleItem: (DownloadItem) -> Unit,
  onClearAllFiles: () -> Unit,
  onNavigateToBatch: () -> Unit
) {
  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .testTag("local_storage_screen"),
    contentPadding = PaddingValues(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Storage Stats Card
    item {
      StorageStatsCard(
        stats = storageStats,
        onClearAll = onClearAllFiles
      )
    }

    // 2. Search & Filter Bar
    item {
      Column(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChanged,
          placeholder = {
            Text(
              "Search downloaded files...",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.Search,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
          },
          trailingIcon = {
            if (searchQuery.isNotBlank()) {
              IconButton(onClick = { onSearchQueryChanged("") }) {
                Icon(
                  imageVector = Icons.Default.Clear,
                  contentDescription = "Clear Search",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
              }
            }
          },
          singleLine = true,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
          ),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("storage_search_field")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter chips (All, Videos, Audio)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          listOf(
            "ALL" to "All Files",
            "VIDEO" to "Videos (MP4)",
            "AUDIO" to "Audio (MP3)"
          ).forEach { (key, label) ->
            FilterChip(
              selected = storageFilter == key,
              onClick = { onFilterChanged(key) },
              label = { Text(label, fontSize = 12.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
              ),
              modifier = Modifier.testTag("filter_chip_$key")
            )
          }
        }
      }
    }

    // 3. Batch Selection Actions Bar (if items exist)
    if (filteredItems.isNotEmpty()) {
      item {
        AnimatedVisibility(visible = selectedItemIds.isNotEmpty()) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier
              .fillMaxWidth()
              .testTag("selection_actions_bar")
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "${selectedItemIds.size} selected",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )

              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onClearSelection) {
                  Text("Cancel", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }

                Button(
                  onClick = onDeleteSelected,
                  colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                  ),
                  shape = RoundedCornerShape(8.dp),
                  contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                  modifier = Modifier.testTag("delete_selected_button")
                ) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                  )
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Delete", fontSize = 12.sp)
                }
              }
            }
          }
        }
      }

      // Selection Header (Select All)
      item {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "Downloaded Media (${filteredItems.size})",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )

          if (selectedItemIds.size < filteredItems.size) {
            TextButton(
              onClick = { onSelectAll(filteredItems) },
              modifier = Modifier.testTag("select_all_storage_button")
            ) {
              Icon(
                imageVector = Icons.Default.SelectAll,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Select All", fontSize = 12.sp)
            }
          } else {
            TextButton(onClick = onClearSelection) {
              Text("Deselect All", fontSize = 12.sp)
            }
          }
        }
      }

      // 4. Downloaded Media Items List
      items(filteredItems, key = { it.id }) { item ->
        LocalStorageItemCard(
          item = item,
          isSelected = selectedItemIds.contains(item.id),
          onToggleSelect = onToggleSelect,
          onPlay = onPlayMedia,
          onDelete = onDeleteSingleItem
        )
      }
    } else {
      // Empty Storage State
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .testTag("empty_storage_card"),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
          )
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(32.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = if (searchQuery.isNotBlank()) "No Matching Files" else "No Local Files Downloaded Yet",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = if (searchQuery.isNotBlank())
                "Try searching for another video title or clear your search filter."
              else
                "Videos and audio downloaded in TubeBatch are saved directly to your device local storage for offline playback and file sharing.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center
            )

            if (searchQuery.isBlank()) {
              Spacer(modifier = Modifier.height(18.dp))

              Button(
                onClick = onNavigateToBatch,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                  containerColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("go_to_batch_button")
              ) {
                Text("Start Batch Download", fontSize = 13.sp)
              }
            }
          }
        }
      }
    }
  }
}
