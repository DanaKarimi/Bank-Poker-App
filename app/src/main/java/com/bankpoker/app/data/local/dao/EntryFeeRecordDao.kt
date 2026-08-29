package com.bankpoker.app.data.local.dao

import androidx.room.*
import com.bankpoker.app.data.local.entity.EntryFeeRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryFeeRecordDao {
    @Query("SELECT * FROM entry_fee_records WHERE groupId = :groupId ORDER BY timestamp DESC")
    fun getEntryFeeRecordsByGroupId(groupId: String): Flow<List<EntryFeeRecord>>

    @Query("SELECT * FROM entry_fee_records WHERE groupId = :groupId AND paid = 0 ORDER BY timestamp DESC")
    fun getUnpaidEntryFeeRecordsByGroupId(groupId: String): Flow<List<EntryFeeRecord>>

    @Query("SELECT * FROM entry_fee_records WHERE id = :id")
    suspend fun getEntryFeeRecordById(id: String): EntryFeeRecord?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntryFeeRecord(record: EntryFeeRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntryFeeRecords(records: List<EntryFeeRecord>)

    @Update
    suspend fun updateEntryFeeRecord(record: EntryFeeRecord)

    @Query("UPDATE entry_fee_records SET paid = :paid WHERE id = :id")
    suspend fun updateEntryFeeRecordPaid(id: String, paid: Boolean)

    @Query("UPDATE entry_fee_records SET amount = :amount, paid = :paid WHERE id = :id")
    suspend fun updateEntryFeeRecordAmountAndPaid(id: String, amount: Long, paid: Boolean)

    @Query("DELETE FROM entry_fee_records WHERE id = :id")
    suspend fun deleteEntryFeeRecordById(id: String)

    @Query("DELETE FROM entry_fee_records WHERE groupId = :groupId")
    suspend fun deleteEntryFeeRecordsByGroupId(groupId: String)

    @Query("DELETE FROM entry_fee_records WHERE tableId = :tableId")
    suspend fun deleteEntryFeeRecordsByTableId(tableId: String)

    @Query("SELECT * FROM entry_fee_records")
    suspend fun getAllEntryFeeRecordsOnce(): List<EntryFeeRecord>

    @Query("DELETE FROM entry_fee_records")
    suspend fun deleteAllEntryFeeRecords()
}
