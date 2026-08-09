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
import com.bankpoker.app.data.local.dao.ExitRecordDao;
import com.bankpoker.app.data.local.dao.ExitRecordDao_Impl;
import com.bankpoker.app.data.local.dao.PlayerDao;
import com.bankpoker.app.data.local.dao.PlayerDao_Impl;
import com.bankpoker.app.data.local.dao.PokerTableDao;
import com.bankpoker.app.data.local.dao.PokerTableDao_Impl;
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

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `poker_tables` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `chipValue` INTEGER, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `closedAt` INTEGER, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `players` (`id` TEXT NOT NULL, `tableId` TEXT NOT NULL, `name` TEXT NOT NULL, `status` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `buy_ins` (`id` TEXT NOT NULL, `tableId` TEXT NOT NULL, `playerId` TEXT NOT NULL, `amount` INTEGER NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `exit_records` (`id` TEXT NOT NULL, `tableId` TEXT NOT NULL, `playerId` TEXT NOT NULL, `amount` INTEGER NOT NULL, `note` TEXT, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '4dc3711b92032d7c8dc778c4f443cd9c')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `poker_tables`");
        db.execSQL("DROP TABLE IF EXISTS `players`");
        db.execSQL("DROP TABLE IF EXISTS `buy_ins`");
        db.execSQL("DROP TABLE IF EXISTS `exit_records`");
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
        final HashMap<String, TableInfo.Column> _columnsPokerTables = new HashMap<String, TableInfo.Column>(6);
        _columnsPokerTables.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPokerTables.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPokerTables.put("chipValue", new TableInfo.Column("chipValue", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPokerTables.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPokerTables.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPokerTables.put("closedAt", new TableInfo.Column("closedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPokerTables = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPokerTables = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPokerTables = new TableInfo("poker_tables", _columnsPokerTables, _foreignKeysPokerTables, _indicesPokerTables);
        final TableInfo _existingPokerTables = TableInfo.read(db, "poker_tables");
        if (!_infoPokerTables.equals(_existingPokerTables)) {
          return new RoomOpenHelper.ValidationResult(false, "poker_tables(com.bankpoker.app.data.local.entity.PokerTable).\n"
                  + " Expected:\n" + _infoPokerTables + "\n"
                  + " Found:\n" + _existingPokerTables);
        }
        final HashMap<String, TableInfo.Column> _columnsPlayers = new HashMap<String, TableInfo.Column>(5);
        _columnsPlayers.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayers.put("tableId", new TableInfo.Column("tableId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayers.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayers.put("status", new TableInfo.Column("status", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPlayers.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
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
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "4dc3711b92032d7c8dc778c4f443cd9c", "03bee232bdfb65c777d26fa8314a5cd1");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "poker_tables","players","buy_ins","exit_records");
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
}
