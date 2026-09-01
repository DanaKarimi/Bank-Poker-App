package com.bankpoker.app.repository

import com.bankpoker.app.data.local.dao.BuyInDao
import com.bankpoker.app.data.local.dao.ExitRecordDao
import com.bankpoker.app.data.local.dao.GroupBalanceDao
import com.bankpoker.app.data.local.dao.PaymentDao
import com.bankpoker.app.data.local.dao.PlayerDao
import com.bankpoker.app.data.local.dao.PokerTableDao
import com.bankpoker.app.data.local.dao.PlayerGroupDao
import com.bankpoker.app.data.local.dao.SettlementRecordDao
import com.bankpoker.app.data.local.BankPokerDatabase
import com.bankpoker.app.data.local.entity.BuyIn
import com.bankpoker.app.data.local.entity.ExitRecord
import com.bankpoker.app.data.local.entity.GroupBalance
import com.bankpoker.app.data.local.entity.Payment
import com.bankpoker.app.data.local.entity.Player
import com.bankpoker.app.data.local.entity.PlayerGroup
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.data.local.entity.SettlementRecord
import com.bankpoker.app.data.local.entity.UnpaidEntryFeeInfo
import com.bankpoker.app.data.local.entity.EntryFeeHistoryInfo
import com.bankpoker.app.data.local.entity.PlayerGameHistory
import com.bankpoker.app.data.local.entity.PlayerProfileData
import androidx.room.withTransaction
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID


import com.bankpoker.app.data.local.dao.EntryFeeRecordDao
import com.bankpoker.app.data.local.entity.EntryFeeRecord

class PokerRepository(
    private val pokerTableDao: PokerTableDao,
    private val playerDao: PlayerDao,
    private val buyInDao: BuyInDao,
    private val exitRecordDao: ExitRecordDao,
    private val playerGroupDao: PlayerGroupDao,
    private val groupBalanceDao: GroupBalanceDao,
    private val paymentDao: PaymentDao,
    private val settlementRecordDao: SettlementRecordDao,
    private val entryFeeRecordDao: EntryFeeRecordDao,
    private val database: BankPokerDatabase? = null
) {

    // Table operations
    fun getQuickTables(): Flow<List<PokerTable>> = pokerTableDao.getQuickTables()

    fun getAllTables(): Flow<List<PokerTable>> = pokerTableDao.getAllTables()

    suspend fun getTableById(tableId: String): PokerTable? = pokerTableDao.getTableById(tableId)

    suspend fun createTable(
        name: String,
        chipValue: Long?,
        groupId: String? = null,
        hasEntryFee: Boolean = false,
        entryFee: Long? = null,
        customId: String? = null
    ): PokerTable {
        val table = PokerTable(
            id = customId ?: UUID.randomUUID().toString(),
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

    suspend fun insertOrUpdatePlayers(players: List<Player>) {
        playerDao.insertPlayers(players)
    }

    suspend fun insertOrUpdateBuyIns(buyIns: List<BuyIn>) {
        buyInDao.insertBuyIns(buyIns)
    }

    suspend fun insertOrUpdateExitRecords(exitRecords: List<ExitRecord>) {
        exitRecordDao.insertExitRecords(exitRecords)
    }

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

    suspend fun updateTable(tableId: String, name: String, chipValue: Long?, hasEntryFee: Boolean, entryFee: Long?) {
        val existing = pokerTableDao.getTableById(tableId) ?: return
        pokerTableDao.updateTable(
            existing.copy(
                name = name,
                chipValue = chipValue,
                hasEntryFee = hasEntryFee,
                entryFee = if (hasEntryFee) entryFee else null
            )
        )
    }

    suspend fun deleteTableAndRelatedData(tableId: String) {
        entryFeeRecordDao.deleteEntryFeeRecordsByTableId(tableId)
        settlementRecordDao.deleteSettlementsByTableId(tableId)
        buyInDao.deleteBuyInsForTable(tableId)
        exitRecordDao.deleteExitRecordsForTable(tableId)
        playerDao.deletePlayersForTable(tableId)
        pokerTableDao.deleteTable(tableId)
    }

    suspend fun deleteTableCascade(tableId: String) {
        deleteTableAndRelatedData(tableId)
    }

    suspend fun getTableDetailsCount(tableId: String): Pair<Int, Int> {
        val playersCount = playerDao.getPlayersForTableOnce(tableId).size
        val buyInsCount = buyInDao.getBuyInsByTableIdOnce(tableId).size
        val exitsCount = exitRecordDao.getExitRecordsByTableIdOnce(tableId).size
        return Pair(playersCount, buyInsCount + exitsCount)
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

    suspend fun createGroup(
        name: String,
        mode: String = "OFFLINE",
        serverId: String? = null,
        inviteCode: String? = null,
        customId: String? = null
    ): PlayerGroup {
        val group = PlayerGroup(
            id = customId ?: serverId ?: UUID.randomUUID().toString(),
            name = name,
            createdAt = System.currentTimeMillis(),
            mode = mode,
            serverId = serverId,
            inviteCode = inviteCode
        )
        playerGroupDao.insertGroup(group)
        return group
    }

    fun getTablesByGroupId(groupId: String): Flow<List<PokerTable>> = pokerTableDao.getTablesByGroupId(groupId)

    fun getBalancesByGroupId(groupId: String): Flow<List<GroupBalance>> = groupBalanceDao.getBalancesByGroupId(groupId)

    fun getPaymentsByGroupId(groupId: String): Flow<List<Payment>> = paymentDao.getPaymentsByGroupId(groupId)

    fun getSettlementsByGroupId(groupId: String): Flow<List<SettlementRecord>> = settlementRecordDao.getSettlementsByGroupId(groupId)

    fun getUnpaidSettlementsByGroupId(groupId: String): Flow<List<SettlementRecord>> = settlementRecordDao.getUnpaidSettlementsByGroupId(groupId)

    suspend fun getAllSettlementsByGroupIdOnce(groupId: String): List<SettlementRecord> = settlementRecordDao.getAllSettlementsByGroupIdOnce(groupId)

    suspend fun closeTableAndApplyToGroup(tableId: String) {
        val table = pokerTableDao.getTableById(tableId) ?: return
        closeTable(tableId)
        val groupId = table.groupId ?: return
        val players = playerDao.getPlayersForTableOnce(tableId)
        val now = System.currentTimeMillis()

        // Insert Entry Fee records if table has entry fee
        if (table.hasEntryFee && table.entryFee != null && table.entryFee > 0) {
            val feeRecords = players.map { p ->
                EntryFeeRecord(
                    id = UUID.randomUUID().toString(),
                    groupId = groupId,
                    tableId = table.id,
                    tableName = table.name,
                    playerName = p.name,
                    amount = table.entryFee,
                    paid = p.entryFeePaid,
                    timestamp = now
                )
            }
            if (feeRecords.isNotEmpty()) {
                entryFeeRecordDao.insertEntryFeeRecords(feeRecords)
            }
        }

        players.forEach { p ->
            val buy = buyInDao.getTotalBuyInsForPlayer(p.id) ?: 0L
            val exit = exitRecordDao.getTotalExitsForPlayer(p.id) ?: 0L
            val net = exit - buy
            if (net != 0L) {
                applyToGroupBalance(groupId, p.name, net)
            }
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
            entryFeeRecordDao.deleteEntryFeeRecordsByTableId(table.id)
            settlementRecordDao.deleteSettlementsByTableId(table.id)
            buyInDao.deleteBuyInsForTable(table.id)
            exitRecordDao.deleteExitRecordsForTable(table.id)
            playerDao.deletePlayersForTable(table.id)
            pokerTableDao.deleteTable(table.id)
        }
        entryFeeRecordDao.deleteEntryFeeRecordsByGroupId(groupId)
        settlementRecordDao.deleteSettlementsByGroupId(groupId)
        groupBalanceDao.deleteBalancesByGroupId(groupId)
        paymentDao.deletePaymentsByGroupId(groupId)
        playerGroupDao.deleteGroup(groupId)
    }

    suspend fun recordPayment(groupId: String, fromPlayer: String, toPlayer: String, amount: Long) {
        if (amount <= 0) return
        paymentDao.insertPayment(
            Payment(UUID.randomUUID().toString(), groupId, fromPlayer, toPlayer, amount, System.currentTimeMillis())
        )
        applyToGroupBalance(groupId, fromPlayer, amount)
        applyToGroupBalance(groupId, toPlayer, -amount)
    }

    suspend fun recordManualPayment(groupId: String, payerName: String, receiverName: String, amount: Long) {
        recordPayment(groupId, payerName, receiverName, amount)
    }

    suspend fun updatePaymentAmount(paymentId: String, newAmount: Long) {
        val existing = paymentDao.getPaymentById(paymentId) ?: return
        if (newAmount <= 0) return
        val delta = newAmount - existing.amount
        paymentDao.updatePayment(existing.copy(amount = newAmount))
        applyToGroupBalance(existing.groupId, existing.fromPlayer, delta)
        applyToGroupBalance(existing.groupId, existing.toPlayer, -delta)
    }

    suspend fun deletePayment(paymentId: String) {
        val existing = paymentDao.getPaymentById(paymentId) ?: return
        paymentDao.deletePaymentById(paymentId)
        applyToGroupBalance(existing.groupId, existing.fromPlayer, -existing.amount)
        applyToGroupBalance(existing.groupId, existing.toPlayer, existing.amount)
    }

    // Entry Fee Records operations
    fun getEntryFeeRecordsByGroupId(groupId: String): Flow<List<EntryFeeRecord>> =
        entryFeeRecordDao.getEntryFeeRecordsByGroupId(groupId)

    fun getUnpaidEntryFeeRecordsByGroupId(groupId: String): Flow<List<EntryFeeRecord>> =
        entryFeeRecordDao.getUnpaidEntryFeeRecordsByGroupId(groupId)

    suspend fun updateEntryFeeRecord(id: String, amount: Long, paid: Boolean) {
        entryFeeRecordDao.updateEntryFeeRecordAmountAndPaid(id, amount, paid)
    }

    suspend fun deleteEntryFeeRecord(id: String) {
        entryFeeRecordDao.deleteEntryFeeRecordById(id)
    }

    suspend fun markEntryFeeRecordPaid(id: String) {
        entryFeeRecordDao.updateEntryFeeRecordPaid(id, true)
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
        val settlements = settlementRecordDao.getAllSettlementsOnce()
        val entryFees = entryFeeRecordDao.getAllEntryFeeRecordsOnce()

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

        val settlementsArray = JSONArray()
        settlements.forEach { s ->
            val obj = JSONObject()
            obj.put("id", s.id)
            obj.put("groupId", s.groupId)
            obj.put("tableId", s.tableId)
            obj.put("tableName", s.tableName)
            obj.put("payerName", s.payerName)
            obj.put("receiverName", s.receiverName)
            obj.put("amount", s.amount)
            obj.put("initialAmount", s.initialAmount)
            obj.put("paid", s.paid)
            obj.put("timestamp", s.timestamp)
            settlementsArray.put(obj)
        }
        root.put("settlements", settlementsArray)

        val entryFeesArray = JSONArray()
        entryFees.forEach { ef ->
            val obj = JSONObject()
            obj.put("id", ef.id)
            obj.put("groupId", ef.groupId)
            obj.put("tableId", ef.tableId)
            obj.put("tableName", ef.tableName)
            obj.put("playerName", ef.playerName)
            obj.put("amount", ef.amount)
            obj.put("paid", ef.paid)
            obj.put("timestamp", ef.timestamp)
            entryFeesArray.put(obj)
        }
        root.put("entryFees", entryFeesArray)

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

        val settlements = mutableListOf<SettlementRecord>()
        val settlementsArray = root.optJSONArray("settlements")
        if (settlementsArray != null) {
            for (i in 0 until settlementsArray.length()) {
                val obj = settlementsArray.getJSONObject(i)
                settlements.add(
                    SettlementRecord(
                        id = obj.getString("id"),
                        groupId = obj.getString("groupId"),
                        tableId = obj.getString("tableId"),
                        tableName = obj.getString("tableName"),
                        payerName = obj.getString("payerName"),
                        receiverName = obj.getString("receiverName"),
                        amount = obj.getLong("amount"),
                        initialAmount = obj.optLong("initialAmount", obj.getLong("amount")),
                        paid = obj.optBoolean("paid", false),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        val entryFees = mutableListOf<EntryFeeRecord>()
        val entryFeesArray = root.optJSONArray("entryFees")
        if (entryFeesArray != null) {
            for (i in 0 until entryFeesArray.length()) {
                val obj = entryFeesArray.getJSONObject(i)
                entryFees.add(
                    EntryFeeRecord(
                        id = obj.getString("id"),
                        groupId = obj.getString("groupId"),
                        tableId = obj.getString("tableId"),
                        tableName = obj.getString("tableName"),
                        playerName = obj.getString("playerName"),
                        amount = obj.getLong("amount"),
                        paid = obj.optBoolean("paid", false),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis())
                    )
                )
            }
        }

        val db = database
        if (db != null) {
            db.withTransaction {
                performRestore(groups, tables, players, buyIns, exitRecords, payments, balances, settlements, entryFees)
            }
        } else {
            performRestore(groups, tables, players, buyIns, exitRecords, payments, balances, settlements, entryFees)
        }
    }

    private suspend fun performRestore(
        groups: List<PlayerGroup>,
        tables: List<PokerTable>,
        players: List<Player>,
        buyIns: List<BuyIn>,
        exitRecords: List<ExitRecord>,
        payments: List<Payment>,
        balances: List<GroupBalance>,
        settlements: List<SettlementRecord>,
        entryFees: List<EntryFeeRecord>
    ) {
        pokerTableDao.deleteAllTables()
        playerDao.deleteAllPlayers()
        buyInDao.deleteAllBuyIns()
        exitRecordDao.deleteAllExitRecords()
        playerGroupDao.deleteAllGroups()
        groupBalanceDao.deleteAllBalances()
        paymentDao.deleteAllPayments()
        settlementRecordDao.deleteAllSettlements()
        entryFeeRecordDao.deleteAllEntryFeeRecords()

        playerGroupDao.insertGroups(groups)
        pokerTableDao.insertTables(tables)
        playerDao.insertPlayers(players)
        buyInDao.insertBuyIns(buyIns)
        exitRecordDao.insertExitRecords(exitRecords)
        paymentDao.insertPayments(payments)
        groupBalanceDao.insertBalances(balances)
        settlementRecordDao.insertSettlements(settlements)
        entryFeeRecordDao.insertEntryFeeRecords(entryFees)
    }

    // Player Profile operations
    fun getPlayerProfile(name: String): Flow<PlayerProfileData> {
        val trimmedName = name.trim()
        return playerDao.getPlayerGamesByName(trimmedName).map { games ->
            val tablesPlayed = games.size
            val winsCount = games.count { it.netResult > 0 }
            val lossesCount = games.count { it.netResult < 0 }
            val breakEvenCount = games.count { it.netResult == 0L }
            val netResult = games.sumOf { it.netResult }
            val biggestWin = games.filter { it.netResult > 0 }.maxOfOrNull { it.netResult } ?: 0L
            val biggestLoss = games.filter { it.netResult < 0 }.minOfOrNull { it.netResult } ?: 0L
            val totalBuyIns = games.sumOf { it.totalBuyIn }
            val entryFeesPaidCount = games.count { it.entryFeePaid }

            PlayerProfileData(
                playerName = trimmedName,
                tablesPlayed = tablesPlayed,
                winsCount = winsCount,
                lossesCount = lossesCount,
                breakEvenCount = breakEvenCount,
                netResult = netResult,
                biggestWin = biggestWin,
                biggestLoss = biggestLoss,
                totalBuyIns = totalBuyIns,
                entryFeesPaidCount = entryFeesPaidCount,
                games = games
            )
        }
    }

    suspend fun getGroupExportBundle(groupId: String): GroupExportBundle {
        val group = playerGroupDao.getGroupById(groupId) ?: throw IllegalStateException("Group not found")
        val tables = pokerTableDao.getTablesByGroupIdOnce(groupId)
        val tableIds = tables.map { it.id }

        val players = mutableListOf<Player>()
        val buyIns = mutableListOf<BuyIn>()
        val exits = mutableListOf<ExitRecord>()

        for (tId in tableIds) {
            val tablePlayers = playerDao.getPlayersForTableOnce(tId)
            players.addAll(tablePlayers)
            val tableBuyIns = buyInDao.getBuyInsByTableIdOnce(tId)
            buyIns.addAll(tableBuyIns)
            val tableExits = exitRecordDao.getExitRecordsByTableIdOnce(tId)
            exits.addAll(tableExits)
        }

        val payments = paymentDao.getPaymentsByGroupIdOnce(groupId)
        val settlements = settlementRecordDao.getAllSettlementsByGroupIdOnce(groupId)
        val entryFees = entryFeeRecordDao.getEntryFeeRecordsByGroupIdOnce(groupId)

        return GroupExportBundle(
            group = group,
            tables = tables,
            players = players,
            buyIns = buyIns,
            exits = exits,
            payments = payments,
            settlements = settlements,
            entryFees = entryFees
        )
    }

    suspend fun updateGroupAfterOnlineConversion(groupId: String, serverId: String, inviteCode: String) {
        playerGroupDao.updateGroupModeAndSync(groupId, "ONLINE", serverId, inviteCode)
    }
}

data class GroupExportBundle(
    val group: PlayerGroup,
    val tables: List<PokerTable>,
    val players: List<Player>,
    val buyIns: List<BuyIn>,
    val exits: List<ExitRecord>,
    val payments: List<Payment>,
    val settlements: List<SettlementRecord>,
    val entryFees: List<EntryFeeRecord>
)



