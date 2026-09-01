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
            if (currentGroup?.mode == "ONLINE" && remoteRepository != null) {
                remoteRepository.recordPayment(groupId, payerName, receiverName, amount)
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
            if (currentGroup?.mode == "ONLINE" && remoteRepository != null) {
                // Online group: API call -> Server success -> Room Insert
                val result = remoteRepository.createTable(
                    groupId = groupId,
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
            if (currentGroup?.mode == "ONLINE" && remoteRepository != null) {
                remoteRepository.recordPayment(groupId, fromPlayer, toPlayer, amount)
            }
        }
    }

    fun syncSettlementPlan(settlements: List<com.bankpoker.app.ui.screens.Settlement>) {
        viewModelScope.launch {
            val currentGroup = _group.value ?: repository.getGroupById(groupId)
            if (currentGroup?.mode == "ONLINE" && remoteRepository != null) {
                remoteRepository.syncSettlement(groupId, settlements)
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
