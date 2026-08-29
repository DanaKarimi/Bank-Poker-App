package com.bankpoker.app.data.local.dao

import androidx.room.*
import com.bankpoker.app.data.local.entity.SettlementRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SettlementRecordDao {
    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY timestamp ASC")
    fun getSettlementsByGroupId(groupId: String): Flow<List<SettlementRecord>>

    @Query("SELECT * FROM settlements WHERE groupId = :groupId AND paid = 0 ORDER BY timestamp ASC")
    fun getUnpaidSettlementsByGroupId(groupId: String): Flow<List<SettlementRecord>>

    @Query("SELECT * FROM settlements WHERE groupId = :groupId AND paid = 0 ORDER BY timestamp ASC")
    suspend fun getUnpaidSettlementsByGroupIdOnce(groupId: String): List<SettlementRecord>

    @Query("SELECT * FROM settlements WHERE groupId = :groupId AND payerName = :payerName AND receiverName = :receiverName AND paid = 0 ORDER BY timestamp ASC")
    suspend fun getUnpaidSettlementsForPair(groupId: String, payerName: String, receiverName: String): List<SettlementRecord>

    @Query("SELECT * FROM settlements WHERE groupId = :groupId AND payerName = :payerName AND receiverName = :receiverName ORDER BY timestamp ASC")
    suspend fun getAllSettlementsForPair(groupId: String, payerName: String, receiverName: String): List<SettlementRecord>

    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY timestamp ASC")
    suspend fun getAllSettlementsByGroupIdOnce(groupId: String): List<SettlementRecord>

    @Query("SELECT * FROM settlements WHERE id = :id")
    suspend fun getSettlementById(id: String): SettlementRecord?

    @Query("UPDATE settlements SET paid = 1 WHERE id = :id")
    suspend fun markSettlementPaid(id: String)

    @Query("UPDATE settlements SET amount = :newAmount, paid = :paid WHERE id = :id")
    suspend fun updateSettlementAmountAndPaid(id: String, newAmount: Long, paid: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: SettlementRecord)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlements(settlements: List<SettlementRecord>)

    @Update
    suspend fun updateSettlement(settlement: SettlementRecord)

    @Query("DELETE FROM settlements WHERE groupId = :groupId")
    suspend fun deleteSettlementsByGroupId(groupId: String)

    @Query("DELETE FROM settlements WHERE tableId = :tableId")
    suspend fun deleteSettlementsByTableId(tableId: String)

    @Query("SELECT * FROM settlements")
    suspend fun getAllSettlementsOnce(): List<SettlementRecord>

    @Query("DELETE FROM settlements")
    suspend fun deleteAllSettlements()
}
