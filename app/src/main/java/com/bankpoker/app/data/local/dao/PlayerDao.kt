package com.bankpoker.app.data.local.dao

import androidx.room.*
import com.bankpoker.app.data.local.entity.Player
import kotlinx.coroutines.flow.Flow

@Dao
interface PlayerDao {
    @Query("SELECT * FROM players WHERE tableId = :tableId ORDER BY createdAt ASC")
    fun getPlayersByTableId(tableId: String): Flow<List<Player>>

    @Query("SELECT * FROM players WHERE id = :playerId")
    suspend fun getPlayerById(playerId: String): Player?

    @Insert
    suspend fun insertPlayer(player: Player)

    @Update
    suspend fun updatePlayer(player: Player)

    @Query("UPDATE players SET status = :status WHERE id = :playerId")
    suspend fun updatePlayerStatus(playerId: String, status: String)

    @Query("SELECT COUNT(*) FROM players WHERE tableId = :tableId AND status = 'PLAYING'")
    suspend fun getPlayingPlayersCount(tableId: String): Int

    @Query("SELECT name FROM players GROUP BY name ORDER BY COUNT(*) DESC, MAX(createdAt) DESC")
    suspend fun getAllPlayerNames(): List<String>

    @Query("SELECT * FROM players")
    suspend fun getAllPlayersOnce(): List<Player>
}
