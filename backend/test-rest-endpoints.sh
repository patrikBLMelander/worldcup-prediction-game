#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8080/api"
TOKEN=""

echo -e "${BLUE}=== Testing World Cup REST API Endpoints ===${NC}\n"

# Function to extract token from response
extract_token() {
    echo "$1" | grep -o '"token":"[^"]*' | cut -d'"' -f4
}

# Function to extract ID from response
extract_id() {
    echo "$1" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
}

# Test 1: Register a new user
echo -e "${YELLOW}1. Registering new user...${NC}"
REGISTER_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "password123"
  }')

HTTP_CODE=$(echo "$REGISTER_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$REGISTER_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "201" ]; then
    echo -e "${GREEN}✓ Registration successful!${NC}"
    TOKEN=$(extract_token "$BODY")
    USER_ID=$(extract_id "$BODY")
    echo -e "${BLUE}Token: ${TOKEN:0:30}...${NC}\n"
else
    echo -e "${RED}✗ Registration failed (HTTP $HTTP_CODE)${NC}"
    echo "Response: $BODY"
    exit 1
fi

# Test 2: Login
echo -e "${YELLOW}2. Logging in...${NC}"
LOGIN_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "testuser@example.com",
    "password": "password123"
  }')

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$LOGIN_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Login successful!${NC}"
    TOKEN=$(extract_token "$BODY")
    echo -e "${BLUE}Token: ${TOKEN:0:30}...${NC}\n"
else
    echo -e "${RED}✗ Login failed (HTTP $HTTP_CODE)${NC}"
    exit 1
fi

# Test 3: Get my profile
echo -e "${YELLOW}3. Getting my profile...${NC}"
PROFILE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$BASE_URL/users/me" \
  -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(echo "$PROFILE_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$PROFILE_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Profile retrieved!${NC}"
    echo "Response: $BODY" | head -3
    echo ""
else
    echo -e "${RED}✗ Failed to get profile (HTTP $HTTP_CODE)${NC}"
fi

# Test 4: Create a match
echo -e "${YELLOW}4. Creating a match...${NC}"
MATCH_DATE=$(date -u -v+1d +"%Y-%m-%dT%H:%M:%S" 2>/dev/null || date -u -d "+1 day" +"%Y-%m-%dT%H:%M:%S" 2>/dev/null || echo "2026-06-15T20:00:00")

CREATE_MATCH_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$BASE_URL/matches" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"homeTeam\": \"Brazil\",
    \"awayTeam\": \"Argentina\",
    \"matchDate\": \"$MATCH_DATE\",
    \"venue\": \"Maracanã\",
    \"group\": \"Group A\"
  }")

HTTP_CODE=$(echo "$CREATE_MATCH_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$CREATE_MATCH_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "201" ]; then
    echo -e "${GREEN}✓ Match created!${NC}"
    MATCH_ID=$(extract_id "$BODY")
    echo -e "${BLUE}Match ID: $MATCH_ID${NC}"
    echo "Response: $BODY" | head -5
    echo ""
else
    echo -e "${RED}✗ Failed to create match (HTTP $HTTP_CODE)${NC}"
    echo "Response: $BODY"
    MATCH_ID=""
fi

# Test 5: Get all matches
echo -e "${YELLOW}5. Getting all matches...${NC}"
GET_MATCHES_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$BASE_URL/matches" \
  -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(echo "$GET_MATCHES_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$GET_MATCHES_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Matches retrieved!${NC}"
    MATCH_COUNT=$(echo "$BODY" | grep -o '"id"' | wc -l | tr -d ' ')
    echo -e "${BLUE}Found $MATCH_COUNT match(es)${NC}\n"
else
    echo -e "${RED}✗ Failed to get matches (HTTP $HTTP_CODE)${NC}"
fi

# Test 6: Get match by ID (if we created one)
if [ -n "$MATCH_ID" ]; then
    echo -e "${YELLOW}6. Getting match by ID ($MATCH_ID)...${NC}"
    GET_MATCH_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$BASE_URL/matches/$MATCH_ID" \
      -H "Authorization: Bearer $TOKEN")

    HTTP_CODE=$(echo "$GET_MATCH_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    BODY=$(echo "$GET_MATCH_RESPONSE" | sed '/HTTP_CODE/d')

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✓ Match retrieved!${NC}"
        echo "Response: $BODY" | head -5
        echo ""
    else
        echo -e "${RED}✗ Failed to get match (HTTP $HTTP_CODE)${NC}"
    fi
fi

# Test 7: Create a prediction (if we have a match)
if [ -n "$MATCH_ID" ]; then
    echo -e "${YELLOW}7. Creating a prediction...${NC}"
    CREATE_PREDICTION_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$BASE_URL/predictions" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d "{
        \"matchId\": $MATCH_ID,
        \"predictedHomeScore\": 2,
        \"predictedAwayScore\": 1
      }")

    HTTP_CODE=$(echo "$CREATE_PREDICTION_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    BODY=$(echo "$CREATE_PREDICTION_RESPONSE" | sed '/HTTP_CODE/d')

    if [ "$HTTP_CODE" = "201" ]; then
        echo -e "${GREEN}✓ Prediction created!${NC}"
        PREDICTION_ID=$(extract_id "$BODY")
        echo -e "${BLUE}Prediction ID: $PREDICTION_ID${NC}"
        echo "Response: $BODY" | head -5
        echo ""
    else
        echo -e "${RED}✗ Failed to create prediction (HTTP $HTTP_CODE)${NC}"
        echo "Response: $BODY"
    fi
fi

# Test 8: Get my predictions
echo -e "${YELLOW}8. Getting my predictions...${NC}"
GET_PREDICTIONS_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$BASE_URL/predictions/my-predictions" \
  -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(echo "$GET_PREDICTIONS_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$GET_PREDICTIONS_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Predictions retrieved!${NC}"
    PREDICTION_COUNT=$(echo "$BODY" | grep -o '"id"' | wc -l | tr -d ' ')
    echo -e "${BLUE}Found $PREDICTION_COUNT prediction(s)${NC}\n"
else
    echo -e "${RED}✗ Failed to get predictions (HTTP $HTTP_CODE)${NC}"
fi

# Test 9: Update match result (if we have a match)
if [ -n "$MATCH_ID" ]; then
    echo -e "${YELLOW}9. Updating match result (this will calculate points)...${NC}"
    UPDATE_RESULT_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X PUT "$BASE_URL/matches/$MATCH_ID/result" \
      -H "Authorization: Bearer $TOKEN" \
      -H "Content-Type: application/json" \
      -d '{
        "homeScore": 2,
        "awayScore": 1
      }')

    HTTP_CODE=$(echo "$UPDATE_RESULT_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
    BODY=$(echo "$UPDATE_RESULT_RESPONSE" | sed '/HTTP_CODE/d')

    if [ "$HTTP_CODE" = "200" ]; then
        echo -e "${GREEN}✓ Match result updated!${NC}"
        echo "Response: $BODY" | head -5
        echo ""
    else
        echo -e "${RED}✗ Failed to update match result (HTTP $HTTP_CODE)${NC}"
        echo "Response: $BODY"
    fi
fi

# Test 10: Get my predictions again (to see points)
echo -e "${YELLOW}10. Getting my predictions again (to see calculated points)...${NC}"
GET_PREDICTIONS_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$BASE_URL/predictions/my-predictions" \
  -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(echo "$GET_PREDICTIONS_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$GET_PREDICTIONS_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Predictions retrieved!${NC}"
    if echo "$BODY" | grep -q '"points":3'; then
        echo -e "${GREEN}✓ Points calculated correctly! (3 points for exact match)${NC}"
    fi
    echo "Response: $BODY" | head -8
    echo ""
else
    echo -e "${RED}✗ Failed to get predictions (HTTP $HTTP_CODE)${NC}"
fi

# Test 11: Get my profile again (to see updated points)
echo -e "${YELLOW}11. Getting my profile again (to see updated total points)...${NC}"
PROFILE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$BASE_URL/users/me" \
  -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(echo "$PROFILE_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$PROFILE_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Profile retrieved!${NC}"
    echo "Response: $BODY"
    echo ""
else
    echo -e "${RED}✗ Failed to get profile (HTTP $HTTP_CODE)${NC}"
fi

# Test 12: Get leaderboard
echo -e "${YELLOW}12. Getting leaderboard...${NC}"
LEADERBOARD_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$BASE_URL/users/leaderboard" \
  -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(echo "$LEADERBOARD_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$LEADERBOARD_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Leaderboard retrieved!${NC}"
    echo "Response: $BODY"
    echo ""
else
    echo -e "${RED}✗ Failed to get leaderboard (HTTP $HTTP_CODE)${NC}"
fi

# Test 13: Get matches filtered by status
echo -e "${YELLOW}13. Getting matches filtered by status (FINISHED)...${NC}"
FILTERED_MATCHES_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X GET "$BASE_URL/matches?status=FINISHED" \
  -H "Authorization: Bearer $TOKEN")

HTTP_CODE=$(echo "$FILTERED_MATCHES_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$FILTERED_MATCHES_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Filtered matches retrieved!${NC}"
    MATCH_COUNT=$(echo "$BODY" | grep -o '"id"' | wc -l | tr -d ' ')
    echo -e "${BLUE}Found $MATCH_COUNT finished match(es)${NC}\n"
else
    echo -e "${RED}✗ Failed to get filtered matches (HTTP $HTTP_CODE)${NC}"
fi

echo -e "${BLUE}=== Testing Complete ===${NC}"


