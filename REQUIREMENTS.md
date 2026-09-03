# BankPoker - System Requirements & Setup

A complete setup checklist and dependency guide for setting up the BankPoker platform on a fresh machine (Ubuntu Linux, macOS, or Windows).

---

## 1. Prerequisites (Install Once)

| Requirement | Recommended Version | Purpose |
|-------------|---------------------|---------|
| **Node.js** | Node.js `20.x` or `22.x` (LTS) | Runs the backend server and Vite frontend build tools |
| **npm** | `10.x` or higher (bundled with Node.js) | Package manager for Node.js modules |
| **Git** | `2.40+` | Version control |
| **JDK (Java Development Kit)** | OpenJDK `17` or `11` | Required for building the Android application via Gradle |
| **Android Studio** | Ladybug / Hedgehog / Koala or newer | Android SDK manager, IDE, and emulator |
| **Android SDK** | Compile & Target API `35` (Android 15), Min API `24` (Android 7.0) | Android platform compilation |

### Ubuntu / Debian Installation Commands

```bash
# Update package list & install essentials
sudo apt update && sudo apt install -y curl git openjdk-17-jdk build-essential

# Install Node.js 20.x (LTS) via NodeSource
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# Verify versions
node -v   # v20.x.x
npm -v    # 10.x.x
java -version  # openjdk version "17.x.x"
git --version
```

---

## 2. Server (Node.js Backend)

- **Location:** `server/`
- **Runtime:** Node.js `>= 18.0.0` (CommonJS module format)
- **Database:** SQLite3 file database (`server/bankpoker.db`), automatically created and migrated on start.

### Dependencies (from `server/package.json`)

#### Production Dependencies
- `express`: `^5.2.1` — HTTP web and REST API framework
- `sqlite3`: `^6.0.1` — SQLite driver for database persistence
- `jsonwebtoken`: `^9.0.3` — JWT generation and authentication verification
- `bcryptjs`: `^3.0.3` — Password hashing and comparison
- `cors`: `^2.8.6` — Cross-Origin Resource Sharing middleware
- `dotenv`: `^17.4.2` — Environment variable loader

#### Development Dependencies
- `nodemon`: `^3.1.14` — Automatic server restarter on code changes

### npm Scripts

| Script | Command | Purpose |
|--------|---------|---------|
| `npm start` | `node src/server.js` | Runs the production server |
| `npm run dev` | `nodemon src/server.js` | Runs development server with auto-reload on file edits |

### Environment Variables (`server/.env`)

Create a `.env` file inside the `server/` folder:

```env
# Server listen port (default: 3000)
PORT=3000

# Secret key for JWT signing & verification (replace with a secure random string in production)
JWT_SECRET=super_secret_bankpoker_key_change_in_production
```

### Installation & Execution

```bash
cd server
npm install
npm run dev
```

- Server URL: `http://localhost:3000`
- Health check endpoint: `http://localhost:3000/api/health`

---

## 3. Web App (React Frontend)

- **Location:** `web/`
- **Framework:** React 19 + Vite 8 + Tailwind CSS 3
- **Module System:** ES Modules (`"type": "module"`)

### Dependencies (from `web/package.json`)

#### Production Dependencies
- `react`: `^19.2.8` — UI component library
- `react-dom`: `^19.2.8` — DOM renderer for React
- `react-router-dom`: `^7.18.3` — Client-side declarative routing
- `axios`: `^1.20.0` — Promise-based HTTP client
- `lucide-react`: `^1.38.0` — Clean, modern icon set

#### Development Dependencies
- `vite`: `^8.2.2` — Fast frontend development server and bundler
- `@vitejs/plugin-react`: `^6.1.0` — React Fast Refresh plugin for Vite
- `tailwindcss`: `^3.4.19` — Utility-first CSS styling
- `postcss`: `^8.5.26` — CSS processing
- `autoprefixer`: `^10.5.4` — Vendor prefix parser
- `oxlint`: `^1.79.0` — High-performance JavaScript/JSX linter
- `@types/react`: `^19.2.18` — TypeScript definitions for React
- `@types/react-dom`: `^19.2.4` — TypeScript definitions for React DOM

### npm Scripts

| Script | Command | Purpose |
|--------|---------|---------|
| `npm run dev` | `vite` | Starts the local dev server at `http://localhost:5173` |
| `npm run build` | `vite build` | Compiles optimized static assets to `web/dist/` |
| `npm run preview` | `vite preview` | Serves the production build locally |
| `npm run lint` | `oxlint` | Lints the JavaScript/JSX source code |

### Environment Variables (`web/.env`)

Optional environment file inside `web/`:

```env
# URL pointing to the Node.js backend API (defaults to http://localhost:3000 if omitted)
VITE_API_URL=http://localhost:3000
```

### Installation & Execution

```bash
cd web
npm install
npm run dev
```

- Web UI URL: `http://localhost:5173`

---

## 4. Android App (Admin & Host Mobile Client)

- **Location:** `app/`
- **Language:** Kotlin `2.0.21`
- **UI Toolkit:** Jetpack Compose (Compose BOM `2024.10.00` with Material 3 `1.3.0`)
- **Local Database:** Room `2.6.1` (SQLite with Schema Migrations)

### Android SDK & Tooling Specifications

| Property | Value |
|----------|-------|
| **Minimum SDK** | `24` (Android 7.0 Nougat) |
| **Target SDK** | `35` (Android 15) |
| **Compile SDK** | `35` |
| **Android Gradle Plugin (AGP)** | `8.7.0` |
| **Gradle Wrapper** | `8.9` |
| **Kotlin Version** | `2.0.21` |
| **KSP Version** | `2.0.21-1.0.25` |
| **Java Compatibility** | Java 11 / JVM Target 11 (requires JDK 17 or JDK 11 to build) |

### Key Libraries & Versions (from `gradle/libs.versions.toml` & `app/build.gradle.kts`)

- `androidx.compose:compose-bom`: `2024.10.00`
- `androidx.compose.material3:material3`: `1.3.0`
- `androidx.compose.material:material-icons-extended`: Latest compatible
- `androidx.navigation:navigation-compose`: `2.8.3`
- `androidx.room:room-runtime`, `room-ktx`, `room-compiler`: `2.6.1`
- `com.squareup.retrofit2:retrofit`: `2.9.0`
- `com.squareup.retrofit2:converter-gson`: `2.9.0`
- `com.squareup.okhttp3:logging-interceptor`: `4.11.0`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`: `1.9.0`
- `androidx.activity:activity-compose`: `1.9.2`
- `androidx.lifecycle:lifecycle-runtime-ktx`: `2.8.6`
- `androidx.core:core-splashscreen`: `1.0.1`

### How to Open & Run in Android Studio

1. Open Android Studio.
2. Select **Open** and choose the root `BankPoker` repository directory.
3. Wait for Gradle Sync to finish downloading all plugins and dependencies.
4. Select a connected physical Android device or launch an Android Virtual Device (AVD).
5. Click **Run 'app'** (`Shift + F10`) or execute from the terminal:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

### Server IP & Base URL Configuration

The Android app includes a persistent configuration manager (`ServerConfigManager`) that saves the server URL in Android `SharedPreferences`:
- **Default Base URL:** `http://10.0.2.2:3000/` (for Android Studio Emulator).
- **Physical Device / Local LAN:** Tap the **Globe Icon** on the top-left of the Home Screen, enter your PC's LAN IP (e.g. `192.168.1.50`), and tap **Save & Connect**.
- **Persistence:** The entered IP is normalized (adding `http://`, `:3000`, and trailing `/` automatically) and persists across app restarts without re-entry.

---

## 5. Network & Deployment Notes

### Local Network Connectivity

```
[ Android Device / Emulator ]
         │
         ▼  (HTTP requests to host IP:3000 or 10.0.2.2:3000)
[ Node.js Server :3000 ] ◄─── SQLite (server/bankpoker.db)
         ▲
         │  (HTTP requests to localhost:3000 or VITE_API_URL)
[ React Web App :5173 ]
```

- **Android Emulator:** Use `http://10.0.2.2:3000/` to reach the development machine's `localhost`.
- **Physical Android Phones on Wi-Fi:** Ensure PC and phone are on the same Wi-Fi. Find PC IP using `ip a` (Linux) or `ipconfig` (Windows) and set `http://<PC_LAN_IP>:3000/` in the Android Server Settings screen.
- **Firewall:** Ensure inbound TCP traffic on port `80` (or `3000` for direct dev) is allowed:
  ```bash
  # Ubuntu Linux (ufw)
  sudo ufw allow 80/tcp
  sudo ufw allow 3000/tcp
  ```

### Production Deployment (Docker & Nginx on Ubuntu PC)

For production deployment on the Ubuntu PC, BankPoker uses Docker Compose with Nginx as a reverse proxy on port 80 and persistent SQLite storage in `./data/`:

- Complete production setup, safe PM2 database migration, and backup guide:
  👉 **[DEPLOY_UBUNTU.md](DEPLOY_UBUNTU.md)**

### Public Access (Future Step: Cloudflare Tunnel -> bankjoker.ir)

For secure remote access without port forwarding or static public IPs:
1. Cloudflare Tunnel runs on the Ubuntu PC as a daemon.
2. Routes incoming HTTPS traffic from `bankjoker.ir` to the local Nginx container on `http://localhost:80`.
3. Nginx proxies to the BankPoker Express container on `http://bankpoker:3000`.
