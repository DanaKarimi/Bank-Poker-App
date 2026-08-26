package com.bankpoker.app.data.local.dao

import androidx.room.*
import com.bankpoker.app.data.local.entity.BuyIn
import kotlinx.coroutines.flow.Flow

@Dao
interface BuyInDao {
    @Query("SELECT * FROM buy_ins WHERE tableId = :tableId ORDER BY createdAt DESC")
    fun getBuyInsByTableId(tableId: String): Flow<List<BuyIn>>

    @Query("SELECT * FROM buy_ins WHERE playerId = :playerId ORDER BY createdAt DESC")
    fun getBuyInsByPlayerId(playerId: String): Flow<List<BuyIn>>

    @Query("SELECT SUM(amount) FROM buy_ins WHERE tableId = :tableId")
    suspend fun getTotalBuyInsForTable(tableId: String): Long?

    @Query("SELECT SUM(amount) FROM buy_ins WHERE playerId = :playerId")
    suspend fun getTotalBuyInsForPlayer(playerId: String): Long?

    @Insert
    suspend fun insertBuyIn(buyIn: BuyIn)

    @Update
    suspend fun updateBuyIn(buyIn: BuyIn)

    @Delete
    suspend fun deleteBuyIn(buyIn: BuyIn)

    @Query("SELECT * FROM buy_ins")
    suspend fun getAllBuyInsOnce(): List<BuyIn>

    @Query("DELETE FROM buy_ins WHERE tableId = :tableId")
    suspend fun deleteBuyInsForTable(tableId: String)

    @Query("DELETE FROM buy_ins")
    suspend fun deleteAllBuyIns()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuyIns(buyIns: List<BuyIn>)
}

