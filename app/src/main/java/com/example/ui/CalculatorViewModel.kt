package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HistoryEntity
import com.example.data.HistoryRepository
import com.example.ui.components.AppTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CalculatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HistoryRepository
    val historyList: StateFlow<List<HistoryEntity>>

    private val _currentTab = MutableStateFlow(AppTab.BASIC)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _isFunctionsOpen = MutableStateFlow(false)
    val isFunctionsOpen: StateFlow<Boolean> = _isFunctionsOpen.asStateFlow()

    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _showAboutDialog = MutableStateFlow(false)
    val showAboutDialog: StateFlow<Boolean> = _showAboutDialog.asStateFlow()

    private val _loadedSciExpr = MutableStateFlow<String?>(null)
    val loadedSciExpr: StateFlow<String?> = _loadedSciExpr.asStateFlow()

    init {
        val db = AppDatabase.getDatabase(application)
        repository = HistoryRepository(db.historyDao())
        historyList = repository.allHistory.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
    }

    fun selectTab(tab: AppTab) {
        _currentTab.value = tab
        if (tab == AppTab.FUNCTIONS) {
            _isFunctionsOpen.value = true
        } else if (tab == AppTab.BASIC) {
            _isFunctionsOpen.value = false
        }
    }

    fun toggleFunctions(open: Boolean) {
        _isFunctionsOpen.value = open
    }

    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun setAboutDialogVisible(visible: Boolean) {
        _showAboutDialog.value = visible
    }

    fun loadExpressionForCalc(exprRaw: String) {
        _loadedSciExpr.value = exprRaw
        _currentTab.value = AppTab.BASIC
        _isFunctionsOpen.value = false
    }

    fun addHistory(mode: String, exprRaw: String, result: String, note: String = "") {
        viewModelScope.launch {
            repository.addEntry(mode, exprRaw, result, note)
        }
    }

    fun updateNote(id: String, note: String) {
        viewModelScope.launch {
            repository.updateNote(id, note)
        }
    }

    fun deleteEntry(id: String) {
        viewModelScope.launch {
            repository.deleteEntry(id)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun importEntries(entries: List<HistoryEntity>) {
        viewModelScope.launch {
            repository.insertAll(entries)
        }
    }
}
