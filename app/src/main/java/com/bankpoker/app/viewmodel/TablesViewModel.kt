package com.bankpoker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.repository.PokerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class TablesViewModel(
    private val repository: PokerRepository
) : ViewModel() {

    val tables: Flow<List<PokerTable>> = repository.getQuickTables().map { list ->
        list.sortedWith(
            compareByDescending<PokerTable> { it.status == "ACTIVE" }
                .thenByDescending { it.createdAt }
        )
    }

    private val _lastChipValue = MutableStateFlow<Long?>(null)
    val lastChipValue: StateFlow<Long?> = _lastChipValue.asStateFlow()

    private val _refreshTrigger = MutableStateFlow(0)
    private val _playerCounts = MutableStateFlow<Map<String, Int>>(emptyMap())
    val playerCounts: StateFlow<Map<String, Int>> = _playerCounts.asStateFlow()

    init {
        viewModelScope.launch {
            tables.collect { tablesList ->
                if (tablesList.isNotEmpty()) {
                    _lastChipValue.value = tablesList.firstOrNull()?.chipValue
                }
                recomputePlayerCounts(tablesList)
            }
        }

        viewModelScope.launch {
            _refreshTrigger.collect {
                val tablesList = repository.getAllTablesOnce()
                recomputePlayerCounts(tablesList)
            }
        }
    }

    private suspend fun recomputePlayerCounts(tablesList: List<PokerTable>) {
        val counts = mutableMapOf<String, Int>()
        for (table in tablesList) {
            counts[table.id] = repository.getPlayingPlayersCount(table.id)
        }
        _playerCounts.value = counts
    }

    fun refreshPlayerCounts() {
        _refreshTrigger.value += 1
    }

    fun createTable(name: String, chipValue: Long?, hasEntryFee: Boolean = false, entryFee: Long? = null) {
        viewModelScope.launch {
            repository.createTable(
                name = name,
                chipValue = chipValue,
                hasEntryFee = hasEntryFee,
                entryFee = entryFee
            )
            _lastChipValue.value = chipValue
        }
    }

    fun updateTable(tableId: String, name: String, chipValue: Long?, hasEntryFee: Boolean, entryFee: Long?) {
        viewModelScope.launch {
            repository.updateTable(tableId, name, chipValue, hasEntryFee, entryFee)
        }
    }

    fun deleteTable(tableId: String) {
        viewModelScope.launch {
            repository.deleteTableCascade(tableId)
        }
    }

    suspend fun getTableDetailsCount(tableId: String): Pair<Int, Int> {
        return repository.getTableDetailsCount(tableId)
    }

    suspend fun exportBackup(): String = repository.exportBackupJson()

    suspend fun restoreBackup(json: String) = repository.restoreBackupJson(json)
}