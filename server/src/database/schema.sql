-- Enable Foreign Key Support
PRAGMA foreign_keys = ON;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role TEXT DEFAULT 'PLAYER',
    created_at INTEGER NOT NULL
);

-- 2. Groups Table
CREATE TABLE IF NOT EXISTS groups (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    invite_code TEXT UNIQUE,
    server_id TEXT,
    updated_at INTEGER NOT NULL,
    is_synced INTEGER DEFAULT 0,
    is_deleted INTEGER DEFAULT 0
);

-- 3. Group Members Table
CREATE TABLE IF NOT EXISTS group_members (
    user_id TEXT NOT NULL,
    group_id TEXT NOT NULL,
    joined_at INTEGER NOT NULL,
    PRIMARY KEY(user_id, group_id),
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE CASCADE
);

-- 4. Poker Tables Table
CREATE TABLE IF NOT EXISTS tables (
    id TEXT PRIMARY KEY,
    group_id TEXT,
    name TEXT NOT NULL,
    chip_value INTEGER,
    status TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    closed_at INTEGER,
    has_entry_fee INTEGER DEFAULT 0,
    entry_fee INTEGER,
    server_id TEXT,
    updated_at INTEGER NOT NULL,
    is_synced INTEGER DEFAULT 0,
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE SET NULL
);

-- 5. Players Table
CREATE TABLE IF NOT EXISTS players (
    id TEXT PRIMARY KEY,
    table_id TEXT NOT NULL,
    name TEXT NOT NULL,
    status TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    entry_fee_paid INTEGER DEFAULT 0,
    server_id TEXT,
    updated_at INTEGER NOT NULL,
    is_synced INTEGER DEFAULT 0,
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY(table_id) REFERENCES tables(id) ON DELETE CASCADE
);

-- 6. BuyIns Table
CREATE TABLE IF NOT EXISTS buy_ins (
    id TEXT PRIMARY KEY,
    table_id TEXT NOT NULL,
    player_id TEXT NOT NULL,
    amount INTEGER NOT NULL,
    note TEXT,
    created_at INTEGER NOT NULL,
    server_id TEXT,
    updated_at INTEGER NOT NULL,
    is_synced INTEGER DEFAULT 0,
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY(table_id) REFERENCES tables(id) ON DELETE CASCADE,
    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE
);

-- 7. Exits (Exit Records) Table
CREATE TABLE IF NOT EXISTS exit_records (
    id TEXT PRIMARY KEY,
    table_id TEXT NOT NULL,
    player_id TEXT NOT NULL,
    amount INTEGER NOT NULL,
    note TEXT,
    created_at INTEGER NOT NULL,
    server_id TEXT,
    updated_at INTEGER NOT NULL,
    is_synced INTEGER DEFAULT 0,
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY(table_id) REFERENCES tables(id) ON DELETE CASCADE,
    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE
);

-- 8. Payments Table
CREATE TABLE IF NOT EXISTS payments (
    id TEXT PRIMARY KEY,
    group_id TEXT NOT NULL,
    from_player TEXT NOT NULL,
    to_player TEXT NOT NULL,
    amount INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    server_id TEXT,
    updated_at INTEGER NOT NULL,
    is_synced INTEGER DEFAULT 0,
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE CASCADE
);

-- 9. Settlement Records Table
CREATE TABLE IF NOT EXISTS settlement_records (
    id TEXT PRIMARY KEY,
    group_id TEXT NOT NULL,
    table_id TEXT NOT NULL,
    table_name TEXT NOT NULL,
    payer_name TEXT NOT NULL,
    receiver_name TEXT NOT NULL,
    amount INTEGER NOT NULL,
    initial_amount INTEGER NOT NULL,
    paid INTEGER DEFAULT 0,
    timestamp INTEGER NOT NULL,
    server_id TEXT,
    updated_at INTEGER NOT NULL,
    is_synced INTEGER DEFAULT 0,
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE CASCADE
);

-- 10. Entry Fee Records Table
CREATE TABLE IF NOT EXISTS entry_fee_records (
    id TEXT PRIMARY KEY,
    group_id TEXT NOT NULL,
    table_id TEXT NOT NULL,
    table_name TEXT NOT NULL,
    player_name TEXT NOT NULL,
    amount INTEGER NOT NULL,
    paid INTEGER DEFAULT 0,
    timestamp INTEGER NOT NULL,
    server_id TEXT,
    updated_at INTEGER NOT NULL,
    is_synced INTEGER DEFAULT 0,
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_group_members_user ON group_members(user_id);
CREATE INDEX IF NOT EXISTS idx_group_members_group ON group_members(group_id);
CREATE INDEX IF NOT EXISTS idx_tables_group ON tables(group_id);
CREATE INDEX IF NOT EXISTS idx_players_table ON players(table_id);
CREATE INDEX IF NOT EXISTS idx_buy_ins_table ON buy_ins(table_id);
CREATE INDEX IF NOT EXISTS idx_buy_ins_player ON buy_ins(player_id);
CREATE INDEX IF NOT EXISTS idx_exit_records_table ON exit_records(table_id);
CREATE INDEX IF NOT EXISTS idx_exit_records_player ON exit_records(player_id);
CREATE INDEX IF NOT EXISTS idx_payments_group ON payments(group_id);
CREATE INDEX IF NOT EXISTS idx_settlement_records_group ON settlement_records(group_id);
CREATE INDEX IF NOT EXISTS idx_entry_fee_records_group ON entry_fee_records(group_id);
