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

  private final EntityDeletionOrUpdateAdapter<PokerTable> __updateAdapterOfPokerTable;

  private final SharedSQLiteStatement __preparedStmtOfCloseTable;

  public PokerTableDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPokerTable = new EntityInsertionAdapter<PokerTable>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `poker_tables` (`id`,`name`,`chipValue`,`status`,`createdAt`,`closedAt`) VALUES (?,?,?,?,?,?)";
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
      }
    };
    this.__updateAdapterOfPokerTable = new EntityDeletionOrUpdateAdapter<PokerTable>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `poker_tables` SET `id` = ?,`name` = ?,`chipValue` = ?,`status` = ?,`createdAt` = ?,`closedAt` = ? WHERE `id` = ?";
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
        statement.bindString(7, entity.getId());
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
            _item = new PokerTable(_tmpId,_tmpName,_tmpChipValue,_tmpStatus,_tmpCreatedAt,_tmpClosedAt);
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
            _result = new PokerTable(_tmpId,_tmpName,_tmpChipValue,_tmpStatus,_tmpCreatedAt,_tmpClosedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
