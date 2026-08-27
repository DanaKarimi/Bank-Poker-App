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
import com.bankpoker.app.repository.PokerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupDetailViewModel(
    private val repository: PokerRepository,
    private val groupId: String
) : ViewModel() {

    private val _group = MutableStateFlow<PlayerGroup?>(null)
    val group: StateFlow<PlayerGroup?> = _group.asStateFlow()

    val tables: Flow<List<PokerTable>> = repository.getTablesByGroupId(groupId)
    val balances: Flow<List<GroupBalance>> = repository.getBalancesByGroupId(groupId)
    val payments: Flow<List<Payment>> = repository.getPaymentsByGroupId(groupId)
    val entryFeeDebtors: Flow<List<UnpaidEntryFeeInfo>> = repository.getUnpaidEntryFeeDebtorsByGroupId(groupId)
    val entryFeeHistory: Flow<List<EntryFeeHistoryInfo>> = repository.getEntryFeeHistoryByGroupId(groupId)

    fun markEntryFeePaid(playerId: String) {
        viewModelScope.launch {
            repository.toggleEntryFee(playerId, true)
        }
    }



    init {
        viewModelScope.launch {
            _group.value = repository.getGroupById(groupId)
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


    fun createTable(name: String, chipValue: Long?, hasEntryFee: Boolean, entryFee: Long?) {
        viewModelScope.launch {
            repository.createTable(
                name = name,
                chipValue = chipValue,
                groupId = groupId,
                hasEntryFee = hasEntryFee,
                entryFee = entryFee
            )
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
        }
    }
}

class GroupDetailViewModelFactory(
    private val repository: PokerRepository,
    private val groupId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupDetailViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupDetailViewModel(repository, groupId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
