# 🚀 BankPoker - Ubuntu Production Deployment Guide

This guide provides the complete, step-by-step procedure for deploying the Dockerized BankPoker platform on the dedicated **Ubuntu PC (Production Server)** while safely migrating the existing production SQLite database from the current PM2 service.

---

## 🏗 System & Network Architecture

```
Internet (Future: Cloudflare Tunnel -> bankjoker.ir)
   │
   ▼
[ Host Port 80 ]
   │
   ▼
┌─────────────────────────────────────────────────────────────┐
│ Docker Bridge Network (bankpoker-network)                    │
│                                                             │
│   ┌─────────────────────┐                                   │
│   │   bankpoker-nginx   │ (Reverse proxy, gzip, no-cache)   │
│   │   (nginx:alpine)    │                                   │
│   └──────────┬──────────┘                                   │
│              │ proxy_pass http://bankpoker:3000              │
│              ▼                                              │
│   ┌─────────────────────┐                                   │
│   │    bankpoker-app    │ (Node 20 LTS + Express 5 +        │
│   │ (built React SPA)   │  built React SPA static assets)   │
│   └──────────┬──────────┘                                   │
│              │ DATABASE_PATH=/app/data/bankpoker.db         │
└──────────────┼──────────────────────────────────────────────┘
               ▼
┌─────────────────────────────────────────────────────────────┐
│ Ubuntu Host Persistent Storage                              │
│ Directory: ./data/                                          │
│ Database:  ./data/bankpoker.db (SQLite)                     │
└─────────────────────────────────────────────────────────────┘
```

- **Nginx** is the only service that publishes a port to the host (`80:80`).
- **Node.js Express server** runs internally on port 3000 inside the Docker network (unexposed directly to the public network).
- **SQLite Database** resides in the host directory `./data/bankpoker.db`, surviving container restarts, rebuilds, and `docker compose down`.

---

## 📋 Step 1: Install Docker & Docker Compose on Ubuntu (One-Time)

If Docker and Docker Compose v2 are not already installed on the Ubuntu PC, install them using the official Docker repository:

```bash
# 1. Update package list and install prerequisites
sudo apt update
sudo apt install -y ca-certificates curl gnupg lsb-release

# 2. Add Docker official GPG key
sudo install -m 0755 -d /etc/apt/keyrings
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
sudo chmod a+r /etc/apt/keyrings/docker.gpg

# 3. Add Docker repository
echo \
  "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
  $(lsb_release -cs) stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

# 4. Install Docker Engine and Docker Compose plugin
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin

# 5. Allow current user to run Docker without sudo (requires re-login or newgrp)
sudo usermod -aG docker $USER
newgrp docker

# 6. Verify installation
docker --version
docker compose version
```

---

## 💾 Step 2: CRITICAL — Safe Pre-Migration Database Backup

> [!CAUTION]
> **DO NOT SKIP THIS STEP.** The Ubuntu PC contains real game and player data. Create a verified backup copy in your home directory before proceeding.

```bash
cd ~/Bank-Poker-App

# Create a permanent backups directory outside the repository
mkdir -p ~/bankpoker_backups

# Copy the existing PM2 SQLite database with a timestamp
cp server/bankpoker.db ~/bankpoker_backups/bankpoker_backup_$(date +%Y%m%d_%H%M%S).db

# Verify backup size and integrity
ls -lh ~/bankpoker_backups/
```

---

## 🔄 Step 3: Pull the Latest Code from GitHub

```bash
cd ~/Bank-Poker-App

# Fetch and switch to the features branch
git checkout features
git pull origin features
```

---

## 📁 Step 4: Prepare Persistent Data Directory & Copy Database

1. Create the persistent `data/` directory at the project root:
   ```bash
   mkdir -p data
   ```

2. Copy the active production database into the persistent directory:
   ```bash
   cp server/bankpoker.db data/bankpoker.db
   ```

3. Ensure proper file permissions so the container's non-root `node` user can read and write to the database and directory:
   ```bash
   chmod 775 data
   chmod 664 data/bankpoker.db
   chown -R $USER:$USER data
   ```

---

## 🛑 Step 5: Stop the Old PM2 Service

Before launching the Docker containers, stop the old Node.js PM2 process to release database locks and avoid concurrent writes:

```bash
# Check the running PM2 process name
pm2 status

# Stop the BankPoker process (do NOT delete it yet)
pm2 stop <app-name-or-id>
```

> [!NOTE]
> Do NOT run `pm2 delete` or uninstall PM2 yet. Keeping the configuration intact allows an instant rollback if needed.

---

## 🔐 Step 6: Configure Environment Variables

1. Copy the environment template:
   ```bash
   cp .env.example .env
   ```

2. Generate a secure, cryptographically random JWT secret:
   ```bash
   openssl rand -base64 32
   ```

3. Edit `.env` (`nano .env`) and set `JWT_SECRET`:
   ```env
   PORT=3000
   DATABASE_PATH=/app/data/bankpoker.db
   HTTP_PORT=80
   JWT_SECRET=<PASTE_THE_GENERATED_SECRET_HERE>
   ```

---

## 🚀 Step 7: Build and Start with Docker Compose

Build the multi-stage image (which compiles the React SPA and packages the Express backend) and start the containers in detached mode:

```bash
docker compose build --no-cache
docker compose up -d
```

---

## ✅ Step 8: Verification & Health Checks

1. **Verify container status:**
   ```bash
   docker compose ps
   ```
   Both `bankpoker-app` and `bankpoker-nginx` should show status `Up (healthy)`.

2. **Inspect logs:**
   ```bash
   docker compose logs -f
   ```
   Confirm SQLite connects to `/app/data/bankpoker.db` and Express is running on port 3000.

3. **Test the API Health Check endpoint:**
   ```bash
   curl -i http://localhost/api/health
   ```
   Should return `HTTP/1.1 200 OK` with `{"status":"ok", ...}`.

4. **Test Web Application in Browser:**
   - On the Ubuntu PC: Open `http://localhost/` in your browser.
   - From another phone or PC on the same Wi-Fi: Find the Ubuntu PC IP using `ip -br a`, then open `http://<UBUNTU_IP>/`.
   - Log in with existing accounts to verify historical tables and player balances load accurately from the database.

---

## ⏪ Rollback Plan (If Needed)

If an unexpected issue arises during testing, revert back to the original PM2 service in seconds:

```bash
# 1. Stop Docker containers
cd ~/Bank-Poker-App
docker compose down

# 2. Restart the PM2 process
pm2 restart <app-name-or-id>

# 3. Verify PM2 status
pm2 status
```

---

## 🧹 Step 9: Retiring PM2 (Only After Full Verification)

Once you have verified that the Dockerized application is operating normally:

```bash
# Remove the old process from PM2
pm2 delete <app-name-or-id>
pm2 save
```

---

## 💾 Ongoing Database Backup Strategy

To create automated daily or weekly backups of the persistent database on Ubuntu without stopping the containers, you can use SQLite's safe online backup command:

```bash
# Safe live SQLite backup (works while database is active)
sqlite3 ~/Bank-Poker-App/data/bankpoker.db ".backup '/home/$USER/bankpoker_backups/bankpoker_$(date +%Y%m%d_%H%M%S).db'"
```

Or a standard file copy:
```bash
cp ~/Bank-Poker-App/data/bankpoker.db ~/bankpoker_backups/bankpoker_$(date +%Y%m%d_%H%M%S).db
```

To automate daily backups at 03:00 AM, add this to `crontab -e`:
```cron
0 3 * * * cp ~/Bank-Poker-App/data/bankpoker.db ~/bankpoker_backups/bankpoker_$(date +\%Y\%m\%d).db
```

---

## 🌐 Future Step: Cloudflare Tunnel (bankjoker.ir)

Once local Docker testing on port 80 is verified, Cloudflare Tunnel will be installed on the Ubuntu PC to expose the service to the internet at `bankjoker.ir`:

```
Internet (HTTPS: bankjoker.ir)
  │
  ▼
Cloudflare Edge (SSL Termination)
  │
  ▼
cloudflared daemon (running on Ubuntu PC)
  │
  ▼
http://localhost:80 (bankpoker-nginx)
```

No router port forwarding or public static IP is required.
