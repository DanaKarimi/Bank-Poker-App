package com.bankpoker.app.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bankpoker.app.data.local.entity.BuyIn
import com.bankpoker.app.data.local.entity.ExitRecord
import com.bankpoker.app.data.local.entity.Player
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.repository.PokerRepository
import com.bankpoker.app.repository.RemoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

class TableDetailViewModel(
    private val repository: PokerRepository,
    private val tableId: String,
    private val onRefreshCounts: (() -> Unit)? = null,
    private val remoteRepository: RemoteRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(TableDetailUiState())
    val uiState: StateFlow<TableDetailUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val players: Flow<List<Player>> = repository.getPlayersByTableId(tableId)
    val buyIns: Flow<List<BuyIn>> = repository.getBuyInsByTableId(tableId)
    val exitRecords: Flow<List<ExitRecord>> = repository.getExitRecordsByTableId(tableId)
    val savedPlayerNames: Flow<List<String>> = MutableStateFlow<List<String>>(emptyList()).also { flow ->
        viewModelScope.launch {
            val names = repository.getAllSavedPlayerNames()
            flow.value = names
        }
    }

    init {
        loadTableData()
        refreshTableFromServer()
    }

    fun loadTableData() {
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

    /**
     * Refresh and sync table players, buy-ins, and exits from server into local Room database
     */
    fun refreshTableFromServer(onComplete: ((Boolean, String?) -> Unit)? = null) {
        if (remoteRepository == null) {
            onComplete?.invoke(false, "Remote repository not available")
            return
        }
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                Log.d("TableDetail", "Refreshing table data from server for table: $tableId")

                // 1. Fetch and sync Players
                val playersResult = remoteRepository.getTablePlayers(tableId)
                if (playersResult.isSuccess) {
                    val remotePlayers = playersResult.getOrNull() ?: emptyList()
                    Log.d("TableDetail", "Fetched ${remotePlayers.size} players from server")

                    val roomPlayers = remotePlayers.map { dto ->
                        val player = Player(
                            id = dto.id,
                            tableId = tableId,
                            name = dto.name,
                            status = if (dto.status.equals("EXITED", ignoreCase = true)) "EXITED" else "PLAYING",
                            createdAt = if (dto.resolvedCreatedAt > 0) dto.resolvedCreatedAt else System.currentTimeMillis(),
                            entryFeePaid = dto.entryFeePaid || dto.entryFeePaidInt > 0
                        )
                        Log.d("Room", "Inserted player: ${player.name} (id: ${player.id})")
                        player
                    }
                    if (roomPlayers.isNotEmpty()) {
                        repository.insertOrUpdatePlayers(roomPlayers)
                    }
                } else {
                    Log.w("TableDetail", "Failed to fetch players: ${playersResult.exceptionOrNull()?.message}")
                }

                // 2. Fetch and sync Activity (all Buy-Ins & Exits)
                val activityResult = remoteRepository.getTableActivity(tableId)
                if (activityResult.isSuccess) {
                    val activity = activityResult.getOrNull()
                    val remoteBuyIns = activity?.buyIns ?: emptyList()
                    val remoteExits = activity?.exits ?: emptyList()

                    Log.d("TableDetail", "Fetched ${remoteBuyIns.size} buy-ins and ${remoteExits.size} exits from server")

                    val roomBuyIns = remoteBuyIns.map { dto ->
                        BuyIn(
                            id = dto.id,
                            tableId = tableId,
                            playerId = dto.resolvedPlayerId,
                            amount = dto.amount,
                            note = dto.note ?: "Online Buy-In",
                            createdAt = if (dto.resolvedCreatedAt > 0) dto.resolvedCreatedAt else System.currentTimeMillis()
                        )
                    }
                    if (roomBuyIns.isNotEmpty()) {
                        repository.insertOrUpdateBuyIns(roomBuyIns)
                    }

                    val roomExits = remoteExits.map { dto ->
                        ExitRecord(
                            id = dto.id,
                            tableId = tableId,
                            playerId = dto.resolvedPlayerId,
                            amount = dto.amount,
                            note = dto.note ?: "Online Exit",
                            createdAt = if (dto.resolvedCreatedAt > 0) dto.resolvedCreatedAt else System.currentTimeMillis()
                        )
                    }
                    if (roomExits.isNotEmpty()) {
                        repository.insertOrUpdateExitRecords(roomExits)
                    }
                } else {
                    // Fallback to separate endpoints if needed
                    val buyInsResult = remoteRepository.getTableBuyIns(tableId)
                    if (buyInsResult.isSuccess) {
                        val remoteBuyIns = buyInsResult.getOrNull() ?: emptyList()
                        val roomBuyIns = remoteBuyIns.map { dto ->
                            BuyIn(
                                id = dto.id,
                                tableId = tableId,
                                playerId = dto.resolvedPlayerId,
                                amount = dto.amount,
                                note = dto.note ?: "Online Buy-In",
                                createdAt = if (dto.resolvedCreatedAt > 0) dto.resolvedCreatedAt else System.currentTimeMillis()
                            )
                        }
                        if (roomBuyIns.isNotEmpty()) {
                            repository.insertOrUpdateBuyIns(roomBuyIns)
                        }
                    }

                    val exitsResult = remoteRepository.getTableExits(tableId)
                    if (exitsResult.isSuccess) {
                        val remoteExits = exitsResult.getOrNull() ?: emptyList()
                        val roomExits = remoteExits.map { dto ->
                            ExitRecord(
                                id = dto.id,
                                tableId = tableId,
                                playerId = dto.resolvedPlayerId,
                                amount = dto.amount,
                                note = dto.note ?: "Online Exit",
                                createdAt = if (dto.resolvedCreatedAt > 0) dto.resolvedCreatedAt else System.currentTimeMillis()
                            )
                        }
                        if (roomExits.isNotEmpty()) {
                            repository.insertOrUpdateExitRecords(roomExits)
                        }
                    }
                }

                loadTableData()
                onRefreshCounts?.invoke()
                onComplete?.invoke(true, null)
            } catch (e: Exception) {
                Log.e("TableDetail", "Error refreshing table from server", e)
                onComplete?.invoke(false, e.message)
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun addPlayer(name: String) {
        viewModelScope.launch {
            repository.addPlayer(tableId, name.trim().uppercase())
            onRefreshCounts?.invoke()
        }
    }

    /**
     * Add Buy-In (supports direct server synchronization for online tables)
     */
    fun addBuyIn(playerId: String, amount: Long, note: String?, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val currentTable = _uiState.value.table ?: repository.getTableById(tableId)
            val isOnlineTable = currentTable?.groupId != null && remoteRepository != null

            if (isOnlineTable && remoteRepository != null) {
                Log.d("TableDetail", "Performing direct online buy-in for player: $playerId, amount: $amount")
                val player = repository.getPlayerById(playerId)
                val remoteResult = remoteRepository.directBuyIn(
                    tableId = tableId,
                    playerId = playerId,
                    username = player?.name,
                    amount = amount,
                    note = note
                )

                if (remoteResult.isSuccess) {
                    val directRes = remoteResult.getOrNull()
                    val buyInId = directRes?.buyInId ?: UUID.randomUUID().toString()
                    Log.d("TableDetail", "Direct online buy-in success: $buyInId. Saving to Room.")

                    val buyIn = BuyIn(
                        id = buyInId,
                        tableId = tableId,
                        playerId = playerId,
                        amount = amount,
                        note = note ?: "Direct Buy-In",
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertOrUpdateBuyIns(listOf(buyIn))
                    loadTableData()
                    onRefreshCounts?.invoke()
                    onResult?.invoke(true, null)
                } else {
                    val error = remoteResult.exceptionOrNull()?.message ?: "Direct buy-in failed"
                    Log.e("TableDetail", "Direct online buy-in failed: $error")
                    onResult?.invoke(false, error)
                }
            } else {
                // Offline mode: save directly to local Room
                repository.addBuyIn(tableId, playerId, amount, note)
                loadTableData()
                onRefreshCounts?.invoke()
                onResult?.invoke(true, null)
            }
        }
    }

    /**
     * Add Exit (supports direct server synchronization for online tables)
     */
    fun addExitRecord(playerId: String, amount: Long, note: String?, onResult: ((Boolean, String?) -> Unit)? = null) {
        viewModelScope.launch {
            val currentTable = _uiState.value.table ?: repository.getTableById(tableId)
            val isOnlineTable = currentTable?.groupId != null && remoteRepository != null

            if (isOnlineTable && remoteRepository != null) {
                Log.d("TableDetail", "Performing direct online exit for player: $playerId, amount: $amount")
                val player = repository.getPlayerById(playerId)
                val remoteResult = remoteRepository.directExit(
                    tableId = tableId,
                    playerId = playerId,
                    username = player?.name,
                    amount = amount,
                    note = note
                )

                if (remoteResult.isSuccess) {
                    val directRes = remoteResult.getOrNull()
                    val exitId = directRes?.exitId ?: UUID.randomUUID().toString()
                    Log.d("TableDetail", "Direct online exit success: $exitId. Saving to Room.")

                    val exitRecord = ExitRecord(
                        id = exitId,
                        tableId = tableId,
                        playerId = playerId,
                        amount = amount,
                        note = note ?: "Direct Exit",
                        createdAt = System.currentTimeMillis()
                    )
                    repository.insertOrUpdateExitRecords(listOf(exitRecord))
                    repository.updatePlayerStatus(playerId, "EXITED")
                    loadTableData()
                    onRefreshCounts?.invoke()
                    onResult?.invoke(true, null)
                } else {
                    val error = remoteResult.exceptionOrNull()?.message ?: "Direct exit failed"
                    Log.e("TableDetail", "Direct online exit failed: $error")
                    onResult?.invoke(false, error)
                }
            } else {
                // Offline mode: save directly to local Room
                repository.addExitRecord(tableId, playerId, amount, note)
                loadTableData()
                onRefreshCounts?.invoke()
                onResult?.invoke(true, null)
            }
        }
    }

    fun closeTable() {
        viewModelScope.launch {
            repository.closeTableAndApplyToGroup(tableId)
            onRefreshCounts?.invoke()
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

    fun toggleEntryFee(playerId: String, paid: Boolean) {
        viewModelScope.launch {
            repository.toggleEntryFee(playerId, paid)
        }
    }
}

data class TableDetailUiState(
    val table: PokerTable? = null,
    val totalBuyIns: Long = 0L,
    val totalExits: Long = 0L,
    val remainingBalance: Long = 0L
)
