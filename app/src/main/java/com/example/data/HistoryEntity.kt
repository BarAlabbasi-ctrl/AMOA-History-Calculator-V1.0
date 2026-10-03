package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "history_entries")
data class HistoryEntity(
    @PrimaryKey
    val id: String,
    val timestamp: Long,
    val mode: String, // "Scientific", "Length", "Time"
    val exprRaw: String,
    val result: String,
    val note: String = ""
)
