# Testing the World Cup API

## Quick Start

1. **Start the server:**
```bash
cd backend
mvn spring-boot:run
```

2. **Wait for server to start** (you'll see "Started WorldCupApplication" in the logs)

3. **Run the test script:**
```bash
./test-api.sh
```

## Manual Testing with curl

### 1. Register a new user
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**Expected Response (201 Created):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "user@example.com"
}
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**Expected Response (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "user@example.com"
}
```

### 3. Test with JWT Token (for protected endpoints)

Save the token from the login response, then use it:
```bash
TOKEN="your-token-here"

curl -X GET http://localhost:8080/api/matches \
  -H "Authorization: Bearer $TOKEN"
```

## Testing Validation

### Test invalid email
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "invalid-email",
    "password": "password123"
  }'
```
**Expected:** 400 Bad Request with validation errors

### Test short password
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "123"
  }'
```
**Expected:** 400 Bad Request with validation errors

### Test duplicate email
```bash
# Try to register the same email twice
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```
**Expected:** 409 Conflict

## Using Postman or Insomnia

1. **Base URL:** `http://localhost:8080/api`

2. **Register Endpoint:**
   - Method: POST
   - URL: `http://localhost:8080/api/auth/register`
   - Headers: `Content-Type: application/json`
   - Body (JSON):
     ```json
     {
       "email": "user@example.com",
       "password": "password123"
     }
     ```

3. **Login Endpoint:**
   - Method: POST
   - URL: `http://localhost:8080/api/auth/login`
   - Headers: `Content-Type: application/json`
   - Body (JSON):
     ```json
     {
       "email": "user@example.com",
       "password": "password123"
     }
     ```

4. **For Protected Endpoints:**
   - Add Header: `Authorization: Bearer <your-token>`

## Health Check

Test if the server is running:
```bash
curl http://localhost:8080/actuator/health
```

**Expected Response:**
```json
{"status":"UP"}
```

## H2 Database Console (Development)

Access the H2 console at: `http://localhost:8080/h2-console`

- JDBC URL: `jdbc:h2:mem:worldcupdb`
- Username: `sa`
- Password: (leave empty)


