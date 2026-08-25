package com.bankpoker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.repository.PokerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

    val playerCounts: StateFlow<Map<String, Int>> = 
        combine(tables, repository.getAllTables()) { _, tablesList ->
            tablesList.associate { table ->
                table.id to repository.getPlayingPlayersCount(table.id)
            }
        }.map { counts -> counts }
        .asStateFlow()

    init {
        viewModelScope.launch {
            tables.collect { tablesList ->
                if (tablesList.isNotEmpty()) {
                    _lastChipValue.value = tablesList.firstOrNull()?.chipValue
                }
            }
        }
    }

    fun createTable(name: String, chipValue: Long?) {
        viewModelScope.launch {
            repository.createTable(name, chipValue)
            _lastChipValue.value = chipValue
        }
    }

    fun deleteTable(tableId: String) {
        viewModelScope.launch {
            repository.deleteTableAndRelatedData(tableId)
        }
    }
}
