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
import com.bankpoker.app.data.local.entity.EntryFeeHistoryInfo;
import com.bankpoker.app.data.local.entity.Player;
import com.bankpoker.app.data.local.entity.PlayerGameHistory;
import com.bankpoker.app.data.local.entity.UnpaidEntryFeeInfo;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class PlayerDao_Impl implements PlayerDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Player> __insertionAdapterOfPlayer;

  private final EntityInsertionAdapter<Player> __insertionAdapterOfPlayer_1;

  private final EntityDeletionOrUpdateAdapter<Player> __updateAdapterOfPlayer;

  private final SharedSQLiteStatement __preparedStmtOfUpdatePlayerStatus;

  private final SharedSQLiteStatement __preparedStmtOfDeletePlayersForTable;

  private final SharedSQLiteStatement __preparedStmtOfUpdateEntryFeePaid;

  private final SharedSQLiteStatement __preparedStmtOfSetAllPlayersExitedForTable;

  private final SharedSQLiteStatement __preparedStmtOfDeleteAllPlayers;

  public PlayerDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfPlayer = new EntityInsertionAdapter<Player>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `players` (`id`,`tableId`,`name`,`status`,`createdAt`,`entryFeePaid`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Player entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTableId());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getStatus());
        statement.bindLong(5, entity.getCreatedAt());
        final int _tmp = entity.getEntryFeePaid() ? 1 : 0;
        statement.bindLong(6, _tmp);
      }
    };
    this.__insertionAdapterOfPlayer_1 = new EntityInsertionAdapter<Player>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `players` (`id`,`tableId`,`name`,`status`,`createdAt`,`entryFeePaid`) VALUES (?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Player entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTableId());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getStatus());
        statement.bindLong(5, entity.getCreatedAt());
        final int _tmp = entity.getEntryFeePaid() ? 1 : 0;
        statement.bindLong(6, _tmp);
      }
    };
    this.__updateAdapterOfPlayer = new EntityDeletionOrUpdateAdapter<Player>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `players` SET `id` = ?,`tableId` = ?,`name` = ?,`status` = ?,`createdAt` = ?,`entryFeePaid` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Player entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTableId());
        statement.bindString(3, entity.getName());
        statement.bindString(4, entity.getStatus());
        statement.bindLong(5, entity.getCreatedAt());
        final int _tmp = entity.getEntryFeePaid() ? 1 : 0;
        statement.bindLong(6, _tmp);
        statement.bindString(7, entity.getId());
      }
    };
    this.__preparedStmtOfUpdatePlayerStatus = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE players SET status = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeletePlayersForTable = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM players WHERE tableId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateEntryFeePaid = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE players SET entryFeePaid = ? WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfSetAllPlayersExitedForTable = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE players SET status = 'EXITED' WHERE tableId = ?";
        return _query;
      }
    };
    this.__preparedStmtOfDeleteAllPlayers = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM players";
        return _query;
      }
    };
  }

  @Override
  public Object insertPlayer(final Player player, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPlayer.insert(player);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object insertPlayers(final List<Player> players,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfPlayer_1.insert(players);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePlayer(final Player player, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfPlayer.handle(player);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePlayerStatus(final String playerId, final String status,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdatePlayerStatus.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, status);
        _argIndex = 2;
        _stmt.bindString(_argIndex, playerId);
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
          __preparedStmtOfUpdatePlayerStatus.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePlayersForTable(final String tableId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeletePlayersForTable.acquire();
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
          __preparedStmtOfDeletePlayersForTable.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateEntryFeePaid(final String playerId, final boolean paid,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateEntryFeePaid.acquire();
        int _argIndex = 1;
        final int _tmp = paid ? 1 : 0;
        _stmt.bindLong(_argIndex, _tmp);
        _argIndex = 2;
        _stmt.bindString(_argIndex, playerId);
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
          __preparedStmtOfUpdateEntryFeePaid.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object setAllPlayersExitedForTable(final String tableId,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfSetAllPlayersExitedForTable.acquire();
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
          __preparedStmtOfSetAllPlayersExitedForTable.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAllPlayers(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAllPlayers.acquire();
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
          __preparedStmtOfDeleteAllPlayers.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Player>> getPlayersByTableId(final String tableId) {
    final String _sql = "SELECT * FROM players WHERE tableId = ? ORDER BY createdAt ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, tableId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"players"}, new Callable<List<Player>>() {
      @Override
      @NonNull
      public List<Player> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTableId = CursorUtil.getColumnIndexOrThrow(_cursor, "tableId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfEntryFeePaid = CursorUtil.getColumnIndexOrThrow(_cursor, "entryFeePaid");
          final List<Player> _result = new ArrayList<Player>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Player _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTableId;
            _tmpTableId = _cursor.getString(_cursorIndexOfTableId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpEntryFeePaid;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEntryFeePaid);
            _tmpEntryFeePaid = _tmp != 0;
            _item = new Player(_tmpId,_tmpTableId,_tmpName,_tmpStatus,_tmpCreatedAt,_tmpEntryFeePaid);
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
  public Object getPlayerById(final String playerId,
      final Continuation<? super Player> $completion) {
    final String _sql = "SELECT * FROM players WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, playerId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Player>() {
      @Override
      @Nullable
      public Player call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTableId = CursorUtil.getColumnIndexOrThrow(_cursor, "tableId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfEntryFeePaid = CursorUtil.getColumnIndexOrThrow(_cursor, "entryFeePaid");
          final Player _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTableId;
            _tmpTableId = _cursor.getString(_cursorIndexOfTableId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpEntryFeePaid;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEntryFeePaid);
            _tmpEntryFeePaid = _tmp != 0;
            _result = new Player(_tmpId,_tmpTableId,_tmpName,_tmpStatus,_tmpCreatedAt,_tmpEntryFeePaid);
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
  public Object getPlayingPlayersCount(final String tableId,
      final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM players WHERE tableId = ? AND status = 'PLAYING'";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, tableId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Object getAllPlayerNames(final Continuation<? super List<String>> $completion) {
    final String _sql = "SELECT name FROM players GROUP BY name ORDER BY COUNT(*) DESC, MAX(createdAt) DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
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
  public Object getAllPlayersOnce(final Continuation<? super List<Player>> $completion) {
    final String _sql = "SELECT * FROM players";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Player>>() {
      @Override
      @NonNull
      public List<Player> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTableId = CursorUtil.getColumnIndexOrThrow(_cursor, "tableId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfEntryFeePaid = CursorUtil.getColumnIndexOrThrow(_cursor, "entryFeePaid");
          final List<Player> _result = new ArrayList<Player>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Player _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTableId;
            _tmpTableId = _cursor.getString(_cursorIndexOfTableId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpEntryFeePaid;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEntryFeePaid);
            _tmpEntryFeePaid = _tmp != 0;
            _item = new Player(_tmpId,_tmpTableId,_tmpName,_tmpStatus,_tmpCreatedAt,_tmpEntryFeePaid);
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
  public Object getPlayersForTableOnce(final String tableId,
      final Continuation<? super List<Player>> $completion) {
    final String _sql = "SELECT * FROM players WHERE tableId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, tableId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Player>>() {
      @Override
      @NonNull
      public List<Player> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTableId = CursorUtil.getColumnIndexOrThrow(_cursor, "tableId");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfStatus = CursorUtil.getColumnIndexOrThrow(_cursor, "status");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfEntryFeePaid = CursorUtil.getColumnIndexOrThrow(_cursor, "entryFeePaid");
          final List<Player> _result = new ArrayList<Player>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Player _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTableId;
            _tmpTableId = _cursor.getString(_cursorIndexOfTableId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpStatus;
            _tmpStatus = _cursor.getString(_cursorIndexOfStatus);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final boolean _tmpEntryFeePaid;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEntryFeePaid);
            _tmpEntryFeePaid = _tmp != 0;
            _item = new Player(_tmpId,_tmpTableId,_tmpName,_tmpStatus,_tmpCreatedAt,_tmpEntryFeePaid);
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
  public Flow<List<UnpaidEntryFeeInfo>> getUnpaidEntryFeeDebtors() {
    final String _sql = "\n"
            + "        SELECT \n"
            + "            p.id AS playerId,\n"
            + "            p.name AS playerName,\n"
            + "            t.name AS tableName,\n"
            + "            g.name AS groupName,\n"
            + "            COALESCE(t.entryFee, 0) AS amount,\n"
            + "            p.createdAt AS timestamp\n"
            + "        FROM players p\n"
            + "        INNER JOIN poker_tables t ON p.tableId = t.id\n"
            + "        LEFT JOIN player_groups g ON t.groupId = g.id\n"
            + "        WHERE t.hasEntryFee = 1 \n"
            + "          AND p.entryFeePaid = 0 \n"
            + "          AND (p.status = 'EXITED' OR t.status = 'CLOSED')\n"
            + "        ORDER BY p.createdAt DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"players", "poker_tables",
        "player_groups"}, new Callable<List<UnpaidEntryFeeInfo>>() {
      @Override
      @NonNull
      public List<UnpaidEntryFeeInfo> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPlayerId = 0;
          final int _cursorIndexOfPlayerName = 1;
          final int _cursorIndexOfTableName = 2;
          final int _cursorIndexOfGroupName = 3;
          final int _cursorIndexOfAmount = 4;
          final int _cursorIndexOfTimestamp = 5;
          final List<UnpaidEntryFeeInfo> _result = new ArrayList<UnpaidEntryFeeInfo>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final UnpaidEntryFeeInfo _item;
            final String _tmpPlayerId;
            _tmpPlayerId = _cursor.getString(_cursorIndexOfPlayerId);
            final String _tmpPlayerName;
            _tmpPlayerName = _cursor.getString(_cursorIndexOfPlayerName);
            final String _tmpTableName;
            _tmpTableName = _cursor.getString(_cursorIndexOfTableName);
            final String _tmpGroupName;
            if (_cursor.isNull(_cursorIndexOfGroupName)) {
              _tmpGroupName = null;
            } else {
              _tmpGroupName = _cursor.getString(_cursorIndexOfGroupName);
            }
            final long _tmpAmount;
            _tmpAmount = _cursor.getLong(_cursorIndexOfAmount);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new UnpaidEntryFeeInfo(_tmpPlayerId,_tmpPlayerName,_tmpTableName,_tmpGroupName,_tmpAmount,_tmpTimestamp);
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
  public Flow<List<UnpaidEntryFeeInfo>> getUnpaidEntryFeeDebtorsByGroupId(final String groupId) {
    final String _sql = "\n"
            + "        SELECT \n"
            + "            p.id AS playerId,\n"
            + "            p.name AS playerName,\n"
            + "            t.name AS tableName,\n"
            + "            g.name AS groupName,\n"
            + "            COALESCE(t.entryFee, 0) AS amount,\n"
            + "            p.createdAt AS timestamp\n"
            + "        FROM players p\n"
            + "        INNER JOIN poker_tables t ON p.tableId = t.id\n"
            + "        LEFT JOIN player_groups g ON t.groupId = g.id\n"
            + "        WHERE t.groupId = ?\n"
            + "          AND t.hasEntryFee = 1 \n"
            + "          AND p.entryFeePaid = 0 \n"
            + "          AND (p.status = 'EXITED' OR t.status = 'CLOSED')\n"
            + "        ORDER BY p.createdAt DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, groupId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"players", "poker_tables",
        "player_groups"}, new Callable<List<UnpaidEntryFeeInfo>>() {
      @Override
      @NonNull
      public List<UnpaidEntryFeeInfo> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPlayerId = 0;
          final int _cursorIndexOfPlayerName = 1;
          final int _cursorIndexOfTableName = 2;
          final int _cursorIndexOfGroupName = 3;
          final int _cursorIndexOfAmount = 4;
          final int _cursorIndexOfTimestamp = 5;
          final List<UnpaidEntryFeeInfo> _result = new ArrayList<UnpaidEntryFeeInfo>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final UnpaidEntryFeeInfo _item;
            final String _tmpPlayerId;
            _tmpPlayerId = _cursor.getString(_cursorIndexOfPlayerId);
            final String _tmpPlayerName;
            _tmpPlayerName = _cursor.getString(_cursorIndexOfPlayerName);
            final String _tmpTableName;
            _tmpTableName = _cursor.getString(_cursorIndexOfTableName);
            final String _tmpGroupName;
            if (_cursor.isNull(_cursorIndexOfGroupName)) {
              _tmpGroupName = null;
            } else {
              _tmpGroupName = _cursor.getString(_cursorIndexOfGroupName);
            }
            final long _tmpAmount;
            _tmpAmount = _cursor.getLong(_cursorIndexOfAmount);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            _item = new UnpaidEntryFeeInfo(_tmpPlayerId,_tmpPlayerName,_tmpTableName,_tmpGroupName,_tmpAmount,_tmpTimestamp);
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
  public Flow<List<EntryFeeHistoryInfo>> getEntryFeeHistoryByGroupId(final String groupId) {
    final String _sql = "\n"
            + "        SELECT \n"
            + "            p.id AS playerId,\n"
            + "            p.name AS playerName,\n"
            + "            t.name AS tableName,\n"
            + "            g.name AS groupName,\n"
            + "            COALESCE(t.entryFee, 0) AS amount,\n"
            + "            p.createdAt AS timestamp,\n"
            + "            p.entryFeePaid AS isPaid\n"
            + "        FROM players p\n"
            + "        INNER JOIN poker_tables t ON p.tableId = t.id\n"
            + "        LEFT JOIN player_groups g ON t.groupId = g.id\n"
            + "        WHERE t.groupId = ?\n"
            + "          AND t.hasEntryFee = 1\n"
            + "        ORDER BY p.createdAt DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, groupId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"players", "poker_tables",
        "player_groups"}, new Callable<List<EntryFeeHistoryInfo>>() {
      @Override
      @NonNull
      public List<EntryFeeHistoryInfo> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPlayerId = 0;
          final int _cursorIndexOfPlayerName = 1;
          final int _cursorIndexOfTableName = 2;
          final int _cursorIndexOfGroupName = 3;
          final int _cursorIndexOfAmount = 4;
          final int _cursorIndexOfTimestamp = 5;
          final int _cursorIndexOfIsPaid = 6;
          final List<EntryFeeHistoryInfo> _result = new ArrayList<EntryFeeHistoryInfo>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EntryFeeHistoryInfo _item;
            final String _tmpPlayerId;
            _tmpPlayerId = _cursor.getString(_cursorIndexOfPlayerId);
            final String _tmpPlayerName;
            _tmpPlayerName = _cursor.getString(_cursorIndexOfPlayerName);
            final String _tmpTableName;
            _tmpTableName = _cursor.getString(_cursorIndexOfTableName);
            final String _tmpGroupName;
            if (_cursor.isNull(_cursorIndexOfGroupName)) {
              _tmpGroupName = null;
            } else {
              _tmpGroupName = _cursor.getString(_cursorIndexOfGroupName);
            }
            final long _tmpAmount;
            _tmpAmount = _cursor.getLong(_cursorIndexOfAmount);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsPaid;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPaid);
            _tmpIsPaid = _tmp != 0;
            _item = new EntryFeeHistoryInfo(_tmpPlayerId,_tmpPlayerName,_tmpTableName,_tmpGroupName,_tmpAmount,_tmpTimestamp,_tmpIsPaid);
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
  public Flow<List<EntryFeeHistoryInfo>> getAllEntryFeeHistory() {
    final String _sql = "\n"
            + "        SELECT \n"
            + "            p.id AS playerId,\n"
            + "            p.name AS playerName,\n"
            + "            t.name AS tableName,\n"
            + "            g.name AS groupName,\n"
            + "            COALESCE(t.entryFee, 0) AS amount,\n"
            + "            p.createdAt AS timestamp,\n"
            + "            p.entryFeePaid AS isPaid\n"
            + "        FROM players p\n"
            + "        INNER JOIN poker_tables t ON p.tableId = t.id\n"
            + "        LEFT JOIN player_groups g ON t.groupId = g.id\n"
            + "        WHERE t.hasEntryFee = 1\n"
            + "        ORDER BY p.createdAt DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"players", "poker_tables",
        "player_groups"}, new Callable<List<EntryFeeHistoryInfo>>() {
      @Override
      @NonNull
      public List<EntryFeeHistoryInfo> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPlayerId = 0;
          final int _cursorIndexOfPlayerName = 1;
          final int _cursorIndexOfTableName = 2;
          final int _cursorIndexOfGroupName = 3;
          final int _cursorIndexOfAmount = 4;
          final int _cursorIndexOfTimestamp = 5;
          final int _cursorIndexOfIsPaid = 6;
          final List<EntryFeeHistoryInfo> _result = new ArrayList<EntryFeeHistoryInfo>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final EntryFeeHistoryInfo _item;
            final String _tmpPlayerId;
            _tmpPlayerId = _cursor.getString(_cursorIndexOfPlayerId);
            final String _tmpPlayerName;
            _tmpPlayerName = _cursor.getString(_cursorIndexOfPlayerName);
            final String _tmpTableName;
            _tmpTableName = _cursor.getString(_cursorIndexOfTableName);
            final String _tmpGroupName;
            if (_cursor.isNull(_cursorIndexOfGroupName)) {
              _tmpGroupName = null;
            } else {
              _tmpGroupName = _cursor.getString(_cursorIndexOfGroupName);
            }
            final long _tmpAmount;
            _tmpAmount = _cursor.getLong(_cursorIndexOfAmount);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpIsPaid;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPaid);
            _tmpIsPaid = _tmp != 0;
            _item = new EntryFeeHistoryInfo(_tmpPlayerId,_tmpPlayerName,_tmpTableName,_tmpGroupName,_tmpAmount,_tmpTimestamp,_tmpIsPaid);
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
  public Flow<List<PlayerGameHistory>> getPlayerGamesByName(final String name) {
    final String _sql = "\n"
            + "        SELECT \n"
            + "            p.id AS playerId,\n"
            + "            p.tableId AS tableId,\n"
            + "            t.name AS tableName,\n"
            + "            g.name AS groupName,\n"
            + "            p.createdAt AS date,\n"
            + "            COALESCE((SELECT SUM(b.amount) FROM buy_ins b WHERE b.playerId = p.id), 0) AS totalBuyIn,\n"
            + "            COALESCE((SELECT SUM(e.amount) FROM exit_records e WHERE e.playerId = p.id), 0) AS totalExit,\n"
            + "            (COALESCE((SELECT SUM(e.amount) FROM exit_records e WHERE e.playerId = p.id), 0) - \n"
            + "             COALESCE((SELECT SUM(b.amount) FROM buy_ins b WHERE b.playerId = p.id), 0)) AS netResult,\n"
            + "            p.entryFeePaid AS entryFeePaid\n"
            + "        FROM players p\n"
            + "        INNER JOIN poker_tables t ON p.tableId = t.id\n"
            + "        LEFT JOIN player_groups g ON t.groupId = g.id\n"
            + "        WHERE TRIM(LOWER(p.name)) = TRIM(LOWER(?))\n"
            + "        ORDER BY p.createdAt DESC\n"
            + "    ";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, name);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"buy_ins", "exit_records",
        "players", "poker_tables", "player_groups"}, new Callable<List<PlayerGameHistory>>() {
      @Override
      @NonNull
      public List<PlayerGameHistory> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPlayerId = 0;
          final int _cursorIndexOfTableId = 1;
          final int _cursorIndexOfTableName = 2;
          final int _cursorIndexOfGroupName = 3;
          final int _cursorIndexOfDate = 4;
          final int _cursorIndexOfTotalBuyIn = 5;
          final int _cursorIndexOfTotalExit = 6;
          final int _cursorIndexOfNetResult = 7;
          final int _cursorIndexOfEntryFeePaid = 8;
          final List<PlayerGameHistory> _result = new ArrayList<PlayerGameHistory>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PlayerGameHistory _item;
            final String _tmpPlayerId;
            _tmpPlayerId = _cursor.getString(_cursorIndexOfPlayerId);
            final String _tmpTableId;
            _tmpTableId = _cursor.getString(_cursorIndexOfTableId);
            final String _tmpTableName;
            _tmpTableName = _cursor.getString(_cursorIndexOfTableName);
            final String _tmpGroupName;
            if (_cursor.isNull(_cursorIndexOfGroupName)) {
              _tmpGroupName = null;
            } else {
              _tmpGroupName = _cursor.getString(_cursorIndexOfGroupName);
            }
            final long _tmpDate;
            _tmpDate = _cursor.getLong(_cursorIndexOfDate);
            final long _tmpTotalBuyIn;
            _tmpTotalBuyIn = _cursor.getLong(_cursorIndexOfTotalBuyIn);
            final long _tmpTotalExit;
            _tmpTotalExit = _cursor.getLong(_cursorIndexOfTotalExit);
            final long _tmpNetResult;
            _tmpNetResult = _cursor.getLong(_cursorIndexOfNetResult);
            final boolean _tmpEntryFeePaid;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEntryFeePaid);
            _tmpEntryFeePaid = _tmp != 0;
            _item = new PlayerGameHistory(_tmpPlayerId,_tmpTableId,_tmpTableName,_tmpGroupName,_tmpDate,_tmpTotalBuyIn,_tmpTotalExit,_tmpNetResult,_tmpEntryFeePaid);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
