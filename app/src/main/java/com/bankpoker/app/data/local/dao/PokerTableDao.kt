package com.bankpoker.app.data.local.dao

import androidx.room.*
import com.bankpoker.app.data.local.entity.PokerTable
import kotlinx.coroutines.flow.Flow

@Dao
interface PokerTableDao {
    @Query("SELECT * FROM poker_tables ORDER BY createdAt DESC")
    fun getAllTables(): Flow<List<PokerTable>>

    @Query("SELECT * FROM poker_tables WHERE id = :tableId")
    suspend fun getTableById(tableId: String): PokerTable?

    @Insert
    suspend fun insertTable(table: PokerTable)

    @Update
    suspend fun updateTable(table: PokerTable)

    @Query("UPDATE poker_tables SET status = :status, closedAt = :closedAt WHERE id = :tableId")
    suspend fun closeTable(tableId: String, status: String, closedAt: Long)

    @Query("SELECT * FROM poker_tables ORDER BY createdAt DESC")
    suspend fun getAllTablesOnce(): List<PokerTable>

    @Query("DELETE FROM poker_tables WHERE id = :tableId")
    suspend fun deleteTable(tableId: String)

    @Query("SELECT * FROM poker_tables WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getTablesByGroupId(groupId: String): Flow<List<PokerTable>>
}
