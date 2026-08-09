package com.bankpoker.app.data.local.dao

import androidx.room.*
import com.bankpoker.app.data.local.entity.ExitRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface ExitRecordDao {
    @Query("SELECT * FROM exit_records WHERE tableId = :tableId ORDER BY createdAt DESC")
    fun getExitRecordsByTableId(tableId: String): Flow<List<ExitRecord>>

    @Query("SELECT * FROM exit_records WHERE playerId = :playerId ORDER BY createdAt DESC")
    fun getExitRecordsByPlayerId(playerId: String): Flow<List<ExitRecord>>

    @Query("SELECT SUM(amount) FROM exit_records WHERE tableId = :tableId")
    suspend fun getTotalExitsForTable(tableId: String): Long?

    @Query("SELECT SUM(amount) FROM exit_records WHERE playerId = :playerId")
    suspend fun getTotalExitsForPlayer(playerId: String): Long?

    @Insert
    suspend fun insertExitRecord(exitRecord: ExitRecord)

    @Update
    suspend fun updateExitRecord(exitRecord: ExitRecord)

    @Delete
    suspend fun deleteExitRecord(exitRecord: ExitRecord)

    @Query("SELECT COUNT(*) FROM exit_records WHERE playerId = :playerId")
    suspend fun getExitCountByPlayer(playerId: String): Int
}
