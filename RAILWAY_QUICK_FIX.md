# Railway Quick Fix - Deploy Services Separately

The error you're seeing is because Railway is trying to use Railpack on the root directory. Railway doesn't support docker-compose directly - we need to deploy each service separately.

## Quick Steps to Fix

### 1. Delete the Current Failing Service
- In Railway, go to your project
- Click on the service that's failing
- Go to "Settings" → Scroll down → "Delete Service"
- Confirm deletion

### 2. Add PostgreSQL Database
- Click "+ New" → "Database" → "Add PostgreSQL"
- Wait for it to provision
- Click on it → "Variables" tab
- **Copy the connection details** (you'll need them)

### 3. Deploy Backend Service

1. **Add Service**:
   - Click "+ New" → "GitHub Repo"
   - Select your repository
   - Railway will create a service

2. **Configure Service**:
   - Click on the new service
   - Go to "Settings" tab
   - **Root Directory**: Set to `backend`
   - Railway should auto-detect the Dockerfile

3. **Set Environment Variables**:
   - Go to "Variables" tab
   - Click "Connect" → "PostgreSQL" (this auto-adds DB vars)
   - Railway will add variables like `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD`
   - Then add these Spring Boot variables:
     ```
     SPRING_PROFILES_ACTIVE=prod
     SPRING_DATASOURCE_URL=jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}
     SPRING_DATASOURCE_USERNAME=${PGUSER}
     SPRING_DATASOURCE_PASSWORD=${PGPASSWORD}
     SPRING_JPA_HIBERNATE_DDL_AUTO=update
     JWT_SECRET=<generate with: openssl rand -base64 32>
     FOOTBALL_API_ENABLED=true
     FOOTBALL_API_KEY=<your-api-key>
     FOOTBALL_API_BASE_URL=https://api.football-data.org/v4
     FOOTBALL_API_COMPETITION_ID=2021
     ```
   
   **See `RAILWAY_BACKEND_SETUP.md` for detailed configuration with your PostgreSQL variables.**

4. **Generate Domain**:
   - "Settings" → "Generate Domain"
   - Copy the domain (e.g., `https://your-backend.railway.app`)

### 4. Deploy Frontend Service

1. **Add Service**:
   - Click "+ New" → "GitHub Repo"
   - Select the same repository

2. **Configure Service**:
   - "Settings" tab
   - **Root Directory**: Set to `frontend`
   - Railway should auto-detect the Dockerfile

3. **Set Environment Variables**:
   - "Variables" tab
   - Add (replace with your backend domain):
     ```
     VITE_API_BASE_URL=https://your-backend.railway.app/api
     VITE_WS_BASE_URL=https://your-backend.railway.app
     ```

4. **Generate Domain**:
   - "Settings" → "Generate Domain"
   - Copy the domain

5. **Update Backend CORS**:
   - Go back to backend service → "Variables"
   - Add:
     ```
     CORS_ALLOWED_ORIGINS=https://your-frontend.railway.app
     ```
   - This will trigger a redeploy

## Important Notes

- **Root Directory is key**: Railway needs to know which folder contains the Dockerfile
- **Environment variables**: Set them BEFORE the first deploy
- **Backend domain first**: You need the backend domain before setting frontend vars
- **CORS**: Update backend CORS after you have the frontend domain

## Troubleshooting

**If build still fails:**
- Check that "Root Directory" is set correctly (`backend` or `frontend`)
- Verify Dockerfile exists in that directory
- Check Railway logs for specific errors

**If frontend can't connect to backend:**
- Verify `VITE_API_BASE_URL` is set correctly
- Check backend CORS settings include frontend domain
- Make sure backend is deployed and healthy

