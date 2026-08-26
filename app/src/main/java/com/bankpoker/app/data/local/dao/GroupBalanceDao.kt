package com.bankpoker.app.data.local.dao

import androidx.room.*
import com.bankpoker.app.data.local.entity.GroupBalance
import kotlinx.coroutines.flow.Flow

@Dao
interface GroupBalanceDao {
    @Query("SELECT * FROM group_balances WHERE groupId = :groupId ORDER BY balance ASC")
    fun getBalancesByGroupId(groupId: String): Flow<List<GroupBalance>>

    @Query("SELECT * FROM group_balances WHERE groupId = :groupId AND playerName = :name")
    suspend fun getBalance(groupId: String, name: String): GroupBalance?

    @Insert
    suspend fun insertBalance(balance: GroupBalance)

    @Update
    suspend fun updateBalance(balance: GroupBalance)

    @Query("DELETE FROM group_balances WHERE groupId = :groupId")
    suspend fun deleteBalancesByGroupId(groupId: String)
}

