package com.bankpoker.app.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.bankpoker.app.data.local.entity.PokerTable;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PokerTableDao_Impl implements PokerTableDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<PokerTable> __insertionAdapterOfPokerTable;

  private final EntityInsertionAdapter<PokerTable> __insertionAdapterOfPokerTable_1;

  private final EntityDeletionOrUpdateAdapter<PokerTable> __updateAdapterOfPokerTable;

  private final SharedSQLiteStatement __preparedStmtOfCloseTable;

  private final SharedSQLiteStatement __preparedStmtOfDeleteTable;

  private final SharedSQLiteStatement __preparedStmtOfDeleteTablesByGroupId;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllTables;

  public PokerTableDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPokerTable = new EntityInsertionAdapter<PokerTable>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `poker_tables` (`id`,`name`,`chipValue`,`status`,`createdAt`,`closedAt`,`groupId`,`hasEntryFee`,`entryFee`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PokerTable entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getChipValue() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getChipValue());
        }
        statement.bindString(4, entity.getStatus());
        statement.bindLong(5, entity.getCreatedAt());
        if (entity.getClosedAt() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getClosedAt());
        }
        if (entity.getGroupId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getGroupId());
        }
        final int _tmp = entity.getHasEntryFee() ? 1 : 0;
        statement.bindLong(8, _tmp);
        if (entity.getEntryFee() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getEntryFee());
        }
      }
    };
    this.__insertionAdapterOfPokerTable_1 = new EntityInsertionAdapter<PokerTable>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `poker_tables` (`id`,`name`,`chipValue`,`status`,`createdAt`,`closedAt`,`groupId`,`hasEntryFee`,`entryFee`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PokerTable entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getChipValue() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getChipValue());
        }
        statement.bindString(4, entity.getStatus());
        statement.bindLong(5, entity.getCreatedAt());
        if (entity.getClosedAt() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getClosedAt());
        }
        if (entity.getGroupId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getGroupId());
        }
        final int _tmp = entity.getHasEntryFee() ? 1 : 0;
        statement.bindLong(8, _tmp);
        if (entity.getEntryFee() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getEntryFee());
        }
      }
    };
    this.__updateAdapterOfPokerTable = new EntityDeletionOrUpdateAdapter<PokerTable>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `poker_tables` SET `id` = ?,`name` = ?,`chipValue` = ?,`status` = ?,`createdAt` = ?,`closedAt` = ?,`groupId` = ?,`hasEntryFee` = ?,`entryFee` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PokerTable entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getName());
        if (entity.getChipValue() == null) {
          statement.bindNull(3);
        } else {
          statement.bindLong(3, entity.getChipValue());
        }
        statement.bindString(4, entity.getStatus());
        statement.bindLong(5, entity.getCreatedAt());
        if (entity.getClosedAt() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getClosedAt());
        }
        if (entity.getGroupId() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getGroupId());
        }
        final int _tmp = entity.getHasEntryFee() ? 1 : 0;
        statement.bindLong(8, _tmp);
        if (entity.getEntryFee() == null) {
          statement.bindNull(9);
        } else {
          statement.bindLong(9, entity.getEntryFee());
        }
        statement.bindString(10, entity.getId());
      }
    };
    this.__preparedStmtOfCloseTable = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE poker_tables SET status = ?, closedAt = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteTable = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM poker_tables WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteTablesByGroupId = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM poker_tables WHERE groupId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllTables = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM poker_tables";
        return _query;
      }
    };
  }

  @Override
  public Object insertTable(final PokerTable table, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPokerTable.insert(table);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertTables(final List<PokerTable> tables,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPokerTable_1.insert(tables);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateTable(final PokerTable table, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfPokerTable.handle(table);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object closeTable(final String tableId, final String status, final long closedAt,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfCloseTable.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        _stmt.bindLong(_argIndex, closedAt);
        _argIndex = 3;
        _stmt.bindString(_argIndex, tableId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfCloseTable.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTable(final String tableId, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteTable.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, tableId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteTable.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteTablesByGroupId(final String groupId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteTablesByGroupId.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, groupId);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteTablesByGroupId.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllTables(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllTables.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAllTables.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PokerTable>> getQuickTables() {
    final String _sql = "SELECT * FROM poker_tables WHERE groupId IS NULL ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"poker_tables"}, new Callable<List<PokerTable>>() {
      @Override
      @NonNull
      public List<PokerTable> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfChipValue = CursorUtil.getColumnIndexOrThrow(_cursor, "chipValue");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closedAt");
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfHasEntryFee = CursorUtil.getColumnIndexOrThrow(_cursor, "hasEntryFee");
          final int _cursorIndexOfEntryFee = CursorUtil.getColumnIndexOrThrow(_cursor, "entryFee");
          final List<PokerTable> _result = new ArrayList<PokerTable>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PokerTable _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final Long _tmpChipValue;
            if (_cursor.isNull(_cursorIndexOfChipValue)) {
              _tmpChipValue = null;
            } else {
              _tmpChipValue = _cursor.getLong(_cursorIndexOfChipValue);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getLong(_cursorIndexOfClosedAt);
            }
            final String _tmpGroupId;
            if (_cursor.isNull(_cursorIndexOfGroupId)) {
              _tmpGroupId = null;
            } else {
              _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            }
            final boolean _tmpHasEntryFee;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasEntryFee);
            _tmpHasEntryFee = _tmp != 0;
            final Long _tmpEntryFee;
            if (_cursor.isNull(_cursorIndexOfEntryFee)) {
              _tmpEntryFee = null;
            } else {
              _tmpEntryFee = _cursor.getLong(_cursorIndexOfEntryFee);
            }
            _item = new PokerTable(_tmpId,_tmpName,_tmpChipValue,_tmpStatus,_tmpCreatedAt,_tmpClosedAt,_tmpGroupId,_tmpHasEntryFee,_tmpEntryFee);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<PokerTable>> getAllTables() {
    final String _sql = "SELECT * FROM poker_tables ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"poker_tables"}, new Callable<List<PokerTable>>() {
      @Override
      @NonNull
      public List<PokerTable> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfChipValue = CursorUtil.getColumnIndexOrThrow(_cursor, "chipValue");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closedAt");
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfHasEntryFee = CursorUtil.getColumnIndexOrThrow(_cursor, "hasEntryFee");
          final int _cursorIndexOfEntryFee = CursorUtil.getColumnIndexOrThrow(_cursor, "entryFee");
          final List<PokerTable> _result = new ArrayList<PokerTable>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PokerTable _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final Long _tmpChipValue;
            if (_cursor.isNull(_cursorIndexOfChipValue)) {
              _tmpChipValue = null;
            } else {
              _tmpChipValue = _cursor.getLong(_cursorIndexOfChipValue);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getLong(_cursorIndexOfClosedAt);
            }
            final String _tmpGroupId;
            if (_cursor.isNull(_cursorIndexOfGroupId)) {
              _tmpGroupId = null;
            } else {
              _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            }
            final boolean _tmpHasEntryFee;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasEntryFee);
            _tmpHasEntryFee = _tmp != 0;
            final Long _tmpEntryFee;
            if (_cursor.isNull(_cursorIndexOfEntryFee)) {
              _tmpEntryFee = null;
            } else {
              _tmpEntryFee = _cursor.getLong(_cursorIndexOfEntryFee);
            }
            _item = new PokerTable(_tmpId,_tmpName,_tmpChipValue,_tmpStatus,_tmpCreatedAt,_tmpClosedAt,_tmpGroupId,_tmpHasEntryFee,_tmpEntryFee);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getTableById(final String tableId,
      final Continuation<? super PokerTable> $completion) {
    final String _sql = "SELECT * FROM poker_tables WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, tableId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PokerTable>() {
      @Override
      @Nullable
      public PokerTable call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfChipValue = CursorUtil.getColumnIndexOrThrow(_cursor, "chipValue");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closedAt");
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfHasEntryFee = CursorUtil.getColumnIndexOrThrow(_cursor, "hasEntryFee");
          final int _cursorIndexOfEntryFee = CursorUtil.getColumnIndexOrThrow(_cursor, "entryFee");
          final PokerTable _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final Long _tmpChipValue;
            if (_cursor.isNull(_cursorIndexOfChipValue)) {
              _tmpChipValue = null;
            } else {
              _tmpChipValue = _cursor.getLong(_cursorIndexOfChipValue);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getLong(_cursorIndexOfClosedAt);
            }
            final String _tmpGroupId;
            if (_cursor.isNull(_cursorIndexOfGroupId)) {
              _tmpGroupId = null;
            } else {
              _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            }
            final boolean _tmpHasEntryFee;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasEntryFee);
            _tmpHasEntryFee = _tmp != 0;
            final Long _tmpEntryFee;
            if (_cursor.isNull(_cursorIndexOfEntryFee)) {
              _tmpEntryFee = null;
            } else {
              _tmpEntryFee = _cursor.getLong(_cursorIndexOfEntryFee);
            }
            _result = new PokerTable(_tmpId,_tmpName,_tmpChipValue,_tmpStatus,_tmpCreatedAt,_tmpClosedAt,_tmpGroupId,_tmpHasEntryFee,_tmpEntryFee);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllTablesOnce(final Continuation<? super List<PokerTable>> $completion) {
    final String _sql = "SELECT * FROM poker_tables ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PokerTable>>() {
      @Override
      @NonNull
      public List<PokerTable> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfChipValue = CursorUtil.getColumnIndexOrThrow(_cursor, "chipValue");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closedAt");
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfHasEntryFee = CursorUtil.getColumnIndexOrThrow(_cursor, "hasEntryFee");
          final int _cursorIndexOfEntryFee = CursorUtil.getColumnIndexOrThrow(_cursor, "entryFee");
          final List<PokerTable> _result = new ArrayList<PokerTable>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PokerTable _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final Long _tmpChipValue;
            if (_cursor.isNull(_cursorIndexOfChipValue)) {
              _tmpChipValue = null;
            } else {
              _tmpChipValue = _cursor.getLong(_cursorIndexOfChipValue);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getLong(_cursorIndexOfClosedAt);
            }
            final String _tmpGroupId;
            if (_cursor.isNull(_cursorIndexOfGroupId)) {
              _tmpGroupId = null;
            } else {
              _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            }
            final boolean _tmpHasEntryFee;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasEntryFee);
            _tmpHasEntryFee = _tmp != 0;
            final Long _tmpEntryFee;
            if (_cursor.isNull(_cursorIndexOfEntryFee)) {
              _tmpEntryFee = null;
            } else {
              _tmpEntryFee = _cursor.getLong(_cursorIndexOfEntryFee);
            }
            _item = new PokerTable(_tmpId,_tmpName,_tmpChipValue,_tmpStatus,_tmpCreatedAt,_tmpClosedAt,_tmpGroupId,_tmpHasEntryFee,_tmpEntryFee);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<PokerTable>> getTablesByGroupId(final String groupId) {
    final String _sql = "SELECT * FROM poker_tables WHERE groupId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, groupId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"poker_tables"}, new Callable<List<PokerTable>>() {
      @Override
      @NonNull
      public List<PokerTable> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfChipValue = CursorUtil.getColumnIndexOrThrow(_cursor, "chipValue");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closedAt");
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfHasEntryFee = CursorUtil.getColumnIndexOrThrow(_cursor, "hasEntryFee");
          final int _cursorIndexOfEntryFee = CursorUtil.getColumnIndexOrThrow(_cursor, "entryFee");
          final List<PokerTable> _result = new ArrayList<PokerTable>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PokerTable _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final Long _tmpChipValue;
            if (_cursor.isNull(_cursorIndexOfChipValue)) {
              _tmpChipValue = null;
            } else {
              _tmpChipValue = _cursor.getLong(_cursorIndexOfChipValue);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getLong(_cursorIndexOfClosedAt);
            }
            final String _tmpGroupId;
            if (_cursor.isNull(_cursorIndexOfGroupId)) {
              _tmpGroupId = null;
            } else {
              _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            }
            final boolean _tmpHasEntryFee;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasEntryFee);
            _tmpHasEntryFee = _tmp != 0;
            final Long _tmpEntryFee;
            if (_cursor.isNull(_cursorIndexOfEntryFee)) {
              _tmpEntryFee = null;
            } else {
              _tmpEntryFee = _cursor.getLong(_cursorIndexOfEntryFee);
            }
            _item = new PokerTable(_tmpId,_tmpName,_tmpChipValue,_tmpStatus,_tmpCreatedAt,_tmpClosedAt,_tmpGroupId,_tmpHasEntryFee,_tmpEntryFee);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getTablesByGroupIdOnce(final String groupId,
      final Continuation<? super List<PokerTable>> $completion) {
    final String _sql = "SELECT * FROM poker_tables WHERE groupId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, groupId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PokerTable>>() {
      @Override
      @NonNull
      public List<PokerTable> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfChipValue = CursorUtil.getColumnIndexOrThrow(_cursor, "chipValue");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfClosedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "closedAt");
          final int _cursorIndexOfGroupId = CursorUtil.getColumnIndexOrThrow(_cursor, "groupId");
          final int _cursorIndexOfHasEntryFee = CursorUtil.getColumnIndexOrThrow(_cursor, "hasEntryFee");
          final int _cursorIndexOfEntryFee = CursorUtil.getColumnIndexOrThrow(_cursor, "entryFee");
          final List<PokerTable> _result = new ArrayList<PokerTable>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PokerTable _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final Long _tmpChipValue;
            if (_cursor.isNull(_cursorIndexOfChipValue)) {
              _tmpChipValue = null;
            } else {
              _tmpChipValue = _cursor.getLong(_cursorIndexOfChipValue);
            }
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final Long _tmpClosedAt;
            if (_cursor.isNull(_cursorIndexOfClosedAt)) {
              _tmpClosedAt = null;
            } else {
              _tmpClosedAt = _cursor.getLong(_cursorIndexOfClosedAt);
            }
            final String _tmpGroupId;
            if (_cursor.isNull(_cursorIndexOfGroupId)) {
              _tmpGroupId = null;
            } else {
              _tmpGroupId = _cursor.getString(_cursorIndexOfGroupId);
            }
            final boolean _tmpHasEntryFee;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfHasEntryFee);
            _tmpHasEntryFee = _tmp != 0;
            final Long _tmpEntryFee;
            if (_cursor.isNull(_cursorIndexOfEntryFee)) {
              _tmpEntryFee = null;
            } else {
              _tmpEntryFee = _cursor.getLong(_cursorIndexOfEntryFee);
            }
            _item = new PokerTable(_tmpId,_tmpName,_tmpChipValue,_tmpStatus,_tmpCreatedAt,_tmpClosedAt,_tmpGroupId,_tmpHasEntryFee,_tmpEntryFee);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
