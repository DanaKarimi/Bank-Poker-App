package com.bankpoker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bankpoker.app.data.local.entity.PlayerGroup
import com.bankpoker.app.repository.PokerRepository
import com.bankpoker.app.repository.RemoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class GroupsViewModel(
    private val repository: PokerRepository,
    private val remoteRepository: RemoteRepository? = null
) : ViewModel() {
    val groups: Flow<List<PlayerGroup>> = repository.getAllGroups()

    fun createGroup(
        name: String,
        mode: String = "OFFLINE",
        onSuccess: ((PlayerGroup) -> Unit)? = null,
        onError: ((String) -> Unit)? = null
    ) {
        viewModelScope.launch {
            if (mode == "ONLINE" && remoteRepository != null) {
                val result = remoteRepository.createGroup(name.trim(), "ONLINE")
                if (result.isSuccess) {
                    val response = result.getOrNull()
                    val serverGroupId = response?.groupId
                    val inviteCode = response?.inviteCode
                    val created = repository.createGroup(
                        name = name.trim(),
                        mode = "ONLINE",
                        serverId = serverGroupId,
                        inviteCode = inviteCode,
                        customId = serverGroupId
                    )
                    onSuccess?.invoke(created)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to create online group on server."
                    onError?.invoke(errorMsg)
                }
            } else {
                // Offline group
                val created = repository.createGroup(name = name.trim(), mode = "OFFLINE")
                onSuccess?.invoke(created)
            }
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
