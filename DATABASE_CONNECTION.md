# Database Connection Guide

## PostgreSQL Connection Details

Your PostgreSQL database is running in Docker and is accessible from your local machine.

### Connection Information

- **Host**: `localhost` (or `127.0.0.1`)
- **Port**: `5432`
- **Database**: `worldcupdb`
- **Username**: `worldcup`
- **Password**: `worldcup123`

## Connecting with DBeaver

### Step 1: Open DBeaver
Launch DBeaver application.

### Step 2: Create New Connection
1. Click on **"New Database Connection"** (plug icon) or go to **Database → New Database Connection**
2. Select **PostgreSQL** from the list
3. Click **Next**

### Step 3: Enter Connection Details
Fill in the following information:

**Main Tab:**
- **Host**: `localhost`
- **Port**: `5432`
- **Database**: `worldcupdb`
- **Username**: `worldcup`
- **Password**: `worldcup123`
- ✅ Check **"Save password"** if you want DBeaver to remember it

**Driver Properties Tab (optional):**
- You can leave defaults, or set:
  - `sslmode` = `disable` (for local development)

### Step 4: Test Connection
1. Click **"Test Connection"** button
2. If prompted to download PostgreSQL driver, click **"Download"**
3. Wait for "Connected" message
4. Click **"Finish"**

### Step 5: Connect
Double-click on the new connection to connect to the database.

## Quick Connection String

If you prefer using a connection string:
```
jdbc:postgresql://localhost:5432/worldcupdb
```

## Verify Database is Running

Before connecting, make sure the database container is running:

```bash
docker compose ps
```

You should see `worldcup-postgres` with status "Up" and port "5432:5432".

If it's not running:
```bash
docker compose up -d postgres
```

## Accessing Database via Command Line

You can also access the database directly from the command line:

```bash
# Connect to PostgreSQL
docker exec -it worldcup-postgres psql -U worldcup -d worldcupdb

# Once connected, you can run SQL commands:
# \dt          - List all tables
# \d users     - Describe users table
# SELECT * FROM users;
# \q           - Quit
```

## Troubleshooting

### Connection Refused
- Make sure Docker container is running: `docker compose ps`
- Check if port 5432 is already in use: `lsof -i :5432`

### Authentication Failed
- Verify username and password match docker-compose.yml
- Check if database exists: `docker exec -it worldcup-postgres psql -U worldcup -l`

### Can't Find Driver
- DBeaver will prompt to download the PostgreSQL driver automatically
- Or manually download from: https://jdbc.postgresql.org/download/

## Database Tables

Once connected, you should see these tables:
- `users` - User accounts
- `matches` - World Cup matches
- `predictions` - User predictions

## Viewing Data

Example queries:

```sql
-- View all users
SELECT * FROM users;

-- View all matches
SELECT * FROM matches;

-- View all predictions
SELECT * FROM predictions;

-- View predictions with user and match info
SELECT 
    p.id,
    u.email,
    m.home_team,
    m.away_team,
    p.predicted_home_score,
    p.predicted_away_score,
    p.points
FROM predictions p
JOIN users u ON p.user_id = u.id
JOIN matches m ON p.match_id = m.id;
```


