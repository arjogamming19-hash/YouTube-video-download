package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DownloadStatus
import com.example.ui.components.VideoPlayerDialog
import com.example.ui.screens.BatchQueueScreen
import com.example.ui.screens.LocalStorageScreen
import com.example.ui.screens.PresetsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.BatchDownloadViewModel
import com.example.ui.viewmodel.NavigationTab

class MainActivity : ComponentActivity() {

  private val viewModel: BatchDownloadViewModel by viewModels()

  @OptIn(ExperimentalMaterial3Api::class)
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
        val queueItems by viewModel.queueItems.collectAsStateWithLifecycle()
        val completedItems by viewModel.completedItems.collectAsStateWithLifecycle()
        val filteredStorageItems by viewModel.filteredStorageItems.collectAsStateWithLifecycle()
        val storageStats by viewModel.storageStats.collectAsStateWithLifecycle()
        val maxConcurrency by viewModel.maxConcurrency.collectAsStateWithLifecycle()
        val batchInputText by viewModel.batchInputText.collectAsStateWithLifecycle()
        val selectedFormat by viewModel.selectedFormat.collectAsStateWithLifecycle()
        val selectedQuality by viewModel.selectedQuality.collectAsStateWithLifecycle()
        val playingItem by viewModel.playingItem.collectAsStateWithLifecycle()
        val storageSearchQuery by viewModel.storageSearchQuery.collectAsStateWithLifecycle()
        val storageFilter by viewModel.storageFilter.collectAsStateWithLifecycle()
        val selectedStorageItemIds by viewModel.selectedStorageItemIds.collectAsStateWithLifecycle()
        val userNotice by viewModel.userNotice.collectAsStateWithLifecycle()

        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(userNotice) {
          userNotice?.let { notice ->
            snackbarHostState.showSnackbar(notice)
            viewModel.clearUserNotice()
          }
        }

        // BackHandler: If media player is open, close it; else if not on QUEUE tab, go to QUEUE tab
        BackHandler(enabled = playingItem != null || currentTab != NavigationTab.QUEUE) {
          if (playingItem != null) {
            viewModel.closePlayer()
          } else if (currentTab != NavigationTab.QUEUE) {
            viewModel.setTab(NavigationTab.QUEUE)
          }
        }

        Scaffold(
          modifier = Modifier
            .fillMaxSize()
            .testTag("main_scaffold"),
          snackbarHost = { SnackbarHost(snackbarHostState) },
          topBar = {
            TopAppBar(
              title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Box(
                    modifier = Modifier
                      .size(32.dp)
                      .clip(RoundedCornerShape(8.dp))
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
                      imageVector = Icons.Default.PlayCircle,
                      contentDescription = null,
                      tint = Color.White,
                      modifier = Modifier.size(20.dp)
                    )
                  }
                  Spacer(modifier = Modifier.width(10.dp))
                  Column {
                    Text(
                      text = "TubeBatch",
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Black,
                      letterSpacing = 0.5.sp
                    )
                    Text(
                      text = "Batch Video & Local Storage",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                  }
                }
              },
              actions = {
                // Active downloads indicator badge in top bar
                val activeDownloadsCount = queueItems.count {
                  it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.FETCHING_INFO
                }
                if (activeDownloadsCount > 0) {
                  Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(end = 12.dp)
                  ) {
                    Row(
                      verticalAlignment = Alignment.CenterVertically,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                      Box(
                        modifier = Modifier
                          .size(6.dp)
                          .clip(CircleShape)
                          .background(MaterialTheme.colorScheme.primary)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        text = "$activeDownloadsCount active",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                      )
                    }
                  }
                }
              },
              colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
              )
            )
          },
          bottomBar = {
            NavigationBar(
              containerColor = MaterialTheme.colorScheme.surface,
              tonalElevation = 6.dp
            ) {
              val activeCount = queueItems.count { it.status == DownloadStatus.DOWNLOADING }

              // 1. Batch Queue Tab
              NavigationBarItem(
                selected = currentTab == NavigationTab.QUEUE,
                onClick = { viewModel.setTab(NavigationTab.QUEUE) },
                icon = {
                  BadgedBox(
                    badge = {
                      if (activeCount > 0) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                          Text("$activeCount")
                        }
                      }
                    }
                  ) {
                    Icon(
                      imageVector = Icons.Default.CloudDownload,
                      contentDescription = "Batch Queue"
                    )
                  }
                },
                label = { Text("Batch Queue") },
                colors = NavigationBarItemDefaults.colors(
                  indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  selectedTextColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("tab_queue")
              )

              // 2. Local Storage Tab
              NavigationBarItem(
                selected = currentTab == NavigationTab.LOCAL_STORAGE,
                onClick = { viewModel.setTab(NavigationTab.LOCAL_STORAGE) },
                icon = {
                  BadgedBox(
                    badge = {
                      if (completedItems.isNotEmpty()) {
                        Badge(containerColor = MaterialTheme.colorScheme.secondary) {
                          Text("${completedItems.size}")
                        }
                      }
                    }
                  ) {
                    Icon(
                      imageVector = Icons.Default.Folder,
                      contentDescription = "Local Files"
                    )
                  }
                },
                label = { Text("Local Files") },
                colors = NavigationBarItemDefaults.colors(
                  indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                  selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                  selectedTextColor = MaterialTheme.colorScheme.secondary
                ),
                modifier = Modifier.testTag("tab_storage")
              )

              // 3. Presets & Tools Tab
              NavigationBarItem(
                selected = currentTab == NavigationTab.PRESETS,
                onClick = { viewModel.setTab(NavigationTab.PRESETS) },
                icon = {
                  Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = "Presets & Tools"
                  )
                },
                label = { Text("Demo Packs") },
                colors = NavigationBarItemDefaults.colors(
                  indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  selectedTextColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("tab_presets")
              )
            }
          }
        ) { innerPadding ->
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
          ) {
            when (currentTab) {
              NavigationTab.QUEUE -> {
                BatchQueueScreen(
                  queueItems = queueItems,
                  inputText = batchInputText,
                  onInputChanged = viewModel::updateBatchInput,
                  selectedFormat = selectedFormat,
                  onFormatSelected = viewModel::setFormat,
                  selectedQuality = selectedQuality,
                  onQualitySelected = viewModel::setQuality,
                  maxConcurrency = maxConcurrency,
                  onConcurrencySelected = viewModel::setMaxConcurrency,
                  onStartBatch = { viewModel.startBatchDownload() },
                  onPauseItem = viewModel::pauseItem,
                  onResumeItem = viewModel::resumeItem,
                  onCancelItem = viewModel::cancelItem,
                  onRetryItem = viewModel::retryItem,
                  onPauseAll = viewModel::pauseAll,
                  onResumeAll = viewModel::resumeAll,
                  onClearQueue = viewModel::clearQueue,
                  onLoadPresetBatch = { urls ->
                    viewModel.startBatchDownload(urls)
                  }
                )
              }
              NavigationTab.LOCAL_STORAGE -> {
                LocalStorageScreen(
                  storageStats = storageStats,
                  filteredItems = filteredStorageItems,
                  searchQuery = storageSearchQuery,
                  onSearchQueryChanged = viewModel::updateStorageSearch,
                  storageFilter = storageFilter,
                  onFilterChanged = viewModel::setStorageFilter,
                  selectedItemIds = selectedStorageItemIds,
                  onToggleSelect = viewModel::toggleSelectStorageItem,
                  onSelectAll = viewModel::selectAllStorageItems,
                  onClearSelection = viewModel::clearStorageSelection,
                  onDeleteSelected = viewModel::deleteSelectedStorageItems,
                  onPlayMedia = viewModel::playMedia,
                  onDeleteSingleItem = viewModel::deleteSingleCompleted,
                  onClearAllFiles = viewModel::clearAllFiles,
                  onNavigateToBatch = { viewModel.setTab(NavigationTab.QUEUE) }
                )
              }
              NavigationTab.PRESETS -> {
                PresetsScreen(
                  onDownloadPreset = { urls ->
                    viewModel.startBatchDownload(urls)
                  }
                )
              }
            }

            // Embedded Media Player Preview Dialog
            playingItem?.let { item ->
              VideoPlayerDialog(
                item = item,
                onDismiss = viewModel::closePlayer
              )
            }
          }
        }
      }
    }
  }
}
