package com.bankpoker.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.bankpoker.app.repository.PokerRepository
import com.bankpoker.app.repository.RemoteRepository

class TableDetailViewModelFactory(
    private val repository: PokerRepository,
    private val tableId: String,
    private val remoteRepository: RemoteRepository? = null
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TableDetailViewModel::class.java)) {
            return TableDetailViewModel(repository, tableId, remoteRepository = remoteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}