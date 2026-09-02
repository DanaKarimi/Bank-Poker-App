package com.bankpoker.app.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.bankpoker.app.data.local.dao.BuyInDao;
import com.bankpoker.app.data.local.dao.BuyInDao_Impl;
import com.bankpoker.app.data.local.dao.EntryFeeRecordDao;
import com.bankpoker.app.data.local.dao.EntryFeeRecordDao_Impl;
import com.bankpoker.app.data.local.dao.ExitRecordDao;
import com.bankpoker.app.data.local.dao.ExitRecordDao_Impl;
import com.bankpoker.app.data.local.dao.GroupBalanceDao;
import com.bankpoker.app.data.local.dao.GroupBalanceDao_Impl;
import com.bankpoker.app.data.local.dao.PaymentDao;
import com.bankpoker.app.data.local.dao.PaymentDao_Impl;
import com.bankpoker.app.data.local.dao.PlayerDao;
import com.bankpoker.app.data.local.dao.PlayerDao_Impl;
import com.bankpoker.app.data.local.dao.PlayerGroupDao;
import com.bankpoker.app.data.local.dao.PlayerGroupDao_Impl;
import com.bankpoker.app.data.local.dao.PokerTableDao;
import com.bankpoker.app.data.local.dao.PokerTableDao_Impl;
import com.bankpoker.app.data.local.dao.SettlementRecordDao;
import com.bankpoker.app.data.local.dao.SettlementRecordDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class BankPokerDatabase_Impl extends BankPokerDatabase {
  private volatile PokerTableDao _pokerTableDao;

  private volatile PlayerDao _playerDao;

  private volatile BuyInDao _buyInDao;

  private volatile ExitRecordDao _exitRecordDao;

  private volatile PlayerGroupDao _playerGroupDao;

  private volatile GroupBalanceDao _groupBalanceDao;

  private volatile PaymentDao _paymentDao;

  private volatile SettlementRecordDao _settlementRecordDao;

  private volatile EntryFeeRecordDao _entryFeeRecordDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(6) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `poker_tables` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `chipValue` INTEGER, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `closedAt` INTEGER, `groupId` TEXT, `hasEntryFee` INTEGER NOT NULL, `entryFee` INTEGER, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `players` (`id` TEXT NOT NULL, `tableId` TEXT NOT NULL, `name` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `entryFeePaid` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `buy_ins` (`id` TEXT NOT NULL, `tableId` TEXT NOT NULL, `playerId` TEXT NOT NULL, `amount` INTEGER NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `exit_records` (`id` TEXT NOT NULL, `tableId` TEXT NOT NULL, `playerId` TEXT NOT NULL, `amount` INTEGER NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `player_groups` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `mode` TEXT NOT NULL, `serverId` TEXT, `inviteCode` TEXT, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `group_balances` (`id` TEXT NOT NULL, `groupId` TEXT NOT NULL, `playerName` TEXT NOT NULL, `balance` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `payments` (`id` TEXT NOT NULL, `groupId` TEXT NOT NULL, `fromPlayer` TEXT NOT NULL, `toPlayer` TEXT NOT NULL, `amount` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `settlements` (`id` TEXT NOT NULL, `groupId` TEXT NOT NULL, `tableId` TEXT NOT NULL, `tableName` TEXT NOT NULL, `payerName` TEXT NOT NULL, `receiverName` TEXT NOT NULL, `amount` INTEGER NOT NULL, `initialAmount` INTEGER NOT NULL, `paid` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `entry_fee_records` (`id` TEXT NOT NULL, `groupId` TEXT NOT NULL, `tableId` TEXT NOT NULL, `tableName` TEXT NOT NULL, `playerName` TEXT NOT NULL, `amount` INTEGER NOT NULL, `paid` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'f39ab3e697f609d6f77e7c24d0fb163b')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `poker_tables`");
        db.execSQL("DROP TABLE IF EXISTS `players`");
        db.execSQL("DROP TABLE IF EXISTS `buy_ins`");
        db.execSQL("DROP TABLE IF EXISTS `exit_records`");
        db.execSQL("DROP TABLE IF EXISTS `player_groups`");
        db.execSQL("DROP TABLE IF EXISTS `group_balances`");
        db.execSQL("DROP TABLE IF EXISTS `payments`");
        db.execSQL("DROP TABLE IF EXISTS `settlements`");
        db.execSQL("DROP TABLE IF EXISTS `entry_fee_records`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsPokerTables = new HashMap<String, TableInfo.Column>(9);
        _columnsPokerTables.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPokerTables.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPokerTables.put("chipValue", new TableInfo.Column("chipValue", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPokerTables.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPokerTables.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPokerTables.put("closedAt", new TableInfo.Column("closedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPokerTables.put("groupId", new TableInfo.Column("groupId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPokerTables.put("hasEntryFee", new TableInfo.Column("hasEntryFee", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPokerTables.put("entryFee", new TableInfo.Column("entryFee", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPokerTables = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPokerTables = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPokerTables = new TableInfo("poker_tables", _columnsPokerTables, _foreignKeysPokerTables, _indicesPokerTables);
        final TableInfo _existingPokerTables = TableInfo.read(db, "poker_tables");
        if (!_infoPokerTables.equals(_existingPokerTables)) {
          return new RoomOpenHelper.ValidationResult(false, "poker_tables(com.bankpoker.app.data.local.entity.PokerTable).\n"
                  + " Expected:\n" + _infoPokerTables + "\n"
                  + " Found:\n" + _existingPokerTables);
        }
        final HashMap<String, TableInfo.Column> _columnsPlayers = new HashMap<String, TableInfo.Column>(6);
        _columnsPlayers.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayers.put("tableId", new TableInfo.Column("tableId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayers.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayers.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayers.put("entryFeePaid", new TableInfo.Column("entryFeePaid", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlayers = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlayers = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlayers = new TableInfo("players", _columnsPlayers, _foreignKeysPlayers, _indicesPlayers);
        final TableInfo _existingPlayers = TableInfo.read(db, "players");
        if (!_infoPlayers.equals(_existingPlayers)) {
          return new RoomOpenHelper.ValidationResult(false, "players(com.bankpoker.app.data.local.entity.Player).\n"
                  + " Expected:\n" + _infoPlayers + "\n"
                  + " Found:\n" + _existingPlayers);
        }
        final HashMap<String, TableInfo.Column> _columnsBuyIns = new HashMap<String, TableInfo.Column>(6);
        _columnsBuyIns.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBuyIns.put("tableId", new TableInfo.Column("tableId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBuyIns.put("playerId", new TableInfo.Column("playerId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBuyIns.put("amount", new TableInfo.Column("amount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBuyIns.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBuyIns.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBuyIns = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBuyIns = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBuyIns = new TableInfo("buy_ins", _columnsBuyIns, _foreignKeysBuyIns, _indicesBuyIns);
        final TableInfo _existingBuyIns = TableInfo.read(db, "buy_ins");
        if (!_infoBuyIns.equals(_existingBuyIns)) {
          return new RoomOpenHelper.ValidationResult(false, "buy_ins(com.bankpoker.app.data.local.entity.BuyIn).\n"
                  + " Expected:\n" + _infoBuyIns + "\n"
                  + " Found:\n" + _existingBuyIns);
        }
        final HashMap<String, TableInfo.Column> _columnsExitRecords = new HashMap<String, TableInfo.Column>(6);
        _columnsExitRecords.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExitRecords.put("tableId", new TableInfo.Column("tableId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExitRecords.put("playerId", new TableInfo.Column("playerId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExitRecords.put("amount", new TableInfo.Column("amount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExitRecords.put("note", new TableInfo.Column("note", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsExitRecords.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysExitRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesExitRecords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoExitRecords = new TableInfo("exit_records", _columnsExitRecords, _foreignKeysExitRecords, _indicesExitRecords);
        final TableInfo _existingExitRecords = TableInfo.read(db, "exit_records");
        if (!_infoExitRecords.equals(_existingExitRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "exit_records(com.bankpoker.app.data.local.entity.ExitRecord).\n"
                  + " Expected:\n" + _infoExitRecords + "\n"
                  + " Found:\n" + _existingExitRecords);
        }
        final HashMap<String, TableInfo.Column> _columnsPlayerGroups = new HashMap<String, TableInfo.Column>(6);
        _columnsPlayerGroups.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayerGroups.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayerGroups.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayerGroups.put("mode", new TableInfo.Column("mode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayerGroups.put("serverId", new TableInfo.Column("serverId", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayerGroups.put("inviteCode", new TableInfo.Column("inviteCode", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPlayerGroups = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPlayerGroups = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPlayerGroups = new TableInfo("player_groups", _columnsPlayerGroups, _foreignKeysPlayerGroups, _indicesPlayerGroups);
        final TableInfo _existingPlayerGroups = TableInfo.read(db, "player_groups");
        if (!_infoPlayerGroups.equals(_existingPlayerGroups)) {
          return new RoomOpenHelper.ValidationResult(false, "player_groups(com.bankpoker.app.data.local.entity.PlayerGroup).\n"
                  + " Expected:\n" + _infoPlayerGroups + "\n"
                  + " Found:\n" + _existingPlayerGroups);
        }
        final HashMap<String, TableInfo.Column> _columnsGroupBalances = new HashMap<String, TableInfo.Column>(4);
        _columnsGroupBalances.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroupBalances.put("groupId", new TableInfo.Column("groupId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroupBalances.put("playerName", new TableInfo.Column("playerName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGroupBalances.put("balance", new TableInfo.Column("balance", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGroupBalances = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGroupBalances = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGroupBalances = new TableInfo("group_balances", _columnsGroupBalances, _foreignKeysGroupBalances, _indicesGroupBalances);
        final TableInfo _existingGroupBalances = TableInfo.read(db, "group_balances");
        if (!_infoGroupBalances.equals(_existingGroupBalances)) {
          return new RoomOpenHelper.ValidationResult(false, "group_balances(com.bankpoker.app.data.local.entity.GroupBalance).\n"
                  + " Expected:\n" + _infoGroupBalances + "\n"
                  + " Found:\n" + _existingGroupBalances);
        }
        final HashMap<String, TableInfo.Column> _columnsPayments = new HashMap<String, TableInfo.Column>(6);
        _columnsPayments.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("groupId", new TableInfo.Column("groupId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("fromPlayer", new TableInfo.Column("fromPlayer", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("toPlayer", new TableInfo.Column("toPlayer", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("amount", new TableInfo.Column("amount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPayments.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPayments = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPayments = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPayments = new TableInfo("payments", _columnsPayments, _foreignKeysPayments, _indicesPayments);
        final TableInfo _existingPayments = TableInfo.read(db, "payments");
        if (!_infoPayments.equals(_existingPayments)) {
          return new RoomOpenHelper.ValidationResult(false, "payments(com.bankpoker.app.data.local.entity.Payment).\n"
                  + " Expected:\n" + _infoPayments + "\n"
                  + " Found:\n" + _existingPayments);
        }
        final HashMap<String, TableInfo.Column> _columnsSettlements = new HashMap<String, TableInfo.Column>(10);
        _columnsSettlements.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("groupId", new TableInfo.Column("groupId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("tableId", new TableInfo.Column("tableId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("tableName", new TableInfo.Column("tableName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("payerName", new TableInfo.Column("payerName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("receiverName", new TableInfo.Column("receiverName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("amount", new TableInfo.Column("amount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("initialAmount", new TableInfo.Column("initialAmount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("paid", new TableInfo.Column("paid", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSettlements.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSettlements = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSettlements = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSettlements = new TableInfo("settlements", _columnsSettlements, _foreignKeysSettlements, _indicesSettlements);
        final TableInfo _existingSettlements = TableInfo.read(db, "settlements");
        if (!_infoSettlements.equals(_existingSettlements)) {
          return new RoomOpenHelper.ValidationResult(false, "settlements(com.bankpoker.app.data.local.entity.SettlementRecord).\n"
                  + " Expected:\n" + _infoSettlements + "\n"
                  + " Found:\n" + _existingSettlements);
        }
        final HashMap<String, TableInfo.Column> _columnsEntryFeeRecords = new HashMap<String, TableInfo.Column>(8);
        _columnsEntryFeeRecords.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEntryFeeRecords.put("groupId", new TableInfo.Column("groupId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEntryFeeRecords.put("tableId", new TableInfo.Column("tableId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEntryFeeRecords.put("tableName", new TableInfo.Column("tableName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEntryFeeRecords.put("playerName", new TableInfo.Column("playerName", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEntryFeeRecords.put("amount", new TableInfo.Column("amount", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEntryFeeRecords.put("paid", new TableInfo.Column("paid", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsEntryFeeRecords.put("timestamp", new TableInfo.Column("timestamp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysEntryFeeRecords = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesEntryFeeRecords = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoEntryFeeRecords = new TableInfo("entry_fee_records", _columnsEntryFeeRecords, _foreignKeysEntryFeeRecords, _indicesEntryFeeRecords);
        final TableInfo _existingEntryFeeRecords = TableInfo.read(db, "entry_fee_records");
        if (!_infoEntryFeeRecords.equals(_existingEntryFeeRecords)) {
          return new RoomOpenHelper.ValidationResult(false, "entry_fee_records(com.bankpoker.app.data.local.entity.EntryFeeRecord).\n"
                  + " Expected:\n" + _infoEntryFeeRecords + "\n"
                  + " Found:\n" + _existingEntryFeeRecords);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "f39ab3e697f609d6f77e7c24d0fb163b", "7185b6870528ca5d7c373de091e52a50");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "poker_tables","players","buy_ins","exit_records","player_groups","group_balances","payments","settlements","entry_fee_records");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `poker_tables`");
      _db.execSQL("DELETE FROM `players`");
      _db.execSQL("DELETE FROM `buy_ins`");
      _db.execSQL("DELETE FROM `exit_records`");
      _db.execSQL("DELETE FROM `player_groups`");
      _db.execSQL("DELETE FROM `group_balances`");
      _db.execSQL("DELETE FROM `payments`");
      _db.execSQL("DELETE FROM `settlements`");
      _db.execSQL("DELETE FROM `entry_fee_records`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(PokerTableDao.class, PokerTableDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PlayerDao.class, PlayerDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BuyInDao.class, BuyInDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ExitRecordDao.class, ExitRecordDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PlayerGroupDao.class, PlayerGroupDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GroupBalanceDao.class, GroupBalanceDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(PaymentDao.class, PaymentDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SettlementRecordDao.class, SettlementRecordDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(EntryFeeRecordDao.class, EntryFeeRecordDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public PokerTableDao pokerTableDao() {
    if (_pokerTableDao != null) {
      return _pokerTableDao;
    } else {
      synchronized(this) {
        if(_pokerTableDao == null) {
          _pokerTableDao = new PokerTableDao_Impl(this);
        }
        return _pokerTableDao;
      }
    }
  }

  @Override
  public PlayerDao playerDao() {
    if (_playerDao != null) {
      return _playerDao;
    } else {
      synchronized(this) {
        if(_playerDao == null) {
          _playerDao = new PlayerDao_Impl(this);
        }
        return _playerDao;
      }
    }
  }

  @Override
  public BuyInDao buyInDao() {
    if (_buyInDao != null) {
      return _buyInDao;
    } else {
      synchronized(this) {
        if(_buyInDao == null) {
          _buyInDao = new BuyInDao_Impl(this);
        }
        return _buyInDao;
      }
    }
  }

  @Override
  public ExitRecordDao exitRecordDao() {
    if (_exitRecordDao != null) {
      return _exitRecordDao;
    } else {
      synchronized(this) {
        if(_exitRecordDao == null) {
          _exitRecordDao = new ExitRecordDao_Impl(this);
        }
        return _exitRecordDao;
      }
    }
  }

  @Override
  public PlayerGroupDao playerGroupDao() {
    if (_playerGroupDao != null) {
      return _playerGroupDao;
    } else {
      synchronized(this) {
        if(_playerGroupDao == null) {
          _playerGroupDao = new PlayerGroupDao_Impl(this);
        }
        return _playerGroupDao;
      }
    }
  }

  @Override
  public GroupBalanceDao groupBalanceDao() {
    if (_groupBalanceDao != null) {
      return _groupBalanceDao;
    } else {
      synchronized(this) {
        if(_groupBalanceDao == null) {
          _groupBalanceDao = new GroupBalanceDao_Impl(this);
        }
        return _groupBalanceDao;
      }
    }
  }

  @Override
  public PaymentDao paymentDao() {
    if (_paymentDao != null) {
      return _paymentDao;
    } else {
      synchronized(this) {
        if(_paymentDao == null) {
          _paymentDao = new PaymentDao_Impl(this);
        }
        return _paymentDao;
      }
    }
  }

  @Override
  public SettlementRecordDao settlementRecordDao() {
    if (_settlementRecordDao != null) {
      return _settlementRecordDao;
    } else {
      synchronized(this) {
        if(_settlementRecordDao == null) {
          _settlementRecordDao = new SettlementRecordDao_Impl(this);
        }
        return _settlementRecordDao;
      }
    }
  }

  @Override
  public EntryFeeRecordDao entryFeeRecordDao() {
    if (_entryFeeRecordDao != null) {
      return _entryFeeRecordDao;
    } else {
      synchronized(this) {
        if(_entryFeeRecordDao == null) {
          _entryFeeRecordDao = new EntryFeeRecordDao_Impl(this);
        }
        return _entryFeeRecordDao;
      }
    }
  }
}
