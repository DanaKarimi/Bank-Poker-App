const express = require('express');
const router = express.Router();
const crypto = require('crypto');
const { run, all } = require('../database/db');
const { authenticateToken, requireAdmin } = require('../middleware/auth');

// List of syncable tables in dependency order
const SYNC_TABLES = [
    'groups',
    'tables',
    'players',
    'buy_ins',
    'exit_records',
    'payments',
    'settlement_records',
    'entry_fee_records'
];

// Helper to normalize client object (supports camelCase & snake_case)
const normalizeRecord = (table, raw) => {
    const now = Date.now();
    const id = raw.id || raw.server_id || raw.serverId || crypto.randomUUID();
    const server_id = raw.server_id || raw.serverId || id;
    const updated_at = Number(raw.updated_at || raw.updatedAt || now);
    const is_synced = 1;
    const is_deleted = (raw.is_deleted === 1 || raw.isDeleted === true || raw.is_deleted === true) ? 1 : 0;

    switch (table) {
        case 'groups':
            return {
                id,
                name: raw.name || 'Unnamed Group',
                invite_code: raw.invite_code || raw.inviteCode || null,
                mode: raw.mode || 'OFFLINE',
                created_by: raw.created_by || raw.createdBy || null,
                created_at: Number(raw.created_at || raw.createdAt || now),
                server_id,
                updated_at,
                is_synced,
                is_deleted
            };

        case 'tables':
        case 'poker_tables':
            return {
                id,
                group_id: raw.group_id || raw.groupId || null,
                name: raw.name || 'Unnamed Table',
                chip_value: raw.chip_value != null ? Number(raw.chip_value) : (raw.chipValue != null ? Number(raw.chipValue) : null),
                status: raw.status || 'ACTIVE',
                created_at: Number(raw.created_at || raw.createdAt || now),
                closed_at: raw.closed_at != null ? Number(raw.closed_at) : (raw.closedAt != null ? Number(raw.closedAt) : null),
                has_entry_fee: (raw.has_entry_fee === 1 || raw.hasEntryFee === true || raw.has_entry_fee === true) ? 1 : 0,
                entry_fee: raw.entry_fee != null ? Number(raw.entry_fee) : (raw.entryFee != null ? Number(raw.entryFee) : null),
                server_id,
                updated_at,
                is_synced,
                is_deleted
            };

        case 'players':
            return {
                id,
                table_id: raw.table_id || raw.tableId || '',
                name: raw.name || '',
                status: raw.status || 'ACTIVE',
                created_at: Number(raw.created_at || raw.createdAt || now),
                entry_fee_paid: (raw.entry_fee_paid === 1 || raw.entryFeePaid === true || raw.entry_fee_paid === true) ? 1 : 0,
                server_id,
                updated_at,
                is_synced,
                is_deleted
            };

        case 'buy_ins':
        case 'buyins':
            return {
                id,
                table_id: raw.table_id || raw.tableId || '',
                player_id: raw.player_id || raw.playerId || '',
                amount: Number(raw.amount || 0),
                note: raw.note || null,
                created_at: Number(raw.created_at || raw.createdAt || now),
                server_id,
                updated_at,
                is_synced,
                is_deleted
            };

        case 'exit_records':
        case 'exits':
            return {
                id,
                table_id: raw.table_id || raw.tableId || '',
                player_id: raw.player_id || raw.playerId || '',
                amount: Number(raw.amount || 0),
                note: raw.note || null,
                created_at: Number(raw.created_at || raw.createdAt || now),
                server_id,
                updated_at,
                is_synced,
                is_deleted
            };

        case 'payments':
            return {
                id,
                group_id: raw.group_id || raw.groupId || '',
                from_player: raw.from_player || raw.fromPlayer || '',
                to_player: raw.to_player || raw.toPlayer || '',
                amount: Number(raw.amount || 0),
                created_at: Number(raw.created_at || raw.createdAt || now),
                server_id,
                updated_at,
                is_synced,
                is_deleted
            };

        case 'settlement_records':
        case 'settlements':
            return {
                id,
                group_id: raw.group_id || raw.groupId || '',
                table_id: raw.table_id || raw.tableId || '',
                table_name: raw.table_name || raw.tableName || '',
                payer_name: raw.payer_name || raw.payerName || '',
                receiver_name: raw.receiver_name || raw.receiverName || '',
                amount: Number(raw.amount || 0),
                initial_amount: Number(raw.initial_amount || raw.initialAmount || raw.amount || 0),
                paid: (raw.paid === 1 || raw.paid === true) ? 1 : 0,
                timestamp: Number(raw.timestamp || now),
                server_id,
                updated_at,
                is_synced,
                is_deleted
            };

        case 'entry_fee_records':
        case 'entry_fees':
            return {
                id,
                group_id: raw.group_id || raw.groupId || '',
                table_id: raw.table_id || raw.tableId || '',
                table_name: raw.table_name || raw.tableName || '',
                player_name: raw.player_name || raw.playerName || '',
                amount: Number(raw.amount || 0),
                paid: (raw.paid === 1 || raw.paid === true) ? 1 : 0,
                timestamp: Number(raw.timestamp || now),
                server_id,
                updated_at,
                is_synced,
                is_deleted
            };

        default:
            return null;
    }
};

/**
 * Upsert a normalized record into SQLite
 */
const upsertRecord = async (targetTable, record) => {
    const keys = Object.keys(record);
    const placeholders = keys.map(() => '?').join(', ');
    const values = Object.values(record);
    const updateClause = keys
        .filter(k => k !== 'id')
        .map(k => `${k} = excluded.${k}`)
        .join(', ');

    const sql = `
        INSERT INTO ${targetTable} (${keys.join(', ')})
        VALUES (${placeholders})
        ON CONFLICT(id) DO UPDATE SET ${updateClause}
    `;

    await run(sql, values);
};

/**
 * POST /api/sync/push
 * (Requires Auth + role='ADMIN')
 * Accept an array or map of records from the client and upsert them into SQLite
 */
router.post('/push', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const body = req.body;
        let syncedCount = 0;

        // 1. If body is an array of items: [{ table: 'tables', ...data }, ...]
        if (Array.isArray(body)) {
            for (const item of body) {
                const targetTable = item._table || item.table;
                if (targetTable) {
                    const normalized = normalizeRecord(targetTable, item.data || item);
                    if (normalized) {
                        const dbTable = (targetTable === 'poker_tables') ? 'tables' :
                                        (targetTable === 'exits') ? 'exit_records' :
                                        (targetTable === 'buyins') ? 'buy_ins' :
                                        (targetTable === 'settlements') ? 'settlement_records' :
                                        (targetTable === 'entry_fees') ? 'entry_fee_records' : targetTable;
                        await upsertRecord(dbTable, normalized);
                        syncedCount++;
                    }
                }
            }
        } else if (typeof body === 'object' && body !== null) {
            // 2. If body is a map of tables: { tables: [...], players: [...], ... }
            for (const [tableKey, records] of Object.entries(body)) {
                if (Array.isArray(records)) {
                    const dbTable = (tableKey === 'poker_tables') ? 'tables' :
                                    (tableKey === 'exits') ? 'exit_records' :
                                    (tableKey === 'buyins') ? 'buy_ins' :
                                    (tableKey === 'settlements') ? 'settlement_records' :
                                    (tableKey === 'entry_fees') ? 'entry_fee_records' : tableKey;

                    for (const rawRecord of records) {
                        const normalized = normalizeRecord(tableKey, rawRecord);
                        if (normalized) {
                            await upsertRecord(dbTable, normalized);
                            syncedCount++;
                        }
                    }
                }
            }
        }

        return res.status(200).json({
            success: true,
            syncedCount,
            timestamp: Date.now()
        });
    } catch (error) {
        console.error('Error during sync push:', error);
        return res.status(500).json({ error: 'Internal server error during sync push' });
    }
});

/**
 * GET /api/sync/pull
 * (Requires Auth + role='ADMIN')
 * Query param: since (timestamp in ms). Returns all records updated after `since`.
 */
router.get('/pull', authenticateToken, requireAdmin, async (req, res) => {
    try {
        const since = Number(req.query.since) || 0;
        const resultData = {};

        for (const tableName of SYNC_TABLES) {
            const records = await all(
                `SELECT * FROM ${tableName} WHERE updated_at > ? ORDER BY updated_at ASC`,
                [since]
            );
            resultData[tableName] = records;
        }

        return res.status(200).json({
            timestamp: Date.now(),
            since,
            data: resultData
        });
    } catch (error) {
        console.error('Error during sync pull:', error);
        return res.status(500).json({ error: 'Internal server error during sync pull' });
    }
});

module.exports = router;
