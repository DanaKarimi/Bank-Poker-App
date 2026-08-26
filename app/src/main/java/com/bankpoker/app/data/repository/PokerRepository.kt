package com.bankpoker.app.repository

import com.bankpoker.app.data.local.dao.BuyInDao
import com.bankpoker.app.data.local.dao.ExitRecordDao
import com.bankpoker.app.data.local.dao.GroupBalanceDao
import com.bankpoker.app.data.local.dao.PaymentDao
import com.bankpoker.app.data.local.dao.PlayerDao
import com.bankpoker.app.data.local.dao.PokerTableDao
import com.bankpoker.app.data.local.dao.PlayerGroupDao
import com.bankpoker.app.data.local.BankPokerDatabase
import com.bankpoker.app.data.local.entity.BuyIn
import com.bankpoker.app.data.local.entity.ExitRecord
import com.bankpoker.app.data.local.entity.GroupBalance
import com.bankpoker.app.data.local.entity.Payment
import com.bankpoker.app.data.local.entity.Player
import com.bankpoker.app.data.local.entity.PlayerGroup
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.data.local.entity.UnpaidEntryFeeInfo
import com.bankpoker.app.data.local.entity.EntryFeeHistoryInfo
import androidx.room.withTransaction
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class PokerRepository(
    private val pokerTableDao: PokerTableDao,
    private val playerDao: PlayerDao,
    private val buyInDao: BuyInDao,
    private val exitRecordDao: ExitRecordDao,
    private val playerGroupDao: PlayerGroupDao,
    private val groupBalanceDao: GroupBalanceDao,
    private val paymentDao: PaymentDao,
    private val database: BankPokerDatabase? = null
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

    // Backup & Restore operations
    suspend fun exportBackupJson(): String {
        val groups = playerGroupDao.getAllGroupsOnce()
        val tables = pokerTableDao.getAllTablesOnce()
        val players = playerDao.getAllPlayersOnce()
        val buyIns = buyInDao.getAllBuyInsOnce()
        val exitRecords = exitRecordDao.getAllExitRecordsOnce()
        val payments = paymentDao.getAllPaymentsOnce()
        val balances = groupBalanceDao.getAllBalancesOnce()

        val root = JSONObject()
        root.put("version", 1)
        root.put("timestamp", System.currentTimeMillis())

        val groupsArray = JSONArray()
        groups.forEach { g ->
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("name", g.name)
            obj.put("createdAt", g.createdAt)
            groupsArray.put(obj)
        }
        root.put("groups", groupsArray)

        val tablesArray = JSONArray()
        tables.forEach { t ->
            val obj = JSONObject()
            obj.put("id", t.id)
            obj.put("name", t.name)
            if (t.chipValue != null) obj.put("chipValue", t.chipValue) else obj.put("chipValue", JSONObject.NULL)
            obj.put("status", t.status)
            obj.put("createdAt", t.createdAt)
            if (t.closedAt != null) obj.put("closedAt", t.closedAt) else obj.put("closedAt", JSONObject.NULL)
            if (t.groupId != null) obj.put("groupId", t.groupId) else obj.put("groupId", JSONObject.NULL)
            obj.put("hasEntryFee", t.hasEntryFee)
            if (t.entryFee != null) obj.put("entryFee", t.entryFee) else obj.put("entryFee", JSONObject.NULL)
            tablesArray.put(obj)
        }
        root.put("tables", tablesArray)

        val playersArray = JSONArray()
        players.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("tableId", p.tableId)
            obj.put("name", p.name)
            obj.put("status", p.status)
            obj.put("createdAt", p.createdAt)
            obj.put("entryFeePaid", p.entryFeePaid)
            playersArray.put(obj)
        }
        root.put("players", playersArray)

        val buyInsArray = JSONArray()
        buyIns.forEach { b ->
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("tableId", b.tableId)
            obj.put("playerId", b.playerId)
            obj.put("amount", b.amount)
            if (b.note != null) obj.put("note", b.note) else obj.put("note", JSONObject.NULL)
            obj.put("createdAt", b.createdAt)
            buyInsArray.put(obj)
        }
        root.put("buyIns", buyInsArray)

        val exitsArray = JSONArray()
        exitRecords.forEach { e ->
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("tableId", e.tableId)
            obj.put("playerId", e.playerId)
            obj.put("amount", e.amount)
            if (e.note != null) obj.put("note", e.note) else obj.put("note", JSONObject.NULL)
            obj.put("createdAt", e.createdAt)
            exitsArray.put(obj)
        }
        root.put("exitRecords", exitsArray)

        val paymentsArray = JSONArray()
        payments.forEach { p ->
            val obj = JSONObject()
            obj.put("id", p.id)
            obj.put("groupId", p.groupId)
            obj.put("fromPlayer", p.fromPlayer)
            obj.put("toPlayer", p.toPlayer)
            obj.put("amount", p.amount)
            obj.put("createdAt", p.createdAt)
            paymentsArray.put(obj)
        }
        root.put("payments", paymentsArray)

        val balancesArray = JSONArray()
        balances.forEach { b ->
            val obj = JSONObject()
            obj.put("id", b.id)
            obj.put("groupId", b.groupId)
            obj.put("playerName", b.playerName)
            obj.put("balance", b.balance)
            balancesArray.put(obj)
        }
        root.put("groupBalances", balancesArray)

        return root.toString(2)
    }

    suspend fun restoreBackupJson(jsonString: String) {
        val root = JSONObject(jsonString)

        val groups = mutableListOf<PlayerGroup>()
        val groupsArray = root.optJSONArray("groups")
        if (groupsArray != null) {
            for (i in 0 until groupsArray.length()) {
                val obj = groupsArray.getJSONObject(i)
                groups.add(
                    PlayerGroup(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val tables = mutableListOf<PokerTable>()
        val tablesArray = root.optJSONArray("tables")
        if (tablesArray != null) {
            for (i in 0 until tablesArray.length()) {
                val obj = tablesArray.getJSONObject(i)
                tables.add(
                    PokerTable(
                        id = obj.getString("id"),
                        name = obj.getString("name"),
                        chipValue = if (obj.isNull("chipValue")) null else obj.optLong("chipValue"),
                        status = obj.optString("status", "ACTIVE"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        closedAt = if (obj.isNull("closedAt")) null else obj.optLong("closedAt"),
                        groupId = if (obj.isNull("groupId")) null else obj.optString("groupId"),
                        hasEntryFee = obj.optBoolean("hasEntryFee", false),
                        entryFee = if (obj.isNull("entryFee")) null else obj.optLong("entryFee")
                    )
                )
            }
        }

        val players = mutableListOf<Player>()
        val playersArray = root.optJSONArray("players")
        if (playersArray != null) {
            for (i in 0 until playersArray.length()) {
                val obj = playersArray.getJSONObject(i)
                players.add(
                    Player(
                        id = obj.getString("id"),
                        tableId = obj.getString("tableId"),
                        name = obj.getString("name"),
                        status = obj.optString("status", "PLAYING"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        entryFeePaid = obj.optBoolean("entryFeePaid", false)
                    )
                )
            }
        }

        val buyIns = mutableListOf<BuyIn>()
        val buyInsArray = root.optJSONArray("buyIns")
        if (buyInsArray != null) {
            for (i in 0 until buyInsArray.length()) {
                val obj = buyInsArray.getJSONObject(i)
                buyIns.add(
                    BuyIn(
                        id = obj.getString("id"),
                        tableId = obj.getString("tableId"),
                        playerId = obj.getString("playerId"),
                        amount = obj.getLong("amount"),
                        note = if (obj.isNull("note")) null else obj.optString("note"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val exitRecords = mutableListOf<ExitRecord>()
        val exitsArray = root.optJSONArray("exitRecords")
        if (exitsArray != null) {
            for (i in 0 until exitsArray.length()) {
                val obj = exitsArray.getJSONObject(i)
                exitRecords.add(
                    ExitRecord(
                        id = obj.getString("id"),
                        tableId = obj.getString("tableId"),
                        playerId = obj.getString("playerId"),
                        amount = obj.getLong("amount"),
                        note = if (obj.isNull("note")) null else obj.optString("note"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val payments = mutableListOf<Payment>()
        val paymentsArray = root.optJSONArray("payments")
        if (paymentsArray != null) {
            for (i in 0 until paymentsArray.length()) {
                val obj = paymentsArray.getJSONObject(i)
                payments.add(
                    Payment(
                        id = obj.getString("id"),
                        groupId = obj.getString("groupId"),
                        fromPlayer = obj.getString("fromPlayer"),
                        toPlayer = obj.getString("toPlayer"),
                        amount = obj.getLong("amount"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }

        val balances = mutableListOf<GroupBalance>()
        val balancesArray = root.optJSONArray("groupBalances")
        if (balancesArray != null) {
            for (i in 0 until balancesArray.length()) {
                val obj = balancesArray.getJSONObject(i)
                balances.add(
                    GroupBalance(
                        id = obj.getString("id"),
                        groupId = obj.getString("groupId"),
                        playerName = obj.getString("playerName"),
                        balance = obj.getLong("balance")
                    )
                )
            }
        }

        val db = database
        if (db != null) {
            db.withTransaction {
                performRestore(groups, tables, players, buyIns, exitRecords, payments, balances)
            }
        } else {
            performRestore(groups, tables, players, buyIns, exitRecords, payments, balances)
        }
    }

    private suspend fun performRestore(
        groups: List<PlayerGroup>,
        tables: List<PokerTable>,
        players: List<Player>,
        buyIns: List<BuyIn>,
        exitRecords: List<ExitRecord>,
        payments: List<Payment>,
        balances: List<GroupBalance>
    ) {
        pokerTableDao.deleteAllTables()
        playerDao.deleteAllPlayers()
        buyInDao.deleteAllBuyIns()
        exitRecordDao.deleteAllExitRecords()
        playerGroupDao.deleteAllGroups()
        groupBalanceDao.deleteAllBalances()
        paymentDao.deleteAllPayments()

        playerGroupDao.insertGroups(groups)
        pokerTableDao.insertTables(tables)
        playerDao.insertPlayers(players)
        buyInDao.insertBuyIns(buyIns)
        exitRecordDao.insertExitRecords(exitRecords)
        paymentDao.insertPayments(payments)
        groupBalanceDao.insertBalances(balances)
    }
}


