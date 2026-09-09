package com.bankpoker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bankpoker.app.data.local.entity.PlayerGroup
import com.bankpoker.app.data.remote.dto.UserGroupSummaryDto
import com.bankpoker.app.repository.PokerRepository
import com.bankpoker.app.repository.RemoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupsViewModel(
    private val repository: PokerRepository,
    private val remoteRepository: RemoteRepository? = null
) : ViewModel() {
    val groups: Flow<List<PlayerGroup>> = repository.getAllGroups()

    private val _serverGroups = MutableStateFlow<Map<String, UserGroupSummaryDto>>(emptyMap())
    val serverGroups: StateFlow<Map<String, UserGroupSummaryDto>> = _serverGroups.asStateFlow()

    init {
        loadServerGroups()
    }

    fun loadServerGroups() {
        if (remoteRepository == null) return
        viewModelScope.launch {
            val res = remoteRepository.getMyGroups()
            if (res.isSuccess) {
                val list = res.getOrNull() ?: emptyList()
                val map = mutableMapOf<String, UserGroupSummaryDto>()
                list.forEach { dto ->
                    map[dto.id] = dto
                    if (!dto.serverId.isNullOrBlank()) {
                        map[dto.serverId] = dto
                    }
                }
                _serverGroups.value = map
            }
        }
    }

    fun createGroup(
        name: String,
        onSuccess: ((PlayerGroup) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val trimmed = name.trim()
            if (remoteRepository != null) {
                val result = remoteRepository.createGroup(trimmed)
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    val serverGroupId = response?.groupId
                    val inviteCode = response?.inviteCode
                    val created = repository.createGroup(
                        name = trimmed,
                        mode = "ONLINE",
                        serverId = serverGroupId,
                        inviteCode = inviteCode,
                        customId = serverGroupId
                    )
                    onSuccess?.invoke(created)
                    return@launch
                } else {
                    android.util.Log.w("GroupsVM", "Server group creation failed; falling back to local creation", result.exceptionOrNull())
                }
            }
            // Local creation (with outbox if offline)
            val localId = java.util.UUID.randomUUID().toString()
            val created = repository.createGroup(
                name = trimmed,
                mode = "ONLINE",
                customId = localId
            )
            repository.enqueueOutbox("create_group", localId, "{\"name\":\"$trimmed\"}")
            onSuccess?.invoke(created)
        }
    }
}

class GroupsViewModelFactory(
    private val repository: PokerRepository,
    private val remoteRepository: RemoteRepository? = null
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupsViewModel(repository, remoteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
