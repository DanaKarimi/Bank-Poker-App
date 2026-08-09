# Bank Poker - Android App

A native Android app for poker chip bankers/hosts to manage tables, players, buy-ins, exits, and settlements.

## Features

- Create and manage poker tables
- Add players to tables
- Record chip buy-ins for active players
- Record exit amounts (can be different from current balance)
- Track player results (Creditor/Debtor/Break-even)
- Optional chip value for money equivalent calculations
- Transaction history with timestamps
- Table summary with totals
- Settlement status tracking

## Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose
- **Design**: Material 3 (Dark theme with green accent)
- **Architecture**: MVVM with ViewModel + Repository
- **Database**: Room (SQLite)
- **Async**: Kotlin Coroutines and Flow
- **Navigation**: Navigation Compose

## Project Structure

```
app/src/main/java/com/bankpoker/app/
├── MainActivity.kt
├── data/
│   ├── local/
│   │   ├── BankPokerDatabase.kt
│   │   ├── dao/
│   │   │   ├── BuyInDao.kt
│   │   │   ├── ExitRecordDao.kt
│   │   │   ├── PlayerDao.kt
│   │   │   └── PokerTableDao.kt
│   │   └── entity/
│   │       ├── BuyIn.kt
│   │       ├── ExitRecord.kt
│   │       ├── Player.kt
│   │       └── PokerTable.kt
│   └── repository/
│       └── PokerRepository.kt
├── ui/
│   ├── navigation/
│   │   ├── AppNavigation.kt
│   │   └── Screen.kt
│   ├── screens/
│   │   ├── TableDetailScreen.kt
│   │   └── TablesScreen.kt
│   └── theme/
│       ├── Color.kt
│       ├── Theme.kt
│       └── Type.kt
└── viewmodel/
    ├── TableDetailViewModel.kt
    ├── TableDetailViewModelFactory.kt
    ├── TablesViewModel.kt
    └── TablesViewModelFactory.kt
```

## How to Build and Run

### Prerequisites

1. Android Studio Hedgehog (2023.1.1) or newer
2. JDK 17 or newer
3. Android SDK with API level 35

### Steps

1. **Open the project in Android Studio**
   - Launch Android Studio
   - Select "Open an Existing Project"
   - Navigate to the `BankPoker` folder and open it

2. **Sync Gradle files**
   - Android Studio will automatically sync the project
   - Wait for the sync to complete

3. **Configure SDK path** (if needed)
   - Edit `local.properties` and set the correct SDK path:
     ```
     sdk.dir=/path/to/your/Android/sdk
     ```

4. **Build the debug APK**
   - In Android Studio: Build → Build Bundle(s) / APK(s) → Build APK(s)
   - Or via command line:
     ```bash
     ./gradlew assembleDebug
     ```
   - The APK will be generated at: `app/build/outputs/apk/debug/app-debug.apk`

5. **Run on device/emulator**
   - Connect an Android device or start an emulator
   - Click the Run button in Android Studio
   - Or install the APK manually:
     ```bash
     adb install app/build/outputs/apk/debug/app-debug.apk
     ```

## Usage Guide

### Creating a Table

1. Open the app - you'll see the Tables screen
2. Tap the floating action button (+)
3. Enter a table name (required)
4. Optionally enter a chip value (e.g., 1 means 1 chip = $1)
5. Tap "Create"

### Managing a Table

1. Tap on a table card to open the Table Detail screen
2. Use the tabs to navigate between sections:

#### Players Tab
- View all players in the table
- See player status (Playing/Exited)
- View current balance for active players
- Add new players with the + button
- Quick access to Buy-in and Exit actions

#### Actions Tab
- Quick access to Buy-in and Exit buttons for each active player
- Only shows players with "Playing" status

#### History Tab
- View all transactions (buy-ins and exits)
- Sorted by newest first
- Shows transaction type, player name, amount, note, and timestamp

#### Results Tab
- View settlement status (Ready/Waiting for exits)
- See each player's final result
- Shows: total buy-ins, total exits, net result
- Labels: Creditor (positive), Debtor (negative), Break-even (zero)
- If chip value is set, also shows money equivalent

### Recording a Buy-in

1. Go to Players or Actions tab
2. Tap "Buy-in" for the desired player
3. Enter the chip amount (must be > 0)
4. Optionally add a note
5. Tap "Save"

### Recording an Exit

1. Go to Players or Actions tab
2. Tap "Exit" for the desired player
3. Enter the exit chip amount (can be any non-negative number)
4. Note: Exit amount can be different from current balance
5. Optionally add a note
6. Tap "Save Exit"
7. Player status changes to "Exited"

### Closing a Table

1. Tap the "Close" button in the top bar
2. Confirm the action
3. No new transactions will be allowed

## Business Rules

- A player can have multiple buy-ins
- A player normally has one exit (which marks them as Exited)
- Exit amount is NOT validated against current balance
- Net Result = Total Exits - Total Buy-ins
  - Positive = Creditor (player won)
  - Negative = Debtor (player lost)
  - Zero = Break-even
- Table Remaining Balance = Total Buy-ins - Total Exits
- Closed tables cannot accept new transactions

## Database Schema

### PokerTable
- id (UUID)
- name (String)
- chipValue (Long?, nullable)
- status (ACTIVE/CLOSED)
- createdAt (timestamp)
- closedAt (timestamp?, nullable)

### Player
- id (UUID)
- tableId (String)
- name (String)
- status (PLAYING/EXITED)
- createdAt (timestamp)

### BuyIn
- id (UUID)
- tableId (String)
- playerId (String)
- amount (Long)
- note (String?, nullable)
- createdAt (timestamp)

### ExitRecord
- id (UUID)
- tableId (String)
- playerId (String)
- amount (Long)
- note (String?, nullable)
- createdAt (timestamp)

## Minimum Requirements

- Minimum SDK: 24 (Android 7.0)
- Target SDK: 35 (Android 15)
- Compile SDK: 35

## License

This project is provided as-is for educational and personal use.
