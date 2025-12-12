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

## Step 5: Set Up Authentication

GitHub no longer accepts passwords for HTTPS. You have two options:

### Option A: Personal Access Token (PAT) - Recommended

1. **Create a Personal Access Token:**
   - Go to GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
   - Or go directly: [github.com/settings/tokens](https://github.com/settings/tokens)
   - Click **"Generate new token"** → **"Generate new token (classic)"**
   - Give it a name: `World Cup Project`
   - Select scopes: Check **`repo`** (full control of private repositories)
   - Click **"Generate token"**
   - **⚠️ COPY THE TOKEN IMMEDIATELY** (you won't see it again!)

2. **Use the token when pushing:**
   - When Git asks for password, paste your **token** (not your GitHub password)
   - Username: Your GitHub username
   - Password: Your Personal Access Token

### Option B: SSH (No password needed after setup)

1. **Check if you have SSH keys:**
   ```bash
   ls -al ~/.ssh
   ```

2. **Generate SSH key (if you don't have one):**
   ```bash
   ssh-keygen -t ed25519 -C "your_email@example.com"
   # Press Enter to accept default location
   # Press Enter twice for no passphrase (or set one)
   ```

3. **Add SSH key to GitHub:**
   ```bash
   # Copy your public key
   cat ~/.ssh/id_ed25519.pub
   # Copy the entire output
   ```
   - Go to GitHub → Settings → SSH and GPG keys → New SSH key
   - Paste your key and save

4. **Use SSH URL instead of HTTPS:**
   ```bash
   git remote add origin git@github.com:YOUR_USERNAME/worldcup-prediction-game.git
   ```

## Step 6: Connect and Push

```bash
# Add remote (replace YOUR_USERNAME with your GitHub username)
# For HTTPS (use PAT as password):
git remote add origin https://github.com/YOUR_USERNAME/worldcup-prediction-game.git

# OR for SSH (no password needed):
git remote add origin git@github.com:YOUR_USERNAME/worldcup-prediction-game.git

# Rename branch to main (if needed)
git branch -M main

# Push to GitHub
# If using HTTPS, you'll be prompted for:
#   Username: your_github_username
#   Password: your_personal_access_token (NOT your GitHub password!)
git push -u origin main
```

## Step 7: Verify

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

