package com.bankpoker.app.data.local.dao

import androidx.room.*
import com.bankpoker.app.data.local.entity.Payment
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentDao {
    @Query("SELECT * FROM payments WHERE groupId = :groupId ORDER BY createdAt DESC")
    fun getPaymentsByGroupId(groupId: String): Flow<List<Payment>>

    @Query("SELECT * FROM payments WHERE id = :id")
    suspend fun getPaymentById(id: String): Payment?

    @Query("SELECT * FROM payments WHERE groupId = :groupId AND fromPlayer = :fromPlayer AND toPlayer = :toPlayer ORDER BY createdAt ASC")
    suspend fun getPaymentsForPair(groupId: String, fromPlayer: String, toPlayer: String): List<Payment>

    @Insert
    suspend fun insertPayment(payment: Payment)

    @Update
    suspend fun updatePayment(payment: Payment)

    @Query("DELETE FROM payments WHERE id = :id")
    suspend fun deletePaymentById(id: String)

    @Query("DELETE FROM payments WHERE groupId = :groupId")
    suspend fun deletePaymentsByGroupId(groupId: String)

    @Query("SELECT * FROM payments")
    suspend fun getAllPaymentsOnce(): List<Payment>

    @Query("DELETE FROM payments")
    suspend fun deleteAllPayments()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayments(payments: List<Payment>)
}


