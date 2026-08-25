package com.bankpoker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bankpoker.app.data.local.entity.GroupBalance
import com.bankpoker.app.data.local.entity.Payment
import com.bankpoker.app.data.local.entity.PlayerGroup
import com.bankpoker.app.data.local.entity.PokerTable
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

    init {
        viewModelScope.launch {
            _group.value = repository.getGroupById(groupId)
        }
    }

    fun createTable(name: String, chipValue: Long?, hasEntryFee: Boolean = false, entryFee: Long? = null) {
        viewModelScope.launch {
            repository.createTable(name, chipValue, groupId, hasEntryFee, entryFee)
        }
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
