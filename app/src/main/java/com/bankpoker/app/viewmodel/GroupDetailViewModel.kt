package com.bankpoker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bankpoker.app.data.local.entity.GroupBalance
import com.bankpoker.app.data.local.entity.Payment
import com.bankpoker.app.data.local.entity.PlayerGroup
import com.bankpoker.app.data.local.entity.PokerTable
import com.bankpoker.app.data.local.entity.UnpaidEntryFeeInfo
import com.bankpoker.app.data.local.entity.EntryFeeHistoryInfo
import com.bankpoker.app.data.remote.dto.CreateTableResponse
import com.bankpoker.app.repository.PokerRepository
import com.bankpoker.app.repository.RemoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    private val repository: PokerRepository,
    private val groupId: String,
    private val remoteRepository: RemoteRepository? = null
) : ViewModel() {

    private val _group = MutableStateFlow<PlayerGroup?>(null)
    val group: StateFlow<PlayerGroup?> = _group.asStateFlow()

    val tables: Flow<List<PokerTable>> = repository.getTablesByGroupId(groupId)
    val balances: Flow<List<GroupBalance>> = repository.getBalancesByGroupId(groupId)
    val payments: Flow<List<Payment>> = repository.getPaymentsByGroupId(groupId)
    val entryFeeDebtors: Flow<List<UnpaidEntryFeeInfo>> = repository.getUnpaidEntryFeeDebtorsByGroupId(groupId)
    val entryFeeHistory: Flow<List<EntryFeeHistoryInfo>> = repository.getEntryFeeHistoryByGroupId(groupId)

    init {
        viewModelScope.launch {
            _group.value = repository.getGroupById(groupId)
        }
    }

    fun markEntryFeePaid(playerId: String) {
        viewModelScope.launch {
            repository.toggleEntryFee(playerId, true)
        }
    }

    fun recordManualPayment(payerName: String, receiverName: String, amount: Long) {
        viewModelScope.launch {
            repository.recordManualPayment(groupId, payerName, receiverName, amount)
            val currentGroup = _group.value ?: repository.getGroupById(groupId)
            val serverGroupId = currentGroup?.serverId ?: currentGroup?.id ?: groupId
            if (currentGroup?.mode == "ONLINE" && remoteRepository != null) {
                remoteRepository.recordPayment(serverGroupId, payerName, receiverName, amount)
                syncSettlementToServer()
                syncGroupStatsToServer()
            }
        }
    }

    fun updateGroupName(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            repository.updateGroupName(groupId, trimmed)
            _group.value = _group.value?.copy(name = trimmed)
        }
    }

    fun deleteGroup() {
        viewModelScope.launch {
            repository.deleteGroupCascade(groupId)
        }
    }

    fun createTable(
        name: String,
        chipValue: Long?,
        hasEntryFee: Boolean,
        entryFee: Long?,
        onSuccess: ((PokerTable) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val currentGroup = _group.value ?: repository.getGroupById(groupId)
            val serverGroupId = currentGroup?.serverId ?: currentGroup?.id ?: groupId
            if (currentGroup?.mode == "ONLINE" && remoteRepository != null) {
                // Online group: API call -> Server success -> Room Insert
                val result = remoteRepository.createTable(
                    groupId = serverGroupId,
                    name = name.trim(),
                    chipValue = chipValue,
                    entryFee = if (hasEntryFee) entryFee else null
                )
                if (result.isSuccess) {
                    val serverTableId = result.getOrNull()?.tableId
                    val table = repository.createTable(
                        name = name.trim(),
                        chipValue = chipValue,
                        groupId = groupId,
                        hasEntryFee = hasEntryFee,
                        entryFee = entryFee,
                        customId = serverTableId
                    )
                    onSuccess?.invoke(table)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to create online table on server."
                    onError?.invoke(errorMsg)
                }
            } else {
                // Offline group: Direct local creation in Room
                val table = repository.createTable(
                    name = name.trim(),
                    chipValue = chipValue,
                    groupId = groupId,
                    hasEntryFee = hasEntryFee,
                    entryFee = entryFee
                )
                onSuccess?.invoke(table)
            }
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

    fun recordPayment(fromPlayer: String, toPlayer: String, amount: Long) {
        viewModelScope.launch {
            repository.recordPayment(groupId, fromPlayer, toPlayer, amount)
            val currentGroup = _group.value ?: repository.getGroupById(groupId)
            val serverGroupId = currentGroup?.serverId ?: currentGroup?.id ?: groupId
            if (currentGroup?.mode == "ONLINE" && remoteRepository != null) {
                remoteRepository.recordPayment(serverGroupId, fromPlayer, toPlayer, amount)
                syncSettlementToServer()
                syncGroupStatsToServer()
            }
        }
    }

    fun syncSettlementToServer() {
        if (remoteRepository == null) return
        viewModelScope.launch {
            try {
                val currentGroup = _group.value ?: repository.getGroupById(groupId)
                if (currentGroup?.mode == "ONLINE") {
                    val serverGroupId = currentGroup.serverId ?: currentGroup.id
                    val currentBalances = repository.getBalancesByGroupIdOnce(groupId)
                    val settlements = com.bankpoker.app.ui.screens.calculateGroupSettlement(currentBalances)
                    android.util.Log.d("SettlementSync", "Pushing ${settlements.size} rows to server group: $serverGroupId")
                    remoteRepository.syncSettlement(serverGroupId, settlements)
                }
            } catch (e: Exception) {
                android.util.Log.e("SettlementSync", "FAILED in syncSettlementToServer: ${e.message}", e)
            }
        }
    }

    fun syncGroupStatsToServer() {
        if (remoteRepository == null) return
        viewModelScope.launch {
            try {
                val currentGroup = _group.value ?: repository.getGroupById(groupId)
                if (currentGroup?.mode == "ONLINE") {
                    val serverGroupId = currentGroup.serverId ?: currentGroup.id
                    val currentBalances = repository.getBalancesByGroupIdOnce(groupId)
                    android.util.Log.d("SettlementSync", "Pushing ${currentBalances.size} balances to server group: $serverGroupId")
                    remoteRepository.syncGroupBalances(serverGroupId, currentBalances)
                }
            } catch (e: Exception) {
                android.util.Log.e("SettlementSync", "FAILED in syncGroupStatsToServer: ${e.message}", e)
            }
        }
    }

    fun syncSettlementPlan(settlements: List<com.bankpoker.app.ui.screens.Settlement>) {
        viewModelScope.launch {
            val currentGroup = _group.value ?: repository.getGroupById(groupId)
            val serverGroupId = currentGroup?.serverId ?: currentGroup?.id ?: groupId
            if (currentGroup?.mode == "ONLINE" && remoteRepository != null) {
                remoteRepository.syncSettlement(serverGroupId, settlements)
            }
        }
    }

    private val _isConverting = MutableStateFlow(false)
    val isConverting: StateFlow<Boolean> = _isConverting.asStateFlow()

    fun convertGroupToOnline(
        onSuccess: (inviteCode: String) -> Unit,
        onError: (String) -> Unit
    ) {
        if (remoteRepository == null) {
            onError("Authentication error: please log in to convert groups to online.")
            return
        }

        viewModelScope.launch {
            val currentGroup = _group.value ?: repository.getGroupById(groupId)
            if (currentGroup == null) {
                onError("Group not found")
                return@launch
            }
            if (currentGroup.mode == "ONLINE") {
                onError("Group is already online.")
                return@launch
            }

            _isConverting.value = true
            try {
                val bundle = repository.getGroupExportBundle(groupId)

                val groupObj = com.google.gson.JsonObject().apply {
                    addProperty("id", bundle.group.id)
                    addProperty("name", bundle.group.name)
                    addProperty("createdAt", bundle.group.createdAt)
                }

                val tablesArr = com.google.gson.JsonArray()
                bundle.tables.forEach { t ->
                    val obj = com.google.gson.JsonObject().apply {
                        addProperty("id", t.id)
                        addProperty("groupId", t.groupId ?: groupId)
                        addProperty("name", t.name)
                        if (t.chipValue != null) addProperty("chipValue", t.chipValue)
                        addProperty("status", t.status)
                        addProperty("createdAt", t.createdAt)
                        if (t.closedAt != null) addProperty("closedAt", t.closedAt)
                        addProperty("hasEntryFee", t.hasEntryFee)
                        if (t.entryFee != null) addProperty("entryFee", t.entryFee)
                    }
                    tablesArr.add(obj)
                }

                val playersArr = com.google.gson.JsonArray()
                bundle.players.forEach { p ->
                    val obj = com.google.gson.JsonObject().apply {
                        addProperty("id", p.id)
                        addProperty("tableId", p.tableId)
                        addProperty("name", p.name)
                        addProperty("status", p.status)
                        addProperty("entryFeePaid", p.entryFeePaid)
                        addProperty("createdAt", p.createdAt)
                    }
                    playersArr.add(obj)
                }

                val buyInsArr = com.google.gson.JsonArray()
                bundle.buyIns.forEach { b ->
                    val obj = com.google.gson.JsonObject().apply {
                        addProperty("id", b.id)
                        addProperty("tableId", b.tableId)
                        addProperty("playerId", b.playerId)
                        addProperty("amount", b.amount)
                        addProperty("createdAt", b.createdAt)
                    }
                    buyInsArr.add(obj)
                }

                val exitsArr = com.google.gson.JsonArray()
                bundle.exits.forEach { e ->
                    val obj = com.google.gson.JsonObject().apply {
                        addProperty("id", e.id)
                        addProperty("tableId", e.tableId)
                        addProperty("playerId", e.playerId)
                        addProperty("amount", e.amount)
                        addProperty("createdAt", e.createdAt)
                    }
                    exitsArr.add(obj)
                }

                val paymentsArr = com.google.gson.JsonArray()
                bundle.payments.forEach { pm ->
                    val obj = com.google.gson.JsonObject().apply {
                        addProperty("id", pm.id)
                        addProperty("groupId", pm.groupId)
                        addProperty("fromPlayer", pm.fromPlayer)
                        addProperty("toPlayer", pm.toPlayer)
                        addProperty("amount", pm.amount)
                        addProperty("createdAt", pm.createdAt)
                    }
                    paymentsArr.add(obj)
                }

                val settlementsArr = com.google.gson.JsonArray()
                bundle.settlements.forEach { s ->
                    val obj = com.google.gson.JsonObject().apply {
                        addProperty("id", s.id)
                        addProperty("groupId", s.groupId)
                        addProperty("tableId", s.tableId)
                        addProperty("tableName", s.tableName)
                        addProperty("payerName", s.payerName)
                        addProperty("receiverName", s.receiverName)
                        addProperty("amount", s.amount)
                        addProperty("initialAmount", s.initialAmount)
                        addProperty("paid", s.paid)
                        addProperty("timestamp", s.timestamp)
                    }
                    settlementsArr.add(obj)
                }

                val entryFeesArr = com.google.gson.JsonArray()
                bundle.entryFees.forEach { ef ->
                    val obj = com.google.gson.JsonObject().apply {
                        addProperty("id", ef.id)
                        addProperty("groupId", ef.groupId)
                        addProperty("tableId", ef.tableId)
                        addProperty("tableName", ef.tableName)
                        addProperty("playerName", ef.playerName)
                        addProperty("amount", ef.amount)
                        addProperty("paid", ef.paid)
                        addProperty("timestamp", ef.timestamp)
                    }
                    entryFeesArr.add(obj)
                }

                val root = com.google.gson.JsonObject().apply {
                    add("group", groupObj)
                    add("tables", tablesArr)
                    add("players", playersArr)
                    add("buyIns", buyInsArr)
                    add("exits", exitsArr)
                    add("payments", paymentsArr)
                    add("settlements", settlementsArr)
                    add("entryFees", entryFeesArr)
                }

                val result = remoteRepository.importGroup(root)
                if (result.isSuccess) {
                    val resp = result.getOrNull()!!
                    repository.updateGroupAfterOnlineConversion(
                        groupId = groupId,
                        serverId = resp.groupId,
                        inviteCode = resp.inviteCode
                    )
                    _group.value = _group.value?.copy(
                        mode = "ONLINE",
                        serverId = resp.groupId,
                        inviteCode = resp.inviteCode
                    )
                    // Fire-and-forget sync invite code to guarantee server matches local
                    syncInviteCodeToServer(resp.inviteCode)
                    onSuccess(resp.inviteCode)
                } else {
                    val err = result.exceptionOrNull()?.message ?: "Conversion failed. Data remains local."
                    onError(err)
                }
            } catch (e: Exception) {
                onError(e.message ?: "Failed to convert group to online. Data remains local.")
            } finally {
                _isConverting.value = false
            }
        }
    }

    fun syncInviteCodeToServer(inviteCode: String) {
        if (remoteRepository == null || inviteCode.isBlank()) return
        val currentGroup = _group.value
        val serverGroupId = currentGroup?.serverId ?: currentGroup?.id ?: groupId
        viewModelScope.launch {
            try {
                remoteRepository.syncInviteCode(serverGroupId, inviteCode)
            } catch (e: Exception) {
                android.util.Log.e("GroupDetailViewModel", "Error syncing invite code to server", e)
            }
        }
    }
}

class GroupDetailViewModelFactory(
    private val repository: PokerRepository,
    private val groupId: String,
    private val remoteRepository: RemoteRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupDetailViewModel(repository, groupId, remoteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
