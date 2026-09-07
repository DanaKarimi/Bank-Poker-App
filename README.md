# 🃏 BankPoker

**BankPoker** is a full-stack, self-hosted poker bank and ledger management system designed for home-game hosts and players. It combines a native **Android Admin App** (Kotlin + Jetpack Compose + Room), a modern **Player Web App** (React + Vite + Tailwind CSS), and a lightweight **Node.js REST API Server** (Express + SQLite).

BankPoker manages poker tables, seated players, live buy-ins, cash-out exits, entry fees ("Voroodi"), cumulative group balances, and automated debt settlement calculations.

---

## 🌟 Key Features

### 1. Dual Operating Modes: Offline & Online
- **Offline Mode (Local Room DB):** Manage standalone "Quick Tables" or local player groups on Android with zero internet connectivity required.
- **Online Mode (Server Sync):** Host online poker groups synchronized in real-time with the Node.js server, allowing players to join via Web and request buy-ins and exits.
- **Seamless Offline-to-Online Conversion:** Convert any existing offline group to an online server group with a single tap. All tables, player histories, balances, and settlements are bulk-uploaded and a unique 6-character Invite Code is generated.

### 2. Player Identity Claim System
- When players join a converted group via the Web App using an invite code, they are prompted to either **claim their existing pre-converted player name** (re-linking past history and stats) or **join as a new player**.
- Native online groups seamlessly bind players to their authenticated account.

### 3. Comprehensive Table & Chip Management
- **Table Controls:** Active vs. Closed states, optional chip values, and table notes.
- **Real-Time Buy-In & Exit Flow:**
  - **Player Requests (Web):** Players can submit buy-in and exit requests with amounts and notes.
  - **Admin Approval (Android):** Host receives notifications and approves/rejects requests.
  - **Direct Admin Actions:** Host can directly add buy-ins or process cash-outs at the table.
  - **Player Confirmation:** Two-way handshake confirming chip handoffs.
- **Live Balances:** Instant calculations of chips in play, total buy-ins, total exits, and net player balance.

### 4. Entry Fee (Voroodi) Tracking
- Optional table entry fee configuration (`entryFee > 0`).
- **Admin Control (Android):** Host marks entry fee as Paid/Unpaid for each seated player.
- **Web App Badges:** Web tables display a dynamic green badge (`Entry Fee Paid ✓`) or red badge (`UNPAID`) based on the authenticated player's payment status.

### 5. Group Balances & Statistical Insights
- **Cumulative Ledger:** Automatically rolls up net player performance when group tables are closed.
- **Player Stats:** Total buy-ins, total exits, net balance, and payment adjustments.
- **Leaderboards:** Identifies 🏆 **Biggest Winner** and 💸 **Biggest Debtor**.
- **Personal Player View:** Players on Web see their own personalized stats cards at the top of the group overview.

### 6. Automated Settlement Plan
- **Greedy Two-Pointer Algorithm:** Computes the minimal number of peer-to-peer transactions required to settle all debts in the group.
- **Payment Badges:** Visual `PAID` (green) and `PENDING` (gold) badges for every debt row.
- **Admin Settlement Action:** Host marks payments as settled, instantly syncing payment records to the backend.

### 7. Audit History & Edits
- Chronological transaction log of all buy-ins, cash-outs, and settlements.
- Full edit and deletion support for hosts to correct bookkeeping errors.

### 8. Authentication & Role-Based Access
- Secure **JWT Authentication** with password hashing (`bcryptjs`).
- Role permissions: `ADMIN` (Hosts managing tables and approving requests) and `PLAYER` (Viewing tables and submitting requests).

### 9. Android Connectivity & IP Persistence
- **Top-Left Globe Status Indicator:** Live visual indicator on the Android Home Screen (Green = Connected, Red = Disconnected).
- **Persistent Server IP:** Server Base URL is stored in `SharedPreferences` (`ServerConfigManager`) and automatically loaded on app restart without re-entry.
- **Rebuildable Retrofit Client:** Dynamically updates backend endpoints on the fly when the host switches Wi-Fi networks or server addresses.

---

## 🏗 System Architecture

The repository is structured as a clean monorepo containing three core components:

```
BankPoker/
├── server/          # Backend REST API (Node.js + Express + SQLite)
├── web/             # Frontend Client for Players (React 19 + Vite + Tailwind CSS)
├── app/             # Native Mobile Admin App (Kotlin + Jetpack Compose + Room)
├── REQUIREMENTS.md  # Detailed setup checklist and dependencies for fresh environments
└── README.md        # Product documentation
```

### Data Flow Overview

```
                      ┌────────────────────────────────────────┐
                      │          Android Host App              │
                      │  (Kotlin, Compose, Room, Retrofit)     │
                      └──────────────────┬─────────────────────┘
                                         │ HTTP / REST (JWT)
                                         ▼
┌──────────────────────┐      ┌────────────────────────┐      ┌──────────────────────┐
│   Player Web App     │ ───► │     Node.js Server     │ ◄─── │   SQLite Database    │
│ (React, Vite, Axios) │      │ (Express, Auth, Sync)  │      │  (server/bankpoker.db)│
└──────────────────────┘      └────────────────────────┘      └──────────────────────┘
```

---

## 🚀 How to Run

### 1. Start the Backend Server

```bash
cd server
npm install
npm run dev
```

- **Port:** `3000` (`http://localhost:3000`)
- **Health Check:** `http://localhost:3000/api/health`
- **Database:** Auto-creates `server/bankpoker.db` on launch.

### 2. Start the Player Web App

```bash
cd web
npm install
npm run dev
```

- **Port:** `5173` (`http://localhost:5173`)
- **Backend API URL:** Configured via `VITE_API_URL` (defaults to `http://localhost:3000`).

### 3. Run the Android Admin App

1. Open the project root in **Android Studio**.
2. Sync Gradle dependencies.
3. Build and launch on an Android device or emulator:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```
4. **Configure Server IP in Android:**
   - Tap the **Globe Icon** in the top-left of the Android Home Screen.
   - Enter your server IP (e.g. `10.0.2.2` for emulator, or your local LAN IP like `192.168.1.50`).
   - Tap **Save & Connect**. The indicator will turn **Green** upon successful connection.

### 4. Docker Compose Deployment (Ubuntu PC Production)

For self-hosted production deployment on the dedicated Ubuntu server (with Nginx reverse proxy and persistent SQLite storage):

```bash
# 1. Copy environment template
cp .env.example .env

# 2. Build and run the production stack
docker compose up -d --build
```

- **Nginx Entrypoint:** Port `80` (`http://localhost/` or `http://<UBUNTU_IP>/`)
- **Persistent Data:** Stored in `./data/bankpoker.db`
- Complete migration from PM2 and safety instructions: 👉 **[DEPLOY_UBUNTU.md](DEPLOY_UBUNTU.md)**

---

## 👤 User Accounts & Registration

BankPoker uses dynamic JWT authentication. You can create accounts directly through the application:

- **Register via Web App:** Navigate to `http://localhost:5173/register` (or `http://localhost/register` in Docker) to create a player account.
- **Register / Test via Android App:** Open the Server Settings screen (tap the Globe icon) to test authentication, registration, and login.

---

## ⚙️ Documentation & Deployment Guides

- **Ubuntu Production Deployment & Migration:** 👉 **[DEPLOY_UBUNTU.md](DEPLOY_UBUNTU.md)**
- **System Requirements & Local Setup:** 👉 **[REQUIREMENTS.md](REQUIREMENTS.md)**

---

## 📄 License

This project is maintained for private poker management and educational use.
