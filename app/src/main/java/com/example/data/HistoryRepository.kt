package com.example.data

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val dao: HistoryDao) {
    val allHistory: Flow<List<HistoryEntity>> = dao.getAllHistory()

    suspend fun addEntry(mode: String, exprRaw: String, result: String, note: String = "") {
        val entry = HistoryEntity(
            id = "${System.currentTimeMillis()}-${(1000..9999).random()}",
            timestamp = System.currentTimeMillis(),
            mode = mode,
            exprRaw = exprRaw,
            result = result,
            note = note
        )
        dao.insert(entry)
        // Automatically prune entries older than 365 days
        val oneYearAgo = System.currentTimeMillis() - (365L * 24 * 60 * 60 * 1000)
        dao.pruneOlderThan(oneYearAgo)
    }

    suspend fun updateNote(id: String, note: String) {
        dao.updateNote(id, note)
    }

    suspend fun deleteEntry(id: String) {
        dao.deleteById(id)
    }

    suspend fun clearHistory() {
        dao.clearAll()
    }

    suspend fun insertAll(entries: List<HistoryEntity>) {
        dao.insertAll(entries)
    }
}
