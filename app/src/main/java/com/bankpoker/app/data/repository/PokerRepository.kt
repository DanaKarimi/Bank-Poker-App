package com.bankpoker.app.repository

import com.bankpoker.app.data.local.dao.BuyInDao
import com.bankpoker.app.data.local.dao.ExitRecordDao
import com.bankpoker.app.data.local.dao.PlayerDao
import com.bankpoker.app.data.local.dao.PokerTableDao
import com.bankpoker.app.data.local.entity.BuyIn
import com.bankpoker.app.data.local.entity.ExitRecord
import com.bankpoker.app.data.local.entity.Player
import com.bankpoker.app.data.local.entity.PokerTable
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class PokerRepository(
    private val pokerTableDao: PokerTableDao,
    private val playerDao: PlayerDao,
    private val buyInDao: BuyInDao,
    private val exitRecordDao: ExitRecordDao
) {
    // Table operations
    fun getAllTables(): Flow<List<PokerTable>> = pokerTableDao.getAllTables()

    suspend fun getTableById(tableId: String): PokerTable? = pokerTableDao.getTableById(tableId)

    suspend fun createTable(name: String, chipValue: Long?): PokerTable {
        val table = PokerTable(
            id = UUID.randomUUID().toString(),
            name = name,
            chipValue = chipValue,
            status = "ACTIVE",
            createdAt = System.currentTimeMillis(),
            closedAt = null
        )
        pokerTableDao.insertTable(table)
        return table
    }

    suspend fun closeTable(tableId: String) {
        pokerTableDao.closeTable(
            tableId = tableId,
            status = "CLOSED",
            closedAt = System.currentTimeMillis()
        )
    }

    // Player operations
    fun getPlayersByTableId(tableId: String): Flow<List<Player>> = playerDao.getPlayersByTableId(tableId)

    suspend fun getPlayerById(playerId: String): Player? = playerDao.getPlayerById(playerId)

    suspend fun addPlayer(tableId: String, name: String): Player {
        val player = Player(
            id = UUID.randomUUID().toString(),
            tableId = tableId,
            name = name,
            status = "PLAYING",
            createdAt = System.currentTimeMillis()
        )
        playerDao.insertPlayer(player)
        return player
    }

    suspend fun updatePlayerStatus(playerId: String, status: String) {
        playerDao.updatePlayerStatus(playerId, status)
    }

    suspend fun getPlayingPlayersCount(tableId: String): Int = playerDao.getPlayingPlayersCount(tableId)

    suspend fun getAllSavedPlayerNames(): List<String> = playerDao.getAllPlayerNames()

    // Buy-in operations
    fun getBuyInsByTableId(tableId: String): Flow<List<BuyIn>> = buyInDao.getBuyInsByTableId(tableId)

    fun getBuyInsByPlayerId(playerId: String): Flow<List<BuyIn>> = buyInDao.getBuyInsByPlayerId(playerId)

    suspend fun getTotalBuyInsForTable(tableId: String): Long = buyInDao.getTotalBuyInsForTable(tableId) ?: 0L

    suspend fun getTotalBuyInsForPlayer(playerId: String): Long = buyInDao.getTotalBuyInsForPlayer(playerId) ?: 0L

    suspend fun addBuyIn(tableId: String, playerId: String, amount: Long, note: String?) {
        val buyIn = BuyIn(
            id = UUID.randomUUID().toString(),
            tableId = tableId,
            playerId = playerId,
            amount = amount,
            note = note,
            createdAt = System.currentTimeMillis()
        )
        buyInDao.insertBuyIn(buyIn)
    }

    suspend fun updateBuyIn(buyIn: BuyIn) {
        buyInDao.updateBuyIn(buyIn)
    }

    suspend fun deleteBuyIn(buyIn: BuyIn) {
        buyInDao.deleteBuyIn(buyIn)
    }

    // Exit operations
    fun getExitRecordsByTableId(tableId: String): Flow<List<ExitRecord>> = exitRecordDao.getExitRecordsByTableId(tableId)

    fun getExitRecordsByPlayerId(playerId: String): Flow<List<ExitRecord>> = exitRecordDao.getExitRecordsByPlayerId(playerId)

    suspend fun getTotalExitsForTable(tableId: String): Long = exitRecordDao.getTotalExitsForTable(tableId) ?: 0L

    suspend fun getTotalExitsForPlayer(playerId: String): Long = exitRecordDao.getTotalExitsForPlayer(playerId) ?: 0L

    suspend fun addExitRecord(tableId: String, playerId: String, amount: Long, note: String?) {
        val exitRecord = ExitRecord(
            id = UUID.randomUUID().toString(),
            tableId = tableId,
            playerId = playerId,
            amount = amount,
            note = note,
            createdAt = System.currentTimeMillis()
        )
        exitRecordDao.insertExitRecord(exitRecord)
        // Update player status to EXITED
        playerDao.updatePlayerStatus(playerId, "EXITED")
    }

    suspend fun updateExitRecord(exitRecord: ExitRecord) {
        exitRecordDao.updateExitRecord(exitRecord)
    }

    suspend fun deleteExitRecord(exitRecord: ExitRecord) {
        exitRecordDao.deleteExitRecord(exitRecord)
    }

    suspend fun getExitCountByPlayer(playerId: String): Int = exitRecordDao.getExitCountByPlayer(playerId)

    suspend fun getAllTablesOnce(): List<PokerTable> = pokerTableDao.getAllTablesOnce()
    suspend fun getAllPlayersOnce(): List<Player> = playerDao.getAllPlayersOnce()
    suspend fun getAllBuyInsOnce(): List<BuyIn> = buyInDao.getAllBuyInsOnce()
    suspend fun getAllExitRecordsOnce(): List<ExitRecord> = exitRecordDao.getAllExitRecordsOnce()
}
