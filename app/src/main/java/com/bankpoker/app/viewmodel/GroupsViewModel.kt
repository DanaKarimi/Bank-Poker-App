package com.bankpoker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bankpoker.app.data.local.entity.PlayerGroup
import com.bankpoker.app.repository.PokerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class GroupsViewModel(private val repository: PokerRepository) : ViewModel() {
    val groups: Flow<List<PlayerGroup>> = repository.getAllGroups()

    fun createGroup(name: String) {
        viewModelScope.launch {
            repository.createGroup(name)
        }
    }
}

class GroupsViewModelFactory(
    private val repository: PokerRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GroupsViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
