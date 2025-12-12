# Quick GitHub Setup Guide

## Step 1: Initialize Git (if not already done)

```bash
# Check if git is initialized
git status

# If not initialized, run:
git init
```

## Step 2: Add All Files

```bash
git add .
```

## Step 3: Create Initial Commit

```bash
git commit -m "Initial commit: World Cup Prediction Game"
```

## Step 4: Create GitHub Repository

1. Go to [github.com](https://github.com)
2. Click the **"+"** icon → **"New repository"**
3. Repository name: `worldcup-prediction-game` (or your preferred name)
4. Description: "A web application for predicting Premier League match results"
5. **Visibility**: Choose Public or Private
6. **DO NOT** check "Initialize with README" (you already have files)
7. Click **"Create repository"**

## Step 5: Connect and Push

```bash
# Add remote (replace YOUR_USERNAME with your GitHub username)
git remote add origin https://github.com/YOUR_USERNAME/worldcup-prediction-game.git

# Rename branch to main (if needed)
git branch -M main

# Push to GitHub
git push -u origin main
```

## Step 6: Verify

1. Go to your GitHub repository page
2. You should see all your files
3. Check that sensitive files are NOT there:
   - ❌ No `.env` files
   - ❌ No API keys in `application.properties`
   - ❌ No `node_modules/` folder
   - ❌ No `target/` folder

## Important Notes

✅ **Safe to commit:**
- Source code
- Configuration files (without secrets)
- Docker files
- Documentation

❌ **Never commit:**
- `.env` files
- API keys or secrets
- `node_modules/`
- `target/` (build artifacts)
- `.idea/` or `.vscode/` (IDE settings)

## Next Steps

After pushing to GitHub, follow the [Railway Deployment Guide](./RAILWAY_DEPLOYMENT.md) to deploy your app!

