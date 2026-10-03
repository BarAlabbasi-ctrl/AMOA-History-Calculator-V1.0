package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history_entries ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<HistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HistoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entries: List<HistoryEntity>)

    @Query("UPDATE history_entries SET note = :note WHERE id = :id")
    suspend fun updateNote(id: String, note: String)

    @Query("DELETE FROM history_entries WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM history_entries")
    suspend fun clearAll()

    @Query("DELETE FROM history_entries WHERE timestamp < :cutoffTimestamp")
    suspend fun pruneOlderThan(cutoffTimestamp: Long)
}
