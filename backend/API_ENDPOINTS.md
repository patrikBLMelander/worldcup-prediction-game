# API Endpoints Documentation

## Base URL
`http://localhost:8080/api`

All protected endpoints require JWT token in header:
```
Authorization: Bearer <your-token>
```

---

## Authentication Endpoints

### Register
**POST** `/auth/register`

Request:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

Response (201 Created):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "user@example.com"
}
```

### Login
**POST** `/auth/login`

Request:
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

Response (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "type": "Bearer",
  "userId": 1,
  "email": "user@example.com"
}
```

---

## Match Endpoints

### Get All Matches
**GET** `/matches`

Query Parameters (optional):
- `status` - Filter by status (SCHEDULED, LIVE, FINISHED, CANCELLED)
- `group` - Filter by group (e.g., "Group A", "Round of 16")

Example: `GET /matches?status=SCHEDULED`

Response (200 OK):
```json
[
  {
    "id": 1,
    "homeTeam": "Brazil",
    "awayTeam": "Argentina",
    "matchDate": "2026-06-15T20:00:00",
    "venue": "Maracanã",
    "group": "Group A",
    "status": "SCHEDULED",
    "homeScore": null,
    "awayScore": null
  }
]
```

### Get Match by ID
**GET** `/matches/{id}`

Response (200 OK):
```json
{
  "id": 1,
  "homeTeam": "Brazil",
  "awayTeam": "Argentina",
  "matchDate": "2026-06-15T20:00:00",
  "venue": "Maracanã",
  "group": "Group A",
  "status": "SCHEDULED",
  "homeScore": null,
  "awayScore": null
}
```

### Create Match
**POST** `/matches`

Request:
```json
{
  "homeTeam": "Brazil",
  "awayTeam": "Argentina",
  "matchDate": "2026-06-15T20:00:00",
  "venue": "Maracanã",
  "group": "Group A"
}
```

Response (201 Created): MatchDTO

### Update Match Result
**PUT** `/matches/{id}/result`

Request:
```json
{
  "homeScore": 2,
  "awayScore": 1
}
```

Response (200 OK): Updated MatchDTO

**Note:** This automatically calculates points for all predictions of this match.

### Update Match Status
**PUT** `/matches/{id}/status?status=LIVE`

Response (200 OK): Updated MatchDTO

---

## Prediction Endpoints

### Create or Update Prediction
**POST** `/predictions`

Request:
```json
{
  "matchId": 1,
  "predictedHomeScore": 2,
  "predictedAwayScore": 1
}
```

Response (201 Created):
```json
{
  "id": 1,
  "matchId": 1,
  "homeTeam": "Brazil",
  "awayTeam": "Argentina",
  "matchDate": "2026-06-15T20:00:00",
  "predictedHomeScore": 2,
  "predictedAwayScore": 1,
  "points": null,
  "createdAt": "2025-12-11T20:00:00",
  "updatedAt": "2025-12-11T20:00:00"
}
```

**Note:** If prediction already exists for this match, it will be updated.

### Get My Predictions
**GET** `/predictions/my-predictions`

Response (200 OK):
```json
[
  {
    "id": 1,
    "matchId": 1,
    "homeTeam": "Brazil",
    "awayTeam": "Argentina",
    "matchDate": "2026-06-15T20:00:00",
    "predictedHomeScore": 2,
    "predictedAwayScore": 1,
    "points": 3,
    "createdAt": "2025-12-11T20:00:00",
    "updatedAt": "2025-12-11T20:00:00"
  }
]
```

### Get My Prediction for Specific Match
**GET** `/predictions/match/{matchId}`

Response (200 OK): PredictionDTO or 404 Not Found

---

## User Endpoints

### Get My Profile
**GET** `/users/me`

Response (200 OK):
```json
{
  "id": 1,
  "email": "user@example.com",
  "totalPoints": 15,
  "predictionCount": 5,
  "createdAt": "2025-12-11T19:00:00"
}
```

### Get Leaderboard
**GET** `/users/leaderboard`

Response (200 OK):
```json
[
  {
    "userId": 1,
    "email": "user1@example.com",
    "totalPoints": 25,
    "predictionCount": 8
  },
  {
    "userId": 2,
    "email": "user2@example.com",
    "totalPoints": 20,
    "predictionCount": 7
  }
]
```

**Note:** Leaderboard is sorted by total points (descending).

---

## Error Responses

### 400 Bad Request
```json
{
  "error": "Error message"
}
```

### 401 Unauthorized
```json
{
  "error": "Invalid email or password"
}
```

### 404 Not Found
```json
{
  "error": "Resource not found"
}
```

### 409 Conflict
```json
{
  "error": "Email already exists"
}
```

---

## Example Workflow

1. **Register/Login** → Get JWT token
2. **Get Matches** → `GET /matches?status=SCHEDULED`
3. **Create Prediction** → `POST /predictions` with matchId and scores
4. **View My Predictions** → `GET /predictions/my-predictions`
5. **Update Match Result** (admin) → `PUT /matches/{id}/result`
6. **View Leaderboard** → `GET /users/leaderboard`
7. **View My Profile** → `GET /users/me`


