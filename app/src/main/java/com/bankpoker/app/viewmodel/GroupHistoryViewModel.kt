package com.bankpoker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bankpoker.app.data.local.entity.EntryFeeRecord
import com.bankpoker.app.data.local.entity.Payment
import com.bankpoker.app.data.local.entity.PlayerGroup
import com.bankpoker.app.repository.PokerRepository
import com.bankpoker.app.repository.RemoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupHistoryViewModel(
    private val repository: PokerRepository,
    private val groupId: String,
    private val remoteRepository: RemoteRepository? = null
) : ViewModel() {

    private val _group = MutableStateFlow<PlayerGroup?>(null)
    val group: StateFlow<PlayerGroup?> = _group.asStateFlow()

    val payments: Flow<List<Payment>> = repository.getPaymentsByGroupId(groupId)
    val entryFeeRecords: Flow<List<EntryFeeRecord>> = repository.getEntryFeeRecordsByGroupId(groupId)

    init {
        viewModelScope.launch {
            _group.value = repository.getGroupById(groupId)
        }
        fetchEntryFeesFromServer()
    }

    fun fetchEntryFeesFromServer() {
        if (remoteRepository == null) return
        viewModelScope.launch {
            try {
                val groupObj = repository.getGroupById(groupId)
                val targetGroupId = groupObj?.serverId ?: groupId
                val res = remoteRepository.getGroupEntryFees(targetGroupId)
                if (res.isSuccess) {
                    val serverList = res.getOrNull() ?: emptyList()
                    val roomRecords = serverList.map { dto ->
                        EntryFeeRecord(
                            id = dto.id,
                            groupId = groupId,
                            tableId = dto.tableId ?: "",
                            tableName = dto.tableName ?: "",
                            playerName = dto.playerName,
                            amount = dto.amount,
                            paid = dto.paid,
                            timestamp = if (dto.timestamp > 0) dto.timestamp else System.currentTimeMillis()
                        )
                    }
                    if (roomRecords.isNotEmpty()) {
                        repository.insertOrUpdateEntryFeeRecords(roomRecords)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("GroupHistoryVM", "Error fetching entry fees from server", e)
            }
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
            // Update Room locally for immediate UI responsiveness
            repository.updateEntryFeeRecord(id, amount, paid)

            // Server-authoritative update via PUT /api/groups/:id/entry-fees/:feeId
            if (remoteRepository != null) {
                try {
                    val groupObj = repository.getGroupById(groupId)
                    val targetGroupId = groupObj?.serverId ?: groupId
                    val result = remoteRepository.updateEntryFeeRecord(targetGroupId, id, paid, amount)
                    if (result.isSuccess) {
                        val updated = result.getOrNull()
                        if (updated != null) {
                            repository.updateEntryFeeRecord(updated.id, updated.amount, updated.paid)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GroupHistoryVM", "Error updating entry fee on server", e)
                }
            }
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
    private val groupId: String,
    private val remoteRepository: RemoteRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupHistoryViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupHistoryViewModel(repository, groupId, remoteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
