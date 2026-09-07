const sqlite3 = require('sqlite3').verbose();
const fs = require('fs');
const path = require('path');
const dotenv = require('dotenv');

// Ensure environment variables are loaded if db.js is loaded standalone
if (!process.env.DATABASE_PATH) {
    dotenv.config({ path: path.resolve(__dirname, '../../.env') });
}

// Database file path: configurable via DATABASE_PATH, defaults to server/bankpoker.db
const defaultDbPath = path.resolve(__dirname, '../../bankpoker.db');
const dbPath = process.env.DATABASE_PATH ? path.resolve(process.env.DATABASE_PATH) : defaultDbPath;

// Ensure database directory exists before connecting
const dbDir = path.dirname(dbPath);
if (!fs.existsSync(dbDir)) {
    fs.mkdirSync(dbDir, { recursive: true });
}

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
        const usersColumns = await all("PRAGMA table_info(users)");
        const userColNames = usersColumns.map(c => c.name);
        if (!userColNames.includes('display_name')) {
            await run("ALTER TABLE users ADD COLUMN display_name TEXT DEFAULT ''");
        }
        if (!userColNames.includes('avatar_id')) {
            await run("ALTER TABLE users ADD COLUMN avatar_id TEXT DEFAULT 'avatar_1'");
        }
        if (!userColNames.includes('updated_at')) {
            await run("ALTER TABLE users ADD COLUMN updated_at INTEGER DEFAULT 0");
        }

        const groupsColumns = await all("PRAGMA table_info(groups)");
        const groupColNames = groupsColumns.map(c => c.name);
        if (!groupColNames.includes('owner_user_id')) {
            await run("ALTER TABLE groups ADD COLUMN owner_user_id TEXT");
        }

        const tablesColumns = await all("PRAGMA table_info(tables)");
        const tableColNames = tablesColumns.map(c => c.name);
        if (!tableColNames.includes('creator_user_id')) {
            await run("ALTER TABLE tables ADD COLUMN creator_user_id TEXT");
        }
        if (!tableColNames.includes('code')) {
            await run("ALTER TABLE tables ADD COLUMN code TEXT");
        }
        if (!tableColNames.includes('published_at')) {
            await run("ALTER TABLE tables ADD COLUMN published_at INTEGER");
        }

        const playersColumns = await all("PRAGMA table_info(players)");
        const playerColNames = playersColumns.map(c => c.name);
        if (!playerColNames.includes('user_linked_at')) {
            await run("ALTER TABLE players ADD COLUMN user_linked_at INTEGER");
        }

        // Auto-generate VAPID keys into system_settings if not already present
        const existingPubKey = await get("SELECT value FROM system_settings WHERE key = 'vapid_public_key'");
        if (!existingPubKey) {
            try {
                const webpush = require('web-push');
                const vapidKeys = webpush.generateVAPIDKeys();
                await run("INSERT OR REPLACE INTO system_settings (key, value) VALUES ('vapid_public_key', ?)", [vapidKeys.publicKey]);
                await run("INSERT OR REPLACE INTO system_settings (key, value) VALUES ('vapid_private_key', ?)", [vapidKeys.privateKey]);
                console.log('Generated fresh VAPID keys for Web Push');
            } catch (vapidErr) {
                console.warn('Failed to generate VAPID keys:', vapidErr.message);
            }
        }

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
