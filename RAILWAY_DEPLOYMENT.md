# Railway Deployment Guide

This guide will help you deploy the World Cup Prediction Game to Railway for free.

## Prerequisites

1. **GitHub Account** - Free account works fine
2. **Railway Account** - Sign up at [railway.app](https://railway.app) (free with $5/month credit)
3. **Football-Data.org API Key** - Get one at [football-data.org](https://www.football-data.org/) (free tier available)

## Step 1: Push to GitHub

1. **Initialize Git** (if not already done):
   ```bash
   git init
   git add .
   git commit -m "Initial commit"
   ```

2. **Create a new repository on GitHub**:
   - Go to [github.com](https://github.com)
   - Click "New repository"
   - Name it (e.g., `worldcup-prediction-game`)
   - Don't initialize with README (you already have one)
   - Click "Create repository"

3. **Push your code**:
   ```bash
   git remote add origin https://github.com/YOUR_USERNAME/YOUR_REPO_NAME.git
   git branch -M main
   git push -u origin main
   ```

## Step 2: Deploy to Railway

Railway doesn't support docker-compose directly. We'll deploy each service separately.

### Step 2.1: Create Project and Add PostgreSQL

1. **Sign in to Railway**:
   - Go to [railway.app](https://railway.app)
   - Sign in with GitHub (recommended)

2. **Create a New Project**:
   - Click "New Project"
   - Select "Empty Project"

3. **Add PostgreSQL Database**:
   - In your Railway project, click "+ New"
   - Select "Database" → "Add PostgreSQL"
   - Railway will create a PostgreSQL instance
   - Click on the PostgreSQL service → "Variables" tab
   - **Copy these values** (you'll need them):
     - `PGHOST` (hostname)
     - `PGPORT` (port, usually 5432)
     - `PGDATABASE` (database name)
     - `PGUSER` (username)
     - `PGPASSWORD` (password)
   - Or copy the `DATABASE_URL` connection string

### Step 2.2: Deploy Backend Service

1. **Add Backend Service**:
   - In your Railway project, click "+ New"
   - Select "GitHub Repo"
   - Choose your repository
   - Railway will try to detect the service - **we need to configure it manually**

2. **Configure Backend for Docker**:
   - Click on the newly created service
   - Go to "Settings" tab
   - Under "Build Command", leave it empty (Docker handles this)
   - Under "Root Directory", set to: `backend`
   - Under "Dockerfile Path", set to: `Dockerfile` (it's in the backend folder)
   - Railway should detect it's a Dockerfile build

3. **Set Backend Environment Variables**:
   - Go to "Variables" tab
   - Add these variables (use the PostgreSQL values from Step 2.1):

   ```
   SPRING_PROFILES_ACTIVE=prod
   SPRING_DATASOURCE_URL=jdbc:postgresql://PGHOST:PGPORT/PGDATABASE
   SPRING_DATASOURCE_USERNAME=PGUSER
   SPRING_DATASOURCE_PASSWORD=PGPASSWORD
   SPRING_JPA_HIBERNATE_DDL_AUTO=update
   JWT_SECRET=<Generate a secure random string>
   FOOTBALL_API_ENABLED=true
   FOOTBALL_API_KEY=<Your Football-Data.org API key>
   FOOTBALL_API_BASE_URL=https://api.football-data.org/v4
   FOOTBALL_API_COMPETITION_ID=2021
   ```
   
   **Important**: Replace `PGHOST`, `PGPORT`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` with the actual values from your PostgreSQL service.
   
   **To generate JWT_SECRET**:
   ```bash
   openssl rand -base64 32
   ```

4. **Link PostgreSQL to Backend**:
   - In the backend service, go to "Settings" → "Connect" → "PostgreSQL"
   - This will automatically add the database connection variables

5. **Generate Backend Domain**:
   - Go to backend service → "Settings" → "Generate Domain"
   - Copy the domain (e.g., `https://your-backend.railway.app`)

### Step 2.3: Deploy Frontend Service

1. **Add Frontend Service**:
   - In your Railway project, click "+ New"
   - Select "GitHub Repo"
   - Choose the same repository
   - Railway will create a new service

2. **Configure Frontend for Docker**:
   - Click on the frontend service
   - Go to "Settings" tab
   - Under "Root Directory", set to: `frontend`
   - Under "Dockerfile Path", set to: `Dockerfile` (it's in the frontend folder)

3. **Set Frontend Environment Variables**:
   - Go to "Variables" tab
   - Add this variable (use your backend domain from Step 2.2):

   ```
   VITE_API_BASE_URL=https://your-backend.railway.app/api
   VITE_WS_BASE_URL=https://your-backend.railway.app
   ```
   
   **Important**: Replace `https://your-backend.railway.app` with your actual backend Railway domain.
   
   **Note**: The frontend needs to know the backend URL at build time, so make sure to set these before Railway builds the frontend.

4. **Generate Frontend Domain**:
   - Go to frontend service → "Settings" → "Generate Domain"
   - Copy the domain (e.g., `https://your-frontend.railway.app`)

5. **Update Backend CORS**:
   - Go back to backend service → "Variables"
   - Add or update:
   ```
   CORS_ALLOWED_ORIGINS=https://your-frontend.railway.app
   ```
   - Replace with your actual frontend domain

   **Alternative**: Railway can deploy services individually. You can:
   - Deploy backend as one service
   - Deploy frontend as another service
   - Use Railway's PostgreSQL addon

6. **Deploy**:
   - Railway will automatically start building and deploying
   - Watch the logs in the Railway dashboard
   - Wait for "Deployment successful"

7. **Generate Domain**:
   - Go to your frontend service → "Settings" → "Generate Domain"
   - Railway will give you a free `.railway.app` domain
   - You can also add a custom domain later

## Step 3: Update Frontend API URL

After deployment, you need to update the frontend to point to your Railway backend URL.

1. **Find your backend URL**:
   - In Railway, go to your backend service
   - Click "Settings" → "Generate Domain"
   - Copy the URL (e.g., `https://your-backend.railway.app`)

2. **Update frontend configuration**:
   - Edit `frontend/src/config/api.js`
   - Update the `API_BASE_URL` to your Railway backend URL
   - Commit and push to GitHub
   - Railway will auto-redeploy

## Step 4: Verify Deployment

1. **Check backend health**:
   - Visit `https://your-backend.railway.app/actuator/health`
   - Should return `{"status":"UP"}`

2. **Check frontend**:
   - Visit your frontend Railway domain
   - Should see the login page

3. **Test the app**:
   - Register a new user
   - Make predictions
   - Check if matches are loading

## Troubleshooting

### Backend won't start
- Check Railway logs for errors
- Verify all environment variables are set
- Ensure PostgreSQL connection string is correct

### Frontend can't connect to backend
- Check CORS settings in `SecurityConfig.java`
- Verify backend URL in `frontend/src/config/api.js`
- Check Railway logs for CORS errors

### Database connection errors
- Verify PostgreSQL service is running in Railway
- Check `SPRING_DATASOURCE_URL` matches Railway's PostgreSQL URL
- Ensure database credentials are correct

### WebSocket not working
- Railway supports WebSockets, but verify your Nginx config
- Check that WebSocket endpoints are properly configured

## Railway Free Tier Limits

- **$5/month credit** - Usually enough for small apps
- **500 hours/month** - Shared compute time
- **5GB storage** - For PostgreSQL
- **100GB bandwidth** - Monthly transfer

## Cost Optimization

- Use Railway's PostgreSQL (included in free tier)
- Monitor usage in Railway dashboard
- Set up usage alerts
- Consider upgrading if you exceed limits

## Next Steps

- Add custom domain (optional)
- Set up monitoring
- Configure backups
- Add CI/CD workflows

## Support

- Railway Docs: [docs.railway.app](https://docs.railway.app)
- Railway Discord: [discord.gg/railway](https://discord.gg/railway)

