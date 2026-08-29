package com.bankpoker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.data.local.entity.UnpaidEntryFeeInfo
import com.bankpoker.app.repository.PokerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerStats(
    val name: String,
    val gamesPlayed: Int,
    val totalBuyIns: Long,
    val totalExits: Long,
    val netResult: Long
)

data class TableStats(
    val table: PokerTable,
    val playerCount: Int,
    val totalBuyIns: Long,
    val totalExits: Long,
    val topWinnerName: String?,
    val topWinnerNet: Long
)

data class StatsUiState(
    val totalTables: Int = 0,
    val closedTables: Int = 0,
    val totalTransactions: Int = 0,
    val distinctPlayers: Int = 0,
    val biggestWinner: PlayerStats? = null,
    val mostActive: PlayerStats? = null,
    val playerStats: List<PlayerStats> = emptyList(),
    val closedTableStats: List<TableStats> = emptyList()
)

class StatsViewModel(private val repository: PokerRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    val unpaidEntryFeeDebtors: Flow<List<UnpaidEntryFeeInfo>> = repository.getUnpaidEntryFeeDebtors()

    fun markEntryFeePaid(playerId: String) {
        viewModelScope.launch {
            repository.toggleEntryFee(playerId, true)
        }
    }


    init {
        viewModelScope.launch {
            val tables = repository.getAllTablesOnce()
            val players = repository.getAllPlayersOnce()
            val buyIns = repository.getAllBuyInsOnce()
            val exitRecords = repository.getAllExitRecordsOnce()

            val buyInByPlayer = buyIns.groupBy { it.playerId }
                .mapValues { entry -> entry.value.sumOf { it.amount } }
            val exitByPlayer = exitRecords.groupBy { it.playerId }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            val byName = players.groupBy { it.name }

            val playerStats = byName.map { (name, records) ->
                val totalBuy = records.sumOf { buyInByPlayer[it.id] ?: 0L }
                val totalExit = records.sumOf { exitByPlayer[it.id] ?: 0L }
                PlayerStats(
                    name = name,
                    gamesPlayed = records.map { it.tableId }.distinct().count(),
                    totalBuyIns = totalBuy,
                    totalExits = totalExit,
                    netResult = totalExit - totalBuy
                )
            }.sortedByDescending { it.netResult }

            val playersByTable = players.groupBy { it.tableId }

            val closedTableStats = tables
                .filter { it.status == "CLOSED" }
                .map { table ->
                    val tPlayers = playersByTable[table.id] ?: emptyList()
                    val tBuy = tPlayers.sumOf { buyInByPlayer[it.id] ?: 0L }
                    val tExit = tPlayers.sumOf { exitByPlayer[it.id] ?: 0L }
                    val winner = tPlayers
                        .map { p ->
                            p.name to ((exitByPlayer[p.id] ?: 0L) - (buyInByPlayer[p.id] ?: 0L))
                        }
                        .maxByOrNull { it.second }
                    TableStats(
                        table = table,
                        playerCount = tPlayers.size,
                        totalBuyIns = tBuy,
                        totalExits = tExit,
                        topWinnerName = winner?.first,
                        topWinnerNet = winner?.second ?: 0L
                    )
                }
                .sortedByDescending { it.table.closedAt ?: 0L }

            _uiState.value = StatsUiState(
                totalTables = tables.size,
                closedTables = tables.count { it.status == "CLOSED" },
                totalTransactions = buyIns.size + exitRecords.size,
                distinctPlayers = byName.size,
                biggestWinner = playerStats.maxByOrNull { it.netResult }?.takeIf { it.netResult > 0 },
                mostActive = playerStats.maxByOrNull { it.gamesPlayed },
                playerStats = playerStats,
                closedTableStats = closedTableStats
            )
        }
    }
}


class StatsViewModelFactory(
    private val repository: PokerRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
