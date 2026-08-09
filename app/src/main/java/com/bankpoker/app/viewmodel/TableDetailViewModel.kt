package com.bankpoker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankpoker.app.data.local.entity.BuyIn
import com.bankpoker.app.data.local.entity.ExitRecord
import com.bankpoker.app.data.local.entity.Player
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.repository.PokerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TableDetailViewModel(
    private val repository: PokerRepository,
    private val tableId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(TableDetailUiState())
    val uiState: StateFlow<TableDetailUiState> = _uiState.asStateFlow()

    val players: Flow<List<Player>> = repository.getPlayersByTableId(tableId)
    val buyIns: Flow<List<BuyIn>> = repository.getBuyInsByTableId(tableId)
    val exitRecords: Flow<List<ExitRecord>> = repository.getExitRecordsByTableId(tableId)
    val savedPlayerNames: Flow<List<String>> = MutableStateFlow(emptyList()).also { flow ->
        viewModelScope.launch {
            val names = repository.getAllSavedPlayerNames()
            (flow as MutableStateFlow).value = names
        }
    }

    init {
        loadTableData()
    }

    private fun loadTableData() {
        viewModelScope.launch {
            val table = repository.getTableById(tableId)
            val totalBuyIns = repository.getTotalBuyInsForTable(tableId)
            val totalExits = repository.getTotalExitsForTable(tableId)
            _uiState.value = _uiState.value.copy(
                table = table,
                totalBuyIns = totalBuyIns,
                totalExits = totalExits,
                remainingBalance = totalBuyIns - totalExits
            )
        }
    }

    fun addPlayer(name: String) {
        viewModelScope.launch {
            repository.addPlayer(tableId, name.trim().uppercase())
        }
    }

    fun addBuyIn(playerId: String, amount: Long, note: String?) {
        viewModelScope.launch {
            repository.addBuyIn(tableId, playerId, amount, note)
            loadTableData()
        }
    }

    fun addExitRecord(playerId: String, amount: Long, note: String?) {
        viewModelScope.launch {
            repository.addExitRecord(tableId, playerId, amount, note)
            loadTableData()
        }
    }

    fun closeTable() {
        viewModelScope.launch {
            repository.closeTable(tableId)
            loadTableData()
        }
    }

    suspend fun getPlayerTotalBuyIns(playerId: String): Long {
        return repository.getTotalBuyInsForPlayer(playerId)
    }

    suspend fun getPlayerTotalExits(playerId: String): Long {
        return repository.getTotalExitsForPlayer(playerId)
    }

    suspend fun getPlayingPlayersCount(): Int {
        return repository.getPlayingPlayersCount(tableId)
    }

    fun updateBuyIn(buyIn: BuyIn) {
        viewModelScope.launch {
            repository.updateBuyIn(buyIn)
            loadTableData()
        }
    }

    fun deleteBuyIn(buyIn: BuyIn) {
        viewModelScope.launch {
            repository.deleteBuyIn(buyIn)
            loadTableData()
        }
    }

    fun updateExitRecord(exitRecord: ExitRecord) {
        viewModelScope.launch {
            repository.updateExitRecord(exitRecord)
            loadTableData()
        }
    }

    fun deleteExitRecord(exitRecord: ExitRecord, playerId: String) {
        viewModelScope.launch {
            repository.deleteExitRecord(exitRecord)
            // Check if player has any remaining exit records
            val exitCount = repository.getExitCountByPlayer(playerId)
            if (exitCount == 0) {
                // Set player status back to PLAYING
                repository.updatePlayerStatus(playerId, "PLAYING")
            }
            loadTableData()
        }
    }
}

data class TableDetailUiState(
    val table: PokerTable? = null,
    val totalBuyIns: Long = 0L,
    val totalExits: Long = 0L,
    val remainingBalance: Long = 0L
)