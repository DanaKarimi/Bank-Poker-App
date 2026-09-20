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
import kotlinx.coroutines.flow.map
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
        .map { records -> deduplicateRecords(records) }

    private val _canManageEntryFees = MutableStateFlow(false)
    val canManageEntryFees: StateFlow<Boolean> = _canManageEntryFees.asStateFlow()

    init {
        viewModelScope.launch {
            _group.value = repository.getGroupById(groupId)
        }
        if (remoteRepository == null) {
            _canManageEntryFees.value = true
        } else if (remoteRepository.tokenManager.getRole() == "SUPER_ADMIN") {
            _canManageEntryFees.value = true
        }
        fetchEntryFeesFromServer()
        fetchPaymentsFromServer()
    }

    fun fetchPaymentsFromServer() {
        if (remoteRepository == null) return
        viewModelScope.launch {
            try {
                val groupObj = repository.getGroupById(groupId)
                val targetGroupId = groupObj?.serverId ?: groupId
                val res = remoteRepository.getGroupPayments(targetGroupId)
                if (res.isSuccess) {
                    val serverList = res.getOrNull() ?: emptyList()
                    val roomPayments = serverList.map { dto ->
                        Payment(
                            id = dto.id,
                            groupId = groupId,
                            fromPlayer = dto.fromPlayer,
                            toPlayer = dto.toPlayer,
                            amount = dto.amount,
                            createdAt = if (dto.createdAt > 0) dto.createdAt else System.currentTimeMillis()
                        )
                    }
                    repository.replaceGroupPayments(groupId, roomPayments)
                }
            } catch (e: Exception) {
                android.util.Log.w("GroupHistoryVM", "Error fetching payments from server", e)
            }
        }
    }

    fun fetchEntryFeesFromServer() {
        if (remoteRepository == null) return
        viewModelScope.launch {
            try {
                val groupObj = repository.getGroupById(groupId)
                val targetGroupId = groupObj?.serverId ?: groupId
                val res = remoteRepository.getGroupEntryFeesResponse(targetGroupId)
                if (res.isSuccess) {
                    val body = res.getOrNull()
                    val serverList = body?.entryFees ?: emptyList()
                    val isSuperAdmin = remoteRepository.tokenManager.getRole() == "SUPER_ADMIN"
                    _canManageEntryFees.value = isSuperAdmin || (body?.canManage == true) || (body?.isAdmin == true)
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
                    val deduplicated = deduplicateRecords(roomRecords)
                    repository.replaceGroupEntryFeeRecords(groupId, deduplicated)
                }
            } catch (e: Exception) {
                android.util.Log.w("GroupHistoryVM", "Error fetching entry fees from server", e)
            }
        }
    }

    private fun deduplicateRecords(records: List<EntryFeeRecord>): List<EntryFeeRecord> {
        val result = mutableListOf<EntryFeeRecord>()
        for (record in records) {
            val normPlayer = record.playerName.trim().lowercase()
            val normTableId = record.tableId.trim()
            val normTableName = record.tableName.trim().lowercase()

            val existingIndex = result.indexOfFirst { existing ->
                val samePlayer = existing.playerName.trim().lowercase() == normPlayer
                if (!samePlayer) return@indexOfFirst false
                val sameTableId = normTableId.isNotBlank() && existing.tableId.trim() == normTableId
                val sameTableName = normTableName.isNotBlank() && existing.tableName.trim().lowercase() == normTableName
                sameTableId || sameTableName
            }

            if (existingIndex >= 0) {
                val existing = result[existingIndex]
                result[existingIndex] = existing.copy(
                    paid = existing.paid || record.paid,
                    amount = if (record.amount > 0) record.amount else existing.amount,
                    timestamp = maxOf(existing.timestamp, record.timestamp),
                    tableId = if (existing.tableId.isBlank()) record.tableId else existing.tableId,
                    tableName = if (existing.tableName.isBlank()) record.tableName else existing.tableName
                )
            } else {
                result.add(record)
            }
        }
        return result
    }

    fun updatePayment(paymentId: String, newAmount: Long) {
        viewModelScope.launch {
            val existing = repository.getPaymentById(paymentId)
            repository.updatePaymentAmount(paymentId, newAmount)
            if (remoteRepository != null) {
                try {
                    val groupObj = repository.getGroupById(groupId)
                    val targetGroupId = groupObj?.serverId ?: groupId
                    val res = remoteRepository.updateGroupPayment(
                        groupId = targetGroupId,
                        paymentId = paymentId,
                        amount = newAmount,
                        fromPlayer = existing?.fromPlayer,
                        toPlayer = existing?.toPlayer
                    )
                    if (res.isFailure) {
                        fetchPaymentsFromServer()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GroupHistoryVM", "Error updating payment on server", e)
                    fetchPaymentsFromServer()
                }
            }
        }
    }

    fun deletePayment(paymentId: String) {
        viewModelScope.launch {
            repository.deletePayment(paymentId)
            if (remoteRepository != null) {
                try {
                    val groupObj = repository.getGroupById(groupId)
                    val targetGroupId = groupObj?.serverId ?: groupId
                    val res = remoteRepository.deleteGroupPayment(targetGroupId, paymentId)
                    if (res.isFailure) {
                        fetchPaymentsFromServer()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GroupHistoryVM", "Error deleting payment on server", e)
                    fetchPaymentsFromServer()
                }
            }
        }
    }

    fun updateEntryFeeRecord(id: String, amount: Long, paid: Boolean) {
        viewModelScope.launch {
            if (remoteRepository != null) {
                // Optimistic local update
                repository.updateEntryFeeRecord(id, amount, paid)
                try {
                    val groupObj = repository.getGroupById(groupId)
                    val targetGroupId = groupObj?.serverId ?: groupId
                    val result = remoteRepository.updateEntryFeeRecord(targetGroupId, id, paid, amount)
                    if (result.isSuccess) {
                        val updated = result.getOrNull()
                        if (updated != null) {
                            repository.updateEntryFeeRecord(updated.id, updated.amount, updated.paid)
                        }
                    } else {
                        // Revert on error (e.g. 403 Forbidden)
                        fetchEntryFeesFromServer()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GroupHistoryVM", "Error updating entry fee on server", e)
                    fetchEntryFeesFromServer()
                }
            } else {
                repository.updateEntryFeeRecord(id, amount, paid)
            }
        }
    }

    fun deleteEntryFeeRecord(id: String) {
        viewModelScope.launch {
            repository.deleteEntryFeeRecord(id)
            if (remoteRepository != null) {
                try {
                    val groupObj = repository.getGroupById(groupId)
                    val targetGroupId = groupObj?.serverId ?: groupId
                    val result = remoteRepository.deleteEntryFeeRecord(targetGroupId, id)
                    if (result.isFailure) {
                        fetchEntryFeesFromServer()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("GroupHistoryVM", "Error deleting entry fee on server", e)
                    fetchEntryFeesFromServer()
                }
            }
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
