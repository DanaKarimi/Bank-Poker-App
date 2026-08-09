package com.bankpoker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.repository.PokerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class TablesViewModel(
    private val repository: PokerRepository
) : ViewModel() {

    val tables: Flow<List<PokerTable>> = repository.getAllTables()

    fun createTable(name: String, chipValue: Long?) {
        viewModelScope.launch {
            repository.createTable(name, chipValue)
        }
    }
}
