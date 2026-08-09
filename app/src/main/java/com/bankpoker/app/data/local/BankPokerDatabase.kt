package com.bankpoker.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bankpoker.app.data.local.dao.BuyInDao
import com.bankpoker.app.data.local.dao.ExitRecordDao
import com.bankpoker.app.data.local.dao.PlayerDao
import com.bankpoker.app.data.local.dao.PokerTableDao
import com.bankpoker.app.data.local.entity.BuyIn
import com.bankpoker.app.data.local.entity.ExitRecord
import com.bankpoker.app.data.local.entity.Player
import com.bankpoker.app.data.local.entity.PokerTable

@Database(
    entities = [PokerTable::class, Player::class, BuyIn::class, ExitRecord::class],
    version = 1,
    exportSchema = false
)
abstract class BankPokerDatabase : RoomDatabase() {
    abstract fun pokerTableDao(): PokerTableDao
    abstract fun playerDao(): PlayerDao
    abstract fun buyInDao(): BuyInDao
    abstract fun exitRecordDao(): ExitRecordDao

    companion object {
        @Volatile
        private var INSTANCE: BankPokerDatabase? = null

        fun getDatabase(context: Context): BankPokerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BankPokerDatabase::class.java,
                    "bank_poker_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
