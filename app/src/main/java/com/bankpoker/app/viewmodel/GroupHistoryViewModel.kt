package com.bankpoker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bankpoker.app.data.local.entity.EntryFeeRecord
import com.bankpoker.app.data.local.entity.Payment
import com.bankpoker.app.data.local.entity.PlayerGroup
import com.bankpoker.app.repository.PokerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupHistoryViewModel(
    private val repository: PokerRepository,
    private val groupId: String
) : ViewModel() {

    private val _group = MutableStateFlow<PlayerGroup?>(null)
    val group: StateFlow<PlayerGroup?> = _group.asStateFlow()

    val payments: Flow<List<Payment>> = repository.getPaymentsByGroupId(groupId)
    val entryFeeRecords: Flow<List<EntryFeeRecord>> = repository.getEntryFeeRecordsByGroupId(groupId)

    init {
        viewModelScope.launch {
            _group.value = repository.getGroupById(groupId)
        }
    }

    fun updatePayment(paymentId: String, newAmount: Long) {
        viewModelScope.launch {
            repository.updatePaymentAmount(paymentId, newAmount)
        }
    }

    fun deletePayment(paymentId: String) {
        viewModelScope.launch {
            repository.deletePayment(paymentId)
        }
    }

    fun updateEntryFeeRecord(id: String, amount: Long, paid: Boolean) {
        viewModelScope.launch {
            repository.updateEntryFeeRecord(id, amount, paid)
        }
    }

    fun deleteEntryFeeRecord(id: String) {
        viewModelScope.launch {
            repository.deleteEntryFeeRecord(id)
        }
    }
}

class GroupHistoryViewModelFactory(
    private val repository: PokerRepository,
    private val groupId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupHistoryViewModel(repository, groupId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
