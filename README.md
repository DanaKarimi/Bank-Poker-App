# 🃏 BankPoker — Poker Bank Manager

BankPoker is a native Android app (Kotlin + Jetpack Compose + Room) designed for poker home-game hosts and bankers. It provides complete management of poker tables, players, buy-ins, exits, and settlements. The app supports both standalone "Quick Tables" and persistent "Groups" for recurring games, with automatic balance accumulation and settlement planning. All wrapped in a beautiful Casino Classic green-felt and gold theme.

## ✨ Features

### 🏠 Home Screen
- **Two main modes on launch:**
  - **Quick Table** – Start a one-time standalone game
  - **Groups** – Manage your recurring poker circles

### ⚡ Quick Table
- Create standalone poker tables with optional chip value
- Tables list shows **only** standalone tables (group tables are kept completely separate)
- Active/Closed status badges
- Full table management: players, buy-ins, exits, history, results

### 👥 Groups
- Create player groups (e.g., "Friday Poker Night")
- Each group has its **own isolated tables** – never mixed with Quick Tables
- **3 tabs per group:**
  - **Tables** – View and create tables specific to this group
  - **Balances** – Cumulative ledger across all closed tables in the group
  - **Stats** – Overview, biggest winner/debtor, and settlement plan

### 🎰 Table Detail
- **3 tabs:** Players / History / Results
- Add players manually to the table
- Record Buy-ins and Exits with optional notes
- Live balance per player (in chips)
- **Table Summary:** total buy-ins, total exits, remaining chips in play
- **Automatic Settlement Plan** on close (greedy algorithm: who pays whom)
- Share results, edit/delete tables

### 💰 Voroodi (Entry Fee) Tracking
- Optional **"Voroodi?" switch** when creating a group table
- If enabled:
  - A **check-circle button** appears next to each player's avatar
  - Tap to toggle: **Paid** (green ✓) or **Unpaid** (grey outline)
  - Shows "Voroodi: Paid" (green) or "Voroodi: Unpaid" (red) under player name
- Perfect for tracking entry fees separate from chip buy-ins

### 📊 Group Balances & Stats
- **Balances accumulate automatically** when any table in the group is closed
- Net result (exits − buy-ins) for each player is added to their group balance
- **Stats tab shows:**
  - Overview: total tables, closed tables, player count
  - Total chips settled so far
  - 🏆 **Biggest Winner** (green highlight)
  - 💸 **Biggest Debtor** (red highlight)
  - **Group-wide Settlement Plan** with "PAID ✓" button to mark payments
  - Payment history log

### 🎨 Casino Classic Design
- Green felt background with radial gradient
- Gold accents, borders, and typography
- Cream-colored text for readability
- FeltCard backgrounds with gold-bordered cards
- Avatar colors assigned per player for visual distinction

## 🛠 Tech Stack

| Technology | Usage |
|------------|-------|
| **Kotlin** | Primary language |
| **Jetpack Compose** | Declarative UI |
| **Material 3** | Components and theming |
| **Room (SQLite)** | Local database with migrations |
| **MVVM Architecture** | ViewModel + StateFlow + Flow |
| **Coroutines** | Async operations |
| **Navigation Compose** | Screen navigation |

## 📦 Project Structure

```
app/src/main/java/com/bankpoker/app/
├── MainActivity.kt
├── data/
│   ├── local/
│   │   ├── BankPokerDatabase.kt          # Room DB with migrations
│   │   ├── dao/
│   │   │   ├── BuyInDao.kt
│   │   │   ├── ExitRecordDao.kt
│   │   │   ├── GroupBalanceDao.kt
│   │   │   ├── PaymentDao.kt
│   │   │   ├── PlayerDao.kt
│   │   │   └── PokerTableDao.kt
│   │   └── entity/
│   │       ├── BuyIn.kt
│   │       ├── ExitRecord.kt
│   │       ├── Group.kt
│   │       ├── GroupBalance.kt
│   │       ├── Payment.kt
│   │       ├── Player.kt                  # + entryFeePaid field
│   │       └── PokerTable.kt              # + hasEntryFee, entryFee, groupId
│   └── repository/
│       └── PokerRepository.kt
├── ui/
│   ├── navigation/
│   │   ├── AppNavigation.kt
│   │   └── Screen.kt
│   ├── screens/
│   │   ├── HomeScreen.kt                  # Two-mode landing page
│   │   ├── TablesScreen.kt                # Quick Tables only
│   │   ├── TableDetailScreen.kt
│   │   ├── GroupsScreen.kt
│   │   ├── GroupDetailScreen.kt           # Tables/Balances/Stats tabs
│   │   └── StatsScreen.kt
│   └── theme/
│       ├── Color.kt                       # Gold, FeltBackground, Cream, etc.
│       ├── Theme.kt
│       └── Type.kt
└── viewmodel/
    ├── TableDetailViewModel.kt            # + toggleEntryFee()
    ├── TablesViewModel.kt                 # Filters groupId = NULL
    ├── GroupDetailViewModel.kt
    └── (Factories)
```

## 🚀 Getting Started

### Prerequisites
1. Android Studio Hedgehog (2023.1.1) or newer
2. JDK 17 or newer
3. Android SDK with API level 35

### Steps
1. **Clone the repository**
   ```bash
   git clone https://github.com/DanaKarimi/Bank-Poker-App.git
   cd Bank-Poker-App
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the project folder

3. **Sync Gradle files**
   - Android Studio will automatically sync
   - Wait for dependencies to download

4. **Configure SDK path** (if needed)
   - Edit `local.properties`:
     ```properties
     sdk.dir=/path/to/your/Android/sdk
     ```

5. **Build and Run**
   - Click the Run button in Android Studio
   - Or via command line:
     ```bash
     ./gradlew assembleDebug
     adb install app/build/outputs/apk/debug/app-debug.apk
     ```

### Minimum Requirements
- **Minimum SDK:** 24 (Android 7.0)
- **Target SDK:** 35 (Android 15)
- **Compile SDK:** 35

## 📋 Business Rules

### Tables
- A table can be **Active** or **Closed**
- Closed tables cannot accept new transactions
- Tables are either **standalone** (groupId = null) or **linked to a group**

### Players & Transactions
- A player can have multiple buy-ins
- A player normally has one exit (marks them as Exited)
- Exit amount is NOT validated against current balance (flexible cash-out)
- **Net Result = Total Exits − Total Buy-ins**
  - Positive → Creditor (won)
  - Negative → Debtor (lost)
  - Zero → Break-even

### Groups
- Group balances accumulate **only when a table is closed**
- Settlement plan uses a greedy two-pointer algorithm
- Payments can be marked as "PAID" to track real-world settlements

### Voroodi (Entry Fee)
- Optional feature for group tables only
- Tracked separately from chip buy-ins/exits
- Visual indicator per player (✓ paid / ○ unpaid)

## 📄 License

This project is provided as-is for educational and personal use.
