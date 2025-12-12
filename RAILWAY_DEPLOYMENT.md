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

1. **Sign in to Railway**:
   - Go to [railway.app](https://railway.app)
   - Sign in with GitHub (recommended)

2. **Create a New Project**:
   - Click "New Project"
   - Select "Deploy from GitHub repo"
   - Choose your repository
   - Railway will auto-detect `docker-compose.yml`

3. **Add PostgreSQL Database**:
   - In your Railway project, click "+ New"
   - Select "Database" → "Add PostgreSQL"
   - Railway will create a PostgreSQL instance
   - **Copy the connection details** (you'll need them)

4. **Configure Environment Variables**:
   - Go to your backend service → "Variables" tab
   - Add the following environment variables:

   ```
   SPRING_PROFILES_ACTIVE=prod
   SPRING_DATASOURCE_URL=<PostgreSQL URL from Railway>
   SPRING_DATASOURCE_USERNAME=<PostgreSQL username>
   SPRING_DATASOURCE_PASSWORD=<PostgreSQL password>
   SPRING_JPA_HIBERNATE_DDL_AUTO=update
   JWT_SECRET=<Generate a secure random string>
   CORS_ALLOWED_ORIGINS=https://your-frontend.railway.app
   FOOTBALL_API_ENABLED=true
   FOOTBALL_API_KEY=<Your Football-Data.org API key>
   FOOTBALL_API_BASE_URL=https://api.football-data.org/v4
   FOOTBALL_API_COMPETITION_ID=2021
   ```
   
   **Important**: Replace `https://your-frontend.railway.app` with your actual frontend Railway domain (you'll get this after deploying the frontend).

   **To generate JWT_SECRET**:
   ```bash
   openssl rand -base64 32
   ```

5. **Update docker-compose.yml for Railway**:
   Railway will automatically use your `docker-compose.yml`, but you may need to:
   - Remove the `postgres` service (Railway provides its own)
   - Update the `SPRING_DATASOURCE_URL` to use Railway's PostgreSQL

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

