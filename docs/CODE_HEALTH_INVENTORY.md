# Code Health Inventory & Conservative Cleanup Plan

**Date**: September 21, 2026  
**Branch**: `features`  
**Repository**: `BankPoker`

---

## 1. Unused Files by Area

### A. Server (`server/src/**`)
- **Active Files**: All 14 files in `server/src/` (`server.js`, `socket.js`, `database/db.js`, `database/schema.sql`, `middleware/auth.js`, `services/notifications.js`, `utils/helpers.js`, and the 7 route files) are actively loaded and mounted.
- **Untracked / Root Scripts**:
  - `import_legacy_backup.js`: Root-level one-off utility for migrating legacy JSON backups into SQLite. Not referenced by runtime, kept untouched as administrative utility.

### B. Web (`web/src/**`)
- **Unreferenced Dead Components**:
  - `web/src/components/TableDetailModal.jsx` (655 lines): Originally used for viewing table details in a modal; completely superseded by the dedicated page `web/src/pages/TableDetail.jsx`. References: 0.
  - `web/src/components/RequestModal.jsx` (187 lines): Legacy generic modal for buy-in/exit requests; completely superseded by `web/src/components/BuyInModal.jsx` and `web/src/components/ExitModal.jsx`. References: 0.
- **Unreferenced Assets**:
  - `web/src/assets/react.svg`: Vite starter template asset. References: 0.
  - `web/src/assets/vite.svg`: Vite starter template asset. References: 0.
  - `web/src/assets/hero.png`: Unreferenced in code.
- **Active Components (Kept)**:
  - `AvatarSystem.jsx`, `BalancesTab.jsx`, `BuyInModal.jsx`, `ExitModal.jsx`, `GroupCodeChip.jsx`, `HistoryTab.jsx`, `NotificationsDropdown.jsx`, `ProfileModal.jsx`, `ProtectedRoute.jsx`, `RequestCard.jsx`, `SmartAmountInput.jsx`, `StatsTab.jsx`, `StatusBadge.jsx`, `TableCard.jsx`.

### C. Android (`app/src/main/java/**`)
- **Unreferenced Classes / Files**:
  - `app/src/main/java/com/bankpoker/app/data/sync/SyncOutboxManager.kt` (119 lines): Offline outbox sync worker scaffold; currently unreferenced by ViewModels or WorkManager. Kept untouched due to potential offline feature roadmap.
- **DTO Inspection**:
  - All DTO classes in `data/remote/dto/` (e.g., `AdminOwnerDto`, `AdminLinkedUserDto`, `NotificationPayload`, `TableMyStatsDto`) are actively referenced either directly or as nested types for Retrofit / Gson JSON deserialization.

---

## 2. Unused Exported Functions & Symbols

### Web
- `web/src/api.js`:
  - `getGroupSettlement`: Redundant alias for `getGroupSettlementPlan`. Only imported in `StatsTab.jsx` but not called (which calls `getGroupSettlementPlan`). Can be safely aliased or cleaned.
- `web/src/components/TableDetailModal.jsx`: default export (dead file).
- `web/src/components/RequestModal.jsx`: default export (dead file).

### Android
- `TableDetailScreen.kt`:
  - `formatTimestamp(timestamp: Long)`: Declared as top-level function in `TableDetailScreen.kt` and consumed cross-file by `GroupHistoryScreen.kt`. Candidate for moving to a dedicated utility file.

---

## 3. Duplicate Patterns

### A. Formatting Helpers
- **Balance / Currency Formatting**:
  - **Web**: `amount >= 0 ? "+$" + amount.toLocaleString() : "-$" + Math.abs(amount).toLocaleString()` repeated with minor variations in:
    - `web/src/pages/TableDetail.jsx`
    - `web/src/pages/GroupStats.jsx`
    - `web/src/components/BalancesTab.jsx`
    - `web/src/components/StatsTab.jsx`
    - `web/src/components/HistoryTab.jsx`
    - `web/src/pages/AdminDashboard.jsx`
  - **Android**: `java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(...)` repeated in `GroupDetailScreen.kt`, and `formatAmount()` in `TableDetailScreen.kt`.
- **Date & Time Formatting**:
  - **Web**: `new Date(ts).toLocaleString()` / `toLocaleDateString()` duplicated across card components and history tabs.
  - **Android**: `SimpleDateFormat("MMM dd, yyyy HH:mm", ...)` duplicated.

### B. Name Normalization
- **Server**:
  - `(name || '').trim().toLowerCase()` repeated ~20 times across `server/src/routes/groups.js` and `server/src/routes/tables.js` for player identity matching and deduplication.

### C. Socket Event Listeners
- **Android**:
  - `GroupDetailViewModel` and `TableDetailViewModel` repeat similar listener attachment and teardown boilerplate for `entry_fee_updated`, `payment_recorded`, `payment_updated`, `payment_deleted`.
- **Web**:
  - `GroupStats.jsx` and `TableDetail.jsx` have identical socket event registration patterns.

### D. Repository & DTO Mapping
- Manual JSON/DTO to Domain entity mappings in `RemoteRepository.kt` and `PokerRepository.kt`.

---

## 4. Large Files & Mixed Responsibilities

| File Path | Lines | Responsibilities / Complexity |
|:---|:---:|:---|
| `app/.../ui/screens/TableDetailScreen.kt` | 3,629 | Table state, tab pager (Players, History, Results), buy-in/exit dialogs, compact scroll header connection, share sheet text generation, player deletion |
| `server/src/routes/groups.js` | 2,606 | Group CRUD, members, balances calculation, payments CRUD, settlement calculation, entry fee sync, player claiming |
| `app/.../ui/screens/GroupDetailScreen.kt` | 2,136 | Group tabs (Tables, Players, Balances, History), payments bottom sheet, invite bottom sheet, settlement cards |
| `web/src/pages/TableDetail.jsx` | 1,607 | Web table view, player management, buy-in/exit modals, live socket updates, share text generator |
| `app/.../ui/screens/HomeScreen.kt` | 1,590 | Dashboard, group list, table list, quick table creation, join group dialog |
| `app/.../repository/RemoteRepository.kt` | 1,542 | Monolithic remote repository covering Auth, Groups, Tables, Requests, Admin, Notifications, Sync |
| `server/src/routes/tables.js` | 1,487 | Table CRUD, buy-ins, exits, player state transitions, table status closure |
| `web/src/pages/Dashboard.jsx` | 1,447 | Main dashboard, group cards, table cards, creation dialogs, notification drawer |
| `app/.../ui/screens/AdminManagementScreen.kt` | 1,199 | Super-admin management for users, groups, tables |
| `app/.../ui/screens/GroupHistoryScreen.kt` | 1,160 | Payments history tab, entry fees history tab, edit/delete bottom sheets |
| `app/.../data/repository/PokerRepository.kt` | 893 | Local Room database repository |
| `web/src/pages/AdminDashboard.jsx` | 836 | Web super-admin dashboard |
| `web/src/pages/GroupStats.jsx` | 831 | Group overview, balances, settlement, history container |
| `app/.../ui/screens/TablesScreen.kt` | 790 | Dedicated tables screen |
| `app/.../ui/components/PremiumComponents.kt` | 780 | Custom design system components |

---

## 5. Candidate Safe Cleanup Actions (Ranked)

### SAFE (Execute in Step 1 & Step 2)
1. **Remove Completely Unreferenced Files**:
   - `web/src/components/TableDetailModal.jsx` (655 lines)
   - `web/src/components/RequestModal.jsx` (187 lines)
   - `web/src/assets/react.svg`
   - `web/src/assets/vite.svg`
2. **Remove Dead Imports & Unused Variables**:
   - Clean unused imports in `web/src/**` and `app/src/main/java/**`.
   - Remove dead variable assignments.
3. **Consolidate Low-Risk Helpers**:
   - Server: Extract `normalizeName(name)` in `server/src/utils/helpers.js` to unify `(name || '').trim().toLowerCase()`.
   - Web: Create `web/src/utils/formatters.js` to unify `formatCurrency(amount)` (`+$3,000`, `-$1,700`, `$0`) and date formatting without modifying UI output.
   - Android: Consolidate `formatGroupBalance(balance)` and `formatTimestamp(ts)` into a shared `com.bankpoker.app.ui.util.Formatters.kt` to eliminate cross-screen dependency.
4. **Clarify Folder Organization**:
   - Align `RemoteRepository.kt` location and ensure package consistency.

### MEDIUM (Execute with Caution)
1. Consolidate repetitive DTO-to-entity mapping extensions in Android.
2. Standardize error handling toast/notification helpers.

### RISKY (DO NOT TOUCH in this pass)
- DO NOT split `TableDetailScreen.kt` or `GroupDetailScreen.kt` into multiple files (avoids UI state breakage, navigation regressions, and complex recomposition bugs).
- DO NOT split `server/src/routes/groups.js` or `server/src/routes/tables.js` (avoids route ordering issues, Express middleware breaks, or transaction lifecycle changes).
- DO NOT alter payment math, balance calculation, settlement algorithm, or entry fee single-source-of-truth logic.
