const sqlite3 = require('sqlite3').verbose();
const fs = require('fs');
const path = require('path');

// Database file path: server/bankpoker.db
const dbPath = path.resolve(__dirname, '../../bankpoker.db');

// SQLite connection
const db = new sqlite3.Database(dbPath, (err) => {
    if (err) {
        console.error('Failed to connect to SQLite database:', err.message);
    } else {
        console.log(`Connected to SQLite database at ${dbPath}`);
    }
});

// Promisified helper methods
const run = (sql, params = []) => {
    return new Promise((resolve, reject) => {
        db.run(sql, params, function (err) {
            if (err) return reject(err);
            resolve({ lastID: this.lastID, changes: this.changes });
        });
    });
};

const get = (sql, params = []) => {
    return new Promise((resolve, reject) => {
        db.get(sql, params, (err, row) => {
            if (err) return reject(err);
            resolve(row);
        });
    });
};

const all = (sql, params = []) => {
    return new Promise((resolve, reject) => {
        db.all(sql, params, (err, rows) => {
            if (err) return reject(err);
            resolve(rows);
        });
    });
};

const exec = (sql) => {
    return new Promise((resolve, reject) => {
        db.exec(sql, (err) => {
            if (err) return reject(err);
            resolve();
        });
    });
};

// Initialize database schema
const initDb = async () => {
    try {
        await run('PRAGMA foreign_keys = ON;');
        const schemaPath = path.join(__dirname, 'schema.sql');
        const schemaSql = fs.readFileSync(schemaPath, 'utf8');
        await exec(schemaSql);

        // Safe column migrations for existing databases
        const groupsColumns = await all("PRAGMA table_info(groups)");
        const columnNames = groupsColumns.map(c => c.name);
        if (!columnNames.includes('created_by')) {
            await run("ALTER TABLE groups ADD COLUMN created_by TEXT");
        }
        if (!columnNames.includes('created_at')) {
            await run("ALTER TABLE groups ADD COLUMN created_at INTEGER");
        }
        if (!columnNames.includes('mode')) {
            await run("ALTER TABLE groups ADD COLUMN mode TEXT DEFAULT 'OFFLINE'");
        }

        const joinReqColumns = await all("PRAGMA table_info(join_requests)");
        const joinReqColNames = joinReqColumns.map(c => c.name);
        if (!joinReqColNames.includes('table_id')) {
            await run("ALTER TABLE join_requests ADD COLUMN table_id TEXT");
        }

        const playersColumns = await all("PRAGMA table_info(players)");
        const playerColNames = playersColumns.map(c => c.name);
        if (!playerColNames.includes('user_id')) {
            await run("ALTER TABLE players ADD COLUMN user_id TEXT");
        }
        if (!playerColNames.includes('entry_fee_paid')) {
            await run("ALTER TABLE players ADD COLUMN entry_fee_paid INTEGER DEFAULT 0");
        }

        const tablesColumns = await all("PRAGMA table_info(tables)");
        const tableColNames = tablesColumns.map(c => c.name);
        if (!tableColNames.includes('is_active')) {
            await run("ALTER TABLE tables ADD COLUMN is_active INTEGER DEFAULT 1");
        }

        await run(`
            CREATE TABLE IF NOT EXISTS synced_balances (
                id TEXT PRIMARY KEY,
                group_id TEXT NOT NULL,
                user_id TEXT,
                username TEXT NOT NULL,
                balance INTEGER NOT NULL,
                updated_at INTEGER NOT NULL,
                FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE
            )
        `);
        await run(`CREATE INDEX IF NOT EXISTS idx_synced_balances_group ON synced_balances(group_id)`);

        console.log('Database schema initialized successfully');
    } catch (error) {
        console.error('Error initializing database schema:', error);
        throw error;
    }
};

module.exports = {
    db,
    run,
    get,
    all,
    exec,
    initDb
};
