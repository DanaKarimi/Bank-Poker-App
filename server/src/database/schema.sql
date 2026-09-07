-- Enable Foreign Key Support and WAL Mode
PRAGMA foreign_keys = ON;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    id TEXT PRIMARY KEY,
    username TEXT UNIQUE NOT NULL,
    display_name TEXT NOT NULL,
    password_hash TEXT, -- NULL for guest accounts
    avatar_id TEXT DEFAULT 'avatar_1',
    role TEXT DEFAULT 'USER', -- 'USER' or 'SUPER_ADMIN'
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);

-- 2. Groups Table
CREATE TABLE IF NOT EXISTS groups (
    id TEXT PRIMARY KEY,
    owner_user_id TEXT,
    created_by TEXT, -- Alias for backward compatibility
    name TEXT NOT NULL,
    invite_code TEXT UNIQUE,
    mode TEXT DEFAULT 'ONLINE',
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    server_id TEXT,
    is_synced INTEGER DEFAULT 1,
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY(owner_user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY(created_by) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_groups_invite_code ON groups(invite_code);
CREATE INDEX IF NOT EXISTS idx_groups_owner ON groups(owner_user_id);

-- 3. Group Members Table
CREATE TABLE IF NOT EXISTS group_members (
    user_id TEXT NOT NULL,
    group_id TEXT NOT NULL,
    role TEXT DEFAULT 'MEMBER', -- 'ADMIN' or 'MEMBER'
    joined_at INTEGER NOT NULL,
    PRIMARY KEY(user_id, group_id),
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_group_members_user ON group_members(user_id);
CREATE INDEX IF NOT EXISTS idx_group_members_group ON group_members(group_id);

-- 4. Tables Table (group_id NULL means Quick Table)
CREATE TABLE IF NOT EXISTS tables (
    id TEXT PRIMARY KEY,
    group_id TEXT,
    creator_user_id TEXT,
    name TEXT NOT NULL,
    code TEXT UNIQUE, -- 6-char alphanumeric code; NULL until first published
    status TEXT DEFAULT 'ACTIVE', -- 'ACTIVE' or 'CLOSED'
    chip_value INTEGER,
    has_entry_fee INTEGER DEFAULT 0,
    entry_fee INTEGER,
    created_at INTEGER NOT NULL,
    closed_at INTEGER,
    published_at INTEGER,
    server_id TEXT,
    updated_at INTEGER NOT NULL,
    is_synced INTEGER DEFAULT 1,
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE CASCADE,
    FOREIGN KEY(creator_user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_tables_group ON tables(group_id);
CREATE INDEX IF NOT EXISTS idx_tables_code ON tables(code);
CREATE INDEX IF NOT EXISTS idx_tables_creator ON tables(creator_user_id);

-- 5. Players Table (One row per table session per player identity)
CREATE TABLE IF NOT EXISTS players (
    id TEXT PRIMARY KEY,
    table_id TEXT NOT NULL,
    user_id TEXT, -- NULL if unclaimed or offline member
    name TEXT NOT NULL,
    status TEXT DEFAULT 'ACTIVE', -- 'ACTIVE' or 'EXITED'
    user_linked_at INTEGER, -- Timestamp when identity was claimed/linked
    entry_fee_paid INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL,
    server_id TEXT,
    updated_at INTEGER NOT NULL,
    is_synced INTEGER DEFAULT 1,
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY(table_id) REFERENCES tables(id) ON DELETE CASCADE,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_players_table ON players(table_id);
CREATE INDEX IF NOT EXISTS idx_players_user ON players(user_id);
CREATE INDEX IF NOT EXISTS idx_players_name ON players(name);

-- 6. Buy-Ins Table
CREATE TABLE IF NOT EXISTS buy_ins (
    id TEXT PRIMARY KEY,
    table_id TEXT NOT NULL,
    player_id TEXT NOT NULL,
    amount INTEGER NOT NULL,
    note TEXT,
    actor_user_id TEXT,
    created_at INTEGER NOT NULL,
    server_id TEXT,
    updated_at INTEGER NOT NULL,
    is_synced INTEGER DEFAULT 1,
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY(table_id) REFERENCES tables(id) ON DELETE CASCADE,
    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE,
    FOREIGN KEY(actor_user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_buy_ins_table ON buy_ins(table_id);
CREATE INDEX IF NOT EXISTS idx_buy_ins_player ON buy_ins(player_id);

-- 7. Exit Records Table
CREATE TABLE IF NOT EXISTS exit_records (
    id TEXT PRIMARY KEY,
    table_id TEXT NOT NULL,
    player_id TEXT NOT NULL,
    amount INTEGER NOT NULL,
    note TEXT,
    actor_user_id TEXT,
    created_at INTEGER NOT NULL,
    server_id TEXT,
    updated_at INTEGER NOT NULL,
    is_synced INTEGER DEFAULT 1,
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY(table_id) REFERENCES tables(id) ON DELETE CASCADE,
    FOREIGN KEY(player_id) REFERENCES players(id) ON DELETE CASCADE,
    FOREIGN KEY(actor_user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_exit_records_table ON exit_records(table_id);
CREATE INDEX IF NOT EXISTS idx_exit_records_player ON exit_records(player_id);

-- 8. Payments Table
CREATE TABLE IF NOT EXISTS payments (
    id TEXT PRIMARY KEY,
    group_id TEXT NOT NULL,
    from_player TEXT NOT NULL,
    to_player TEXT NOT NULL,
    amount INTEGER NOT NULL,
    actor_user_id TEXT,
    created_at INTEGER NOT NULL,
    server_id TEXT,
    updated_at INTEGER NOT NULL,
    is_synced INTEGER DEFAULT 1,
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE CASCADE,
    FOREIGN KEY(actor_user_id) REFERENCES users(id) ON DELETE SET NULL
);
CREATE INDEX IF NOT EXISTS idx_payments_group ON payments(group_id);

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
    is_synced INTEGER DEFAULT 1,
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_settlement_records_group ON settlement_records(group_id);

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
    is_synced INTEGER DEFAULT 1,
    is_deleted INTEGER DEFAULT 0,
    FOREIGN KEY(group_id) REFERENCES groups(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_entry_fee_records_group ON entry_fee_records(group_id);

-- 11. Unified Requests Table (Buy-In & Exit requests)
CREATE TABLE IF NOT EXISTS requests (
    id TEXT PRIMARY KEY,
    table_id TEXT NOT NULL,
    user_id TEXT NOT NULL,
    type TEXT NOT NULL CHECK(type IN ('BUY_IN', 'EXIT')),
    amount INTEGER NOT NULL,
    status TEXT DEFAULT 'PENDING' CHECK(status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY(table_id) REFERENCES tables(id) ON DELETE CASCADE,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_requests_table ON requests(table_id);
CREATE INDEX IF NOT EXISTS idx_requests_user ON requests(user_id);
CREATE INDEX IF NOT EXISTS idx_requests_status ON requests(status);

-- 12. Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    type TEXT NOT NULL,
    payload TEXT NOT NULL, -- JSON string
    count INTEGER DEFAULT 1,
    read INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_read ON notifications(read);
CREATE INDEX IF NOT EXISTS idx_notifications_created ON notifications(created_at);

-- 13. User Settings Table
CREATE TABLE IF NOT EXISTS user_settings (
    user_id TEXT PRIMARY KEY,
    notif_prefs TEXT NOT NULL, -- JSON string of notification preferences
    updated_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 14. System Settings Table (VAPID keys, system configs)
CREATE TABLE IF NOT EXISTS system_settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

-- 15. FCM Push Tokens
CREATE TABLE IF NOT EXISTS fcm_tokens (
    user_id TEXT NOT NULL,
    token TEXT NOT NULL,
    platform TEXT DEFAULT 'android',
    created_at INTEGER NOT NULL,
    PRIMARY KEY(user_id, token),
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_fcm_tokens_user ON fcm_tokens(user_id);

-- 16. Web Push Subscriptions
CREATE TABLE IF NOT EXISTS push_subscriptions (
    user_id TEXT NOT NULL,
    endpoint TEXT PRIMARY KEY,
    keys TEXT NOT NULL, -- JSON string of { p256dh, auth }
    created_at INTEGER NOT NULL,
    FOREIGN KEY(user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_push_subscriptions_user ON push_subscriptions(user_id);

-- 17. Synced Balances Table (Snapshot cache)
CREATE TABLE IF NOT EXISTS synced_balances (
    id TEXT PRIMARY KEY,
    group_id TEXT NOT NULL,
    user_id TEXT,
    username TEXT NOT NULL,
    balance INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    FOREIGN KEY (group_id) REFERENCES groups(id) ON DELETE CASCADE
);
CREATE INDEX IF NOT EXISTS idx_synced_balances_group ON synced_balances(group_id);
