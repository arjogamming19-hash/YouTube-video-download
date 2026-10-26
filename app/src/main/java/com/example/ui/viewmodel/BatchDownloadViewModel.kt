package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DownloadItem
import com.example.data.model.FormatType
import com.example.data.network.StorageStats
import com.example.data.network.YouTubeParser
import com.example.data.repository.DownloadRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class NavigationTab {
  QUEUE,
  LOCAL_STORAGE,
  PRESETS
}

class BatchDownloadViewModel(application: Application) : AndroidViewModel(application) {

  private val database = AppDatabase.getDatabase(application)
  private val repository = DownloadRepository(application, database.downloadDao())

  val queueItems: StateFlow<List<DownloadItem>> = repository.queueDownloads
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val completedItems: StateFlow<List<DownloadItem>> = repository.completedDownloads
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val storageStats: StateFlow<StorageStats> = repository.storageStats

  val maxConcurrency: StateFlow<Int> = repository.maxConcurrency

  private val _currentTab = MutableStateFlow(NavigationTab.QUEUE)
  val currentTab: StateFlow<NavigationTab> = _currentTab.asStateFlow()

  private val _batchInputText = MutableStateFlow("")
  val batchInputText: StateFlow<String> = _batchInputText.asStateFlow()

  private val _selectedFormat = MutableStateFlow(FormatType.VIDEO_MP4)
  val selectedFormat: StateFlow<FormatType> = _selectedFormat.asStateFlow()

  private val _selectedQuality = MutableStateFlow("720p HD")
  val selectedQuality: StateFlow<String> = _selectedQuality.asStateFlow()

  private val _playingItem = MutableStateFlow<DownloadItem?>(null)
  val playingItem: StateFlow<DownloadItem?> = _playingItem.asStateFlow()

  private val _storageSearchQuery = MutableStateFlow("")
  val storageSearchQuery: StateFlow<String> = _storageSearchQuery.asStateFlow()

  private val _storageFilter = MutableStateFlow("ALL")
  val storageFilter: StateFlow<String> = _storageFilter.asStateFlow()

  private val _selectedStorageItemIds = MutableStateFlow<Set<Long>>(emptySet())
  val selectedStorageItemIds: StateFlow<Set<Long>> = _selectedStorageItemIds.asStateFlow()

  private val _userNotice = MutableStateFlow<String?>(null)
  val userNotice: StateFlow<String?> = _userNotice.asStateFlow()

  // Filtered completed items for local storage screen
  val filteredStorageItems: StateFlow<List<DownloadItem>> = combine(
    completedItems,
    _storageSearchQuery,
    _storageFilter
  ) { items, query, filter ->
    items.filter { item ->
      val matchesQuery = query.isBlank() ||
        item.title.contains(query, ignoreCase = true) ||
        item.channelTitle.contains(query, ignoreCase = true)

      val matchesFilter = when (filter) {
        "VIDEO" -> item.formatType.contains("VIDEO")
        "AUDIO" -> item.formatType.contains("AUDIO")
        else -> true
      }
      matchesQuery && matchesFilter
    }
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  fun setTab(tab: NavigationTab) {
    _currentTab.value = tab
    if (tab == NavigationTab.LOCAL_STORAGE) {
      repository.refreshStorageStats()
    }
  }

  fun updateBatchInput(text: String) {
    _batchInputText.value = text
  }

  fun setFormat(format: FormatType) {
    _selectedFormat.value = format
    if (format != FormatType.VIDEO_MP4 && !_selectedQuality.value.contains("kbps")) {
      _selectedQuality.value = "320 kbps"
    } else if (format == FormatType.VIDEO_MP4 && _selectedQuality.value.contains("kbps")) {
      _selectedQuality.value = "720p HD"
    }
  }

  fun setQuality(quality: String) {
    _selectedQuality.value = quality
  }

  fun setMaxConcurrency(concurrency: Int) {
    repository.setMaxConcurrency(concurrency)
  }

  fun startBatchDownload(customUrls: List<String>? = null) {
    viewModelScope.launch {
      val rawInput = customUrls?.joinToString("\n") ?: _batchInputText.value
      val extractedUrls = YouTubeParser.extractBatchUrls(rawInput)

      if (extractedUrls.isEmpty()) {
        _userNotice.value = "Please enter at least one valid YouTube URL or video ID."
        return@launch
      }

      val addedCount = repository.addBatch(
        urls = extractedUrls,
        formatType = _selectedFormat.value.name,
        quality = _selectedQuality.value
      )

      if (customUrls == null) {
        _batchInputText.value = ""
      }
      _userNotice.value = "Added $addedCount item(s) to batch queue."
      _currentTab.value = NavigationTab.QUEUE
    }
  }

  fun pauseItem(id: Long) = repository.pauseItem(id)
  fun resumeItem(id: Long) = repository.resumeItem(id)
  fun cancelItem(id: Long) = repository.cancelItem(id)
  fun retryItem(id: Long) = repository.retryItem(id)

  fun pauseAll() = repository.pauseAll()
  fun resumeAll() = repository.resumeAll()
  fun clearQueue() = repository.clearQueue()

  fun playMedia(item: DownloadItem) {
    _playingItem.value = item
  }

  fun closePlayer() {
    _playingItem.value = null
  }

  fun updateStorageSearch(query: String) {
    _storageSearchQuery.value = query
  }

  fun setStorageFilter(filter: String) {
    _storageFilter.value = filter
  }

  fun toggleSelectStorageItem(id: Long) {
    val current = _selectedStorageItemIds.value.toMutableSet()
    if (current.contains(id)) {
      current.remove(id)
    } else {
      current.add(id)
    }
    _selectedStorageItemIds.value = current
  }

  fun selectAllStorageItems(items: List<DownloadItem>) {
    _selectedStorageItemIds.value = items.map { it.id }.toSet()
  }

  fun clearStorageSelection() {
    _selectedStorageItemIds.value = emptySet()
  }

  fun deleteSingleCompleted(item: DownloadItem) {
    repository.deleteCompletedItem(item, removeFileFromDisk = true)
    _userNotice.value = "Deleted '${item.title.take(30)}' from local storage."
  }

  fun deleteSelectedStorageItems() {
    val selectedIds = _selectedStorageItemIds.value
    val items = completedItems.value.filter { it.id in selectedIds }
    items.forEach { item ->
      repository.deleteCompletedItem(item, removeFileFromDisk = true)
    }
    clearStorageSelection()
    _userNotice.value = "Deleted ${items.size} file(s) from local storage."
  }

  fun clearAllFiles() {
    repository.clearAllCompletedFiles()
    clearStorageSelection()
    _userNotice.value = "Cleared all downloaded local files."
  }

  fun clearUserNotice() {
    _userNotice.value = null
  }
}
