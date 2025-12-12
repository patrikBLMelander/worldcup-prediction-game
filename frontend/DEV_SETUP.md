# Frontend Development Setup

## Running Frontend in Development Mode

To see changes directly with hot reload, run the frontend locally instead of in Docker.

### Step 1: Stop Frontend Docker Container (Optional)

If you want to run frontend locally while keeping backend in Docker:

```bash
# Stop only the frontend container
docker compose stop frontend

# Or keep it running - you can run both (local frontend will use port 5173)
```

### Step 2: Navigate to Frontend Directory

```bash
cd frontend
```

### Step 3: Install Dependencies (if not already done)

```bash
npm install
```

### Step 4: Start Development Server

```bash
npm run dev
```

The frontend will start on `http://localhost:5173` with hot module replacement (HMR) enabled.

## Development vs Docker

### Development Mode (Local)
- **URL**: http://localhost:5173
- **Hot Reload**: ✅ Yes - changes appear instantly
- **Fast Refresh**: ✅ Yes - React components update without losing state
- **Backend**: Can connect to Docker backend at http://localhost:8080

### Docker Mode (Production-like)
- **URL**: http://localhost:3000
- **Hot Reload**: ❌ No - requires rebuild
- **Fast Refresh**: ❌ No
- **Backend**: Same Docker network

## API Configuration

The frontend is configured to proxy API requests to the backend:

- **Development**: `/api` → `http://localhost:8080` (via Vite proxy)
- **Docker**: `/api` → `http://backend:8080` (via nginx proxy)

When running locally, the Vite dev server automatically proxies `/api` requests to `http://localhost:8080`, so your backend can still be running in Docker.

## Recommended Setup

**Option 1: Frontend Local + Backend Docker** (Recommended for development)
```bash
# Terminal 1: Keep backend in Docker
docker compose up -d postgres backend

# Terminal 2: Run frontend locally
cd frontend
npm run dev
```

**Option 2: Everything in Docker**
```bash
docker compose up -d
# Access at http://localhost:3000
```

## Hot Reload Features

When running `npm run dev`:
- ✅ Changes to `.jsx` files update instantly
- ✅ Changes to `.css` files update instantly
- ✅ React Fast Refresh preserves component state
- ✅ No page refresh needed for most changes
- ✅ Error overlay shows compilation errors

## Troubleshooting

### Port Already in Use
If port 5173 is already in use:
```bash
# Kill process on port 5173
lsof -ti:5173 | xargs kill -9

# Or use a different port
npm run dev -- --port 3001
```

### Backend Not Connecting
- Make sure backend is running: `docker compose ps`
- Check backend health: `curl http://localhost:8080/actuator/health`
- Verify Vite proxy config in `vite.config.js`

### Changes Not Appearing
- Check browser console for errors
- Hard refresh: `Cmd+Shift+R` (Mac) or `Ctrl+Shift+R` (Windows)
- Restart dev server: `Ctrl+C` then `npm run dev`


