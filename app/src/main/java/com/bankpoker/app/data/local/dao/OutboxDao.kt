package com.bankpoker.app.data.local.dao

import androidx.room.*
import com.bankpoker.app.data.local.entity.OutboxRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox_operations WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingOperations(): List<OutboxRecord>

    @Query("SELECT * FROM outbox_operations ORDER BY createdAt DESC")
    fun getAllOperations(): Flow<List<OutboxRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOperation(record: OutboxRecord)

    @Update
    suspend fun updateOperation(record: OutboxRecord)

    @Query("DELETE FROM outbox_operations WHERE id = :id")
    suspend fun deleteOperation(id: String)

    @Query("DELETE FROM outbox_operations WHERE status = 'SUCCESS'")
    suspend fun deleteCompletedOperations()

    @Query("SELECT COUNT(*) FROM outbox_operations WHERE status = 'PENDING'")
    fun getPendingCount(): Flow<Int>
}
