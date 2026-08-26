package com.bankpoker.app.repository

import com.bankpoker.app.data.local.dao.BuyInDao
import com.bankpoker.app.data.local.dao.ExitRecordDao
import com.bankpoker.app.data.local.dao.GroupBalanceDao
import com.bankpoker.app.data.local.dao.PaymentDao
import com.bankpoker.app.data.local.dao.PlayerDao
import com.bankpoker.app.data.local.dao.PokerTableDao
import com.bankpoker.app.data.local.dao.PlayerGroupDao
import com.bankpoker.app.data.local.entity.BuyIn
import com.bankpoker.app.data.local.entity.ExitRecord
import com.bankpoker.app.data.local.entity.GroupBalance
import com.bankpoker.app.data.local.entity.Payment
import com.bankpoker.app.data.local.entity.Player
import com.bankpoker.app.data.local.entity.PlayerGroup
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.data.local.entity.UnpaidEntryFeeInfo
import com.bankpoker.app.data.local.entity.EntryFeeHistoryInfo
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class PokerRepository(
    private val pokerTableDao: PokerTableDao,
    private val playerDao: PlayerDao,
    private val buyInDao: BuyInDao,
    private val exitRecordDao: ExitRecordDao,
    private val playerGroupDao: PlayerGroupDao,
    private val groupBalanceDao: GroupBalanceDao,
    private val paymentDao: PaymentDao
) {
    // Table operations
    fun getQuickTables(): Flow<List<PokerTable>> = pokerTableDao.getQuickTables()

    fun getAllTables(): Flow<List<PokerTable>> = pokerTableDao.getAllTables()

    suspend fun getTableById(tableId: String): PokerTable? = pokerTableDao.getTableById(tableId)

    suspend fun createTable(name: String, chipValue: Long?, groupId: String? = null, hasEntryFee: Boolean = false, entryFee: Long? = null): PokerTable {
        val table = PokerTable(
            id = UUID.randomUUID().toString(),
            name = name,
            chipValue = chipValue,
            status = "ACTIVE",
            createdAt = System.currentTimeMillis(),
            closedAt = null,
            groupId = groupId,
            hasEntryFee = hasEntryFee,
            entryFee = entryFee
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
        playerDao.setAllPlayersExitedForTable(tableId)
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

    suspend fun deleteTableAndRelatedData(tableId: String) {
        buyInDao.deleteBuyInsForTable(tableId)
        exitRecordDao.deleteExitRecordsForTable(tableId)
        playerDao.deletePlayersForTable(tableId)
        pokerTableDao.deleteTable(tableId)
    }

    suspend fun toggleEntryFee(playerId: String, paid: Boolean) {
        playerDao.updateEntryFeePaid(playerId, paid)
    }

    fun getUnpaidEntryFeeDebtors(): Flow<List<UnpaidEntryFeeInfo>> = playerDao.getUnpaidEntryFeeDebtors()

    fun getUnpaidEntryFeeDebtorsByGroupId(groupId: String): Flow<List<UnpaidEntryFeeInfo>> = playerDao.getUnpaidEntryFeeDebtorsByGroupId(groupId)

    fun getEntryFeeHistoryByGroupId(groupId: String): Flow<List<EntryFeeHistoryInfo>> = playerDao.getEntryFeeHistoryByGroupId(groupId)

    fun getAllEntryFeeHistory(): Flow<List<EntryFeeHistoryInfo>> = playerDao.getAllEntryFeeHistory()

    suspend fun markEntryFeePaid(playerId: String) {
        playerDao.updateEntryFeePaid(playerId, true)
    }



    // Group operations

    fun getAllGroups(): Flow<List<PlayerGroup>> = playerGroupDao.getAllGroups()

    suspend fun getGroupById(groupId: String): PlayerGroup? = playerGroupDao.getGroupById(groupId)

    suspend fun createGroup(name: String): PlayerGroup {
        val group = PlayerGroup(
            id = UUID.randomUUID().toString(),
            name = name,
            createdAt = System.currentTimeMillis()
        )
        playerGroupDao.insertGroup(group)
        return group
    }

    fun getTablesByGroupId(groupId: String): Flow<List<PokerTable>> = pokerTableDao.getTablesByGroupId(groupId)

    fun getBalancesByGroupId(groupId: String): Flow<List<GroupBalance>> = groupBalanceDao.getBalancesByGroupId(groupId)

    fun getPaymentsByGroupId(groupId: String): Flow<List<Payment>> = paymentDao.getPaymentsByGroupId(groupId)

    suspend fun closeTableAndApplyToGroup(tableId: String) {
        val table = pokerTableDao.getTableById(tableId) ?: return
        closeTable(tableId)
        val groupId = table.groupId ?: return
        val players = playerDao.getPlayersForTableOnce(tableId)
        players.forEach { p ->
            val buy = buyInDao.getTotalBuyInsForPlayer(p.id)
            val exit = exitRecordDao.getTotalExitsForPlayer(p.id)
            val net = (exit ?: 0L) - (buy ?: 0L)
            if (net != 0L) applyToGroupBalance(groupId, p.name, net)
        }
    }

    private suspend fun applyToGroupBalance(groupId: String, name: String, delta: Long) {
        val existing = groupBalanceDao.getBalance(groupId, name)
        if (existing != null) {
            groupBalanceDao.updateBalance(existing.copy(balance = existing.balance + delta))
        } else {
            groupBalanceDao.insertBalance(
                GroupBalance(UUID.randomUUID().toString(), groupId, name, delta)
            )
        }
    }

    suspend fun updateGroupName(groupId: String, newName: String) {
        playerGroupDao.updateGroupName(groupId, newName)
    }

    suspend fun deleteGroupCascade(groupId: String) {
        val tables = pokerTableDao.getTablesByGroupIdOnce(groupId)
        for (table in tables) {
            buyInDao.deleteBuyInsForTable(table.id)
            exitRecordDao.deleteExitRecordsForTable(table.id)
            playerDao.deletePlayersForTable(table.id)
            pokerTableDao.deleteTable(table.id)
        }
        groupBalanceDao.deleteBalancesByGroupId(groupId)
        paymentDao.deletePaymentsByGroupId(groupId)
        playerGroupDao.deleteGroup(groupId)
    }

    suspend fun recordPayment(groupId: String, fromPlayer: String, toPlayer: String, amount: Long) {
        paymentDao.insertPayment(
            Payment(UUID.randomUUID().toString(), groupId, fromPlayer, toPlayer, amount, System.currentTimeMillis())
        )
        val from = groupBalanceDao.getBalance(groupId, fromPlayer)
        if (from != null) groupBalanceDao.updateBalance(from.copy(balance = from.balance + amount))
        val to = groupBalanceDao.getBalance(groupId, toPlayer)
        if (to != null) groupBalanceDao.updateBalance(to.copy(balance = to.balance - amount))
    }
}

