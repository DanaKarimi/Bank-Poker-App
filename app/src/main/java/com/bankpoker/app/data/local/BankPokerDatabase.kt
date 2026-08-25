package com.bankpoker.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.bankpoker.app.data.local.dao.BuyInDao
import com.bankpoker.app.data.local.dao.ExitRecordDao
import com.bankpoker.app.data.local.dao.GroupBalanceDao
import com.bankpoker.app.data.local.dao.PaymentDao
import com.bankpoker.app.data.local.dao.PlayerDao
import com.bankpoker.app.data.local.dao.PlayerGroupDao
import com.bankpoker.app.data.local.dao.PokerTableDao
import com.bankpoker.app.data.local.entity.BuyIn
import com.bankpoker.app.data.local.entity.ExitRecord
import com.bankpoker.app.data.local.entity.GroupBalance
import com.bankpoker.app.data.local.entity.Payment
import com.bankpoker.app.data.local.entity.Player
import com.bankpoker.app.data.local.entity.PlayerGroup
import com.bankpoker.app.data.local.entity.PokerTable

@Database(
    entities = [PokerTable::class, Player::class, BuyIn::class, ExitRecord::class, PlayerGroup::class, GroupBalance::class, Payment::class],
    version = 3,
    exportSchema = false
)
abstract class BankPokerDatabase : RoomDatabase() {
    abstract fun pokerTableDao(): PokerTableDao
    abstract fun playerDao(): PlayerDao
    abstract fun buyInDao(): BuyInDao
    abstract fun exitRecordDao(): ExitRecordDao
    abstract fun playerGroupDao(): PlayerGroupDao
    abstract fun groupBalanceDao(): GroupBalanceDao
    abstract fun paymentDao(): PaymentDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("CREATE TABLE IF NOT EXISTS `player_groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `group_balances` (`id` TEXT NOT NULL, `groupId` TEXT NOT NULL, `playerName` TEXT NOT NULL, `balance` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                database.execSQL("CREATE TABLE IF NOT EXISTS `payments` (`id` TEXT NOT NULL, `groupId` TEXT NOT NULL, `fromPlayer` TEXT NOT NULL, `toPlayer` TEXT NOT NULL, `amount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
                database.execSQL("ALTER TABLE `poker_tables` ADD COLUMN `groupId` TEXT")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `poker_tables` ADD COLUMN `hasEntryFee` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `poker_tables` ADD COLUMN `entryFee` INTEGER")
                database.execSQL("ALTER TABLE `players` ADD COLUMN `entryFeePaid` INTEGER NOT NULL DEFAULT 0")
            }
        }

        @Volatile
        private var INSTANCE: BankPokerDatabase? = null

        fun getDatabase(context: Context): BankPokerDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BankPokerDatabase::class.java,
                    "bank_poker_database"
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
