package com.bankpoker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bankpoker.app.data.local.entity.PlayerProfileData
import com.bankpoker.app.repository.PokerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class PlayerProfileViewModel(
    private val repository: PokerRepository,
    val playerName: String
) : ViewModel() {

    val profileData: StateFlow<PlayerProfileData> = repository.getPlayerProfile(playerName)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = PlayerProfileData(playerName = playerName)
        )
}

class PlayerProfileViewModelFactory(
    private val repository: PokerRepository,
    private val playerName: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerProfileViewModel::class.java)) {
            return PlayerProfileViewModel(repository, playerName) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
