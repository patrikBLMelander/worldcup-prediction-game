# Quick Test Guide

## Server Status
The server should be running at: `http://localhost:8080`

Check if it's running:
```bash
curl http://localhost:8080/actuator/health
```

## Quick Test Commands

### 1. Register a User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'
```

### 3. Test Invalid Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"wrongpassword"}'
```
Should return: `401 Unauthorized`

## Run Full Test Suite
```bash
./test-api.sh
```

## Using the Token

After login, you'll get a token. Save it and use it for protected endpoints:
```bash
TOKEN="your-token-here"
curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/matches
```


