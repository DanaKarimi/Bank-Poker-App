package com.bankpoker.app.data.local.dao

import androidx.room.*
import com.bankpoker.app.data.local.entity.PlayerGroup
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerGroupDao {
    @Query("SELECT * FROM player_groups ORDER BY createdAt DESC")
    fun getAllGroups(): Flow<List<PlayerGroup>>

    @Query("SELECT * FROM player_groups WHERE id = :groupId")
    suspend fun getGroupById(groupId: String): PlayerGroup?

    @Insert
    suspend fun insertGroup(group: PlayerGroup)

    @Query("DELETE FROM player_groups WHERE id = :groupId")
    suspend fun deleteGroup(groupId: String)

    @Query("UPDATE player_groups SET name = :newName WHERE id = :groupId")
    suspend fun updateGroupName(groupId: String, newName: String)

    @Query("SELECT * FROM player_groups")

    suspend fun getAllGroupsOnce(): List<PlayerGroup>

    @Query("DELETE FROM player_groups")
    suspend fun deleteAllGroups()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroups(groups: List<PlayerGroup>)
}


