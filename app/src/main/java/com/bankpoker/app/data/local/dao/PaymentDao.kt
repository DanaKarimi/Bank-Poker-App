package com.bankpoker.app.data.local.dao

import androidx.room.*
import com.bankpoker.app.data.local.entity.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getPaymentsByGroupId(groupId: String): Flow<List<Payment>>

    @Insert
    suspend fun insertPayment(payment: Payment)

    @Query("DELETE FROM payments WHERE groupId = :groupId")
    suspend fun deletePaymentsByGroupId(groupId: String)

    @Query("SELECT * FROM payments")
    suspend fun getAllPaymentsOnce(): List<Payment>

    @Query("DELETE FROM payments")
    suspend fun deleteAllPayments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<Payment>)
}


