# BankPoker Production Deployment Guide (New Architecture)

This document provides complete instructions for deploying the new BankPoker architecture to the production environment on the Ubuntu PC server, as well as building the Web PWA and Android client artifacts.

---

## 1. Production Architecture Overview

The production deployment runs on an **Ubuntu PC** host with the following topology:

```
[Clients: Android App / Web Browser]
                  │
                  ▼ (HTTPS)
      Cloudflare Tunnel (Edge)
                  │
                  ▼ (HTTP :80)
       Nginx (Reverse Proxy)
         ├── /       ──> Web Frontend Static Assets (web/dist)
         └── /api/   ──> Express Backend (http://server:3000)
```

### Production Invariants (DO NOT BREAK)
- **Public Domain**: `https://bankjoker.ir`
- **Ports & Proxies**: Nginx listens on port 80 internally; Express backend runs on internal port 3000.
- **Cloudflare Tunnel**: Manages external SSL termination.
- **Health Check**: `GET /api/health` must return `{"status": "ok", "timestamp": ...}`.
- **Legacy Web Support**: Web bundle must preserve legacy browser polyfills (`renderModernChunks: false`).
- **Database**: SQLite database stored in persistent Docker volume (`/data/bankpoker.db`).

---

## 2. Server Deployment (Ubuntu PC)

### Step 2.1: Pull Latest Code
SSH into the production server and navigate to the project directory:

```bash
cd /opt/BankPoker   # or your local repo path
git fetch origin
git checkout features
git pull origin features
```

### Step 2.2: Verify Environment Variables
Ensure the production `server/.env` file contains:

```bash
PORT=3000
NODE_ENV=production
DATABASE_PATH=/data/bankpoker.db
JWT_SECRET=your_production_jwt_secret_key_here
SUPERADMIN_USERNAMES=admin,danakarimi
```

> **Note on Migrations**:
> Database schema updates and migrations are **fully automated and idempotent**.
> When the Node.js server starts, `server/src/database/db.js` automatically checks table structures, creates new tables (`notifications`, `user_settings`, `fcm_tokens`, `push_subscriptions`), adds missing columns (`code`, `avatar_id`, `display_name`, `user_linked_at`), and initializes indexes. No manual SQL scripts are required.

### Step 2.3: Rebuild and Restart the Docker Container

```bash
docker compose build server
docker compose up -d server
```

Or if restarting the full stack:
```bash
docker compose down
docker compose up -d --build
```

### Step 2.4: Verify Server Health
Check that Express is running and accessible via Nginx:

```bash
curl -i http://localhost/api/health
```
Expected response:
```json
HTTP/1.1 200 OK
Content-Type: application/json; charset=utf-8

{"status":"ok","timestamp":1788500000000}
```

Also test via the public domain:
```bash
curl -i https://bankjoker.ir/api/health
```

---

## 3. Web Frontend Build & Deployment

The Web frontend is built using Vite with `@vitejs/plugin-legacy` to ensure compatibility with older mobile browsers, WebViews, and desktop clients.

### Step 3.1: Build Production Web Bundle

On the build machine or production server:

```bash
cd web
npm install
npm run build
```

The production assets will be generated in `web/dist/`:
- `dist/index.html` (contains PWA meta tags and manifest link)
- `dist/manifest.webmanifest` (PWA web manifest)
- `dist/sw.js` (Service Worker for offline app shell caching)
- `dist/assets/polyfills-legacy-*.js` (Polyfills for older Safari, iOS, and Android WebViews)
- `dist/assets/index-legacy-*.js` (Main application bundle)

### Step 3.2: Verify Nginx Serving
Ensure Nginx is pointing its root directive to `web/dist`.
Restart Nginx if configuration was adjusted:

```bash
docker compose restart nginx
```

Test PWA files:
```bash
curl -I https://bankjoker.ir/manifest.webmanifest
curl -I https://bankjoker.ir/sw.js
```

---

## 4. Android App Build & Deployment

The Android app is built with Jetpack Compose, Room (v7 schema), and Retrofit.

### Step 4.1: Server URL Configuration
The Android app defaults to:
`https://bankjoker.ir`

Users can also switch between server presets or configure a custom URL directly within the app under:
**Settings (Gear icon) -> Server Connection -> Presets**.

### Step 4.2: Build Debug or Release APK
On your development machine:

```powershell
# Stop existing daemons to avoid Windows file locks:
.\gradlew.bat --stop

# Compile and package Debug APK:
.\gradlew.bat assembleDebug --no-daemon

# Or package signed Release APK:
.\gradlew.bat assembleRelease --no-daemon
```

APK output locations:
- **Debug**: `app/build/outputs/apk/debug/app-debug.apk`
- **Release**: `app/build/outputs/apk/release/app-release.apk`

### Step 4.3: Install on Device
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## 5. End-to-End Verification Checklist

After deploying to production, perform the following verification steps:

| Step | Action | Expected Result |
|---|---|---|
| 1 | `curl https://bankjoker.ir/api/health` | HTTP 200 OK with `{"status":"ok"}` |
| 2 | Open `https://bankjoker.ir` in browser | Web app loads with felt-green poker theme and PWA install prompt |
| 3 | Register first user or login with admin account | User receives `SUPER_ADMIN` role automatically |
| 4 | Open `https://bankjoker.ir/admin` | Super Admin dashboard renders with overview stats, users, groups, and tables |
| 5 | Create Quick Table | Table is created without a group; shows `⚡ Quick Table` badge |
| 6 | Publish Table | Generates 6-character group code chip; searchable via Smart Lookup |
| 7 | Delete Player Constraint | Try deleting a player with buy-ins > 0: blocked with exact warning message |
| 8 | Notification Bell | Notification dropdown opens with tabs (All / Unread) and Mark All Read |
| 9 | Android App | Launches, connects to `https://bankjoker.ir`, shows tables, syncs outbox queue |

---

## 6. Troubleshooting & Recovery

### Issue: SQLite Database Locked
- WAL mode is enabled by default in `server/src/database/db.js` (`PRAGMA journal_mode = WAL;`).
- Ensure no multiple container instances are mounting the same database file without WAL.

### Issue: 401 Unauthorized on Admin Routes
- Admin routes (`/api/admin/*`) require `role = 'SUPER_ADMIN'`.
- Use the first registered account, or set `SUPERADMIN_USERNAMES=your_user` in `server/.env` and restart the server.

### Issue: Legacy Browser White Screen
- The build uses `renderModernChunks: false` in `web/vite.config.js`.
- Ensure you run `npm run build` and that Nginx is serving files directly from `web/dist`.
