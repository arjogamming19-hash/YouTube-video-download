package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
  @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
  fun getAllDownloads(): Flow<List<DownloadItem>>

  @Query("SELECT * FROM downloads WHERE status != 'COMPLETED' ORDER BY createdAt DESC")
  fun getQueueDownloads(): Flow<List<DownloadItem>>

  @Query("SELECT * FROM downloads WHERE status = 'COMPLETED' ORDER BY completedAt DESC, createdAt DESC")
  fun getCompletedDownloads(): Flow<List<DownloadItem>>

  @Query("SELECT * FROM downloads WHERE id = :id LIMIT 1")
  suspend fun getById(id: Long): DownloadItem?

  @Query("SELECT * FROM downloads WHERE status = 'QUEUED' ORDER BY createdAt ASC")
  suspend fun getPendingQueue(): List<DownloadItem>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insert(item: DownloadItem): Long

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertAll(items: List<DownloadItem>): List<Long>

  @Update
  suspend fun update(item: DownloadItem)

  @Query("DELETE FROM downloads WHERE id = :id")
  suspend fun deleteById(id: Long)

  @Query("DELETE FROM downloads WHERE status = 'COMPLETED'")
  suspend fun deleteCompleted()

  @Query("DELETE FROM downloads WHERE status != 'COMPLETED'")
  suspend fun clearQueue()

  @Query("DELETE FROM downloads")
  suspend fun deleteAll()
}
