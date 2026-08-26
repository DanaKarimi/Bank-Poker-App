package com.bankpoker.app.data.local.dao

import androidx.room.*
import com.bankpoker.app.data.local.entity.Player
import com.bankpoker.app.data.local.entity.UnpaidVoroodiInfo
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

    @Query("DELETE FROM players WHERE tableId = :tableId")
    suspend fun deletePlayersForTable(tableId: String)

    @Query("SELECT * FROM players WHERE tableId = :tableId")
    suspend fun getPlayersForTableOnce(tableId: String): List<Player>

    @Query("UPDATE players SET entryFeePaid = :paid WHERE id = :playerId")
    suspend fun updateEntryFeePaid(playerId: String, paid: Boolean)

    @Query("UPDATE players SET status = 'EXITED' WHERE tableId = :tableId")
    suspend fun setAllPlayersExitedForTable(tableId: String)

    @Query("""
        SELECT 
            p.id AS playerId,
            p.name AS playerName,
            t.name AS tableName,
            g.name AS groupName,
            COALESCE(t.entryFee, 0) AS amount,
            p.createdAt AS timestamp
        FROM players p
        INNER JOIN poker_tables t ON p.tableId = t.id
        LEFT JOIN player_groups g ON t.groupId = g.id
        WHERE t.hasEntryFee = 1 
          AND p.entryFeePaid = 0 
          AND (p.status = 'EXITED' OR t.status = 'CLOSED')
        ORDER BY p.createdAt DESC
    """)
    fun getUnpaidVoroodiDebtors(): Flow<List<UnpaidVoroodiInfo>>
}

