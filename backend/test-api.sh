#!/bin/bash

# Colors for output
GREEN='\033[0;32m'
BLUE='\033[0;34m'
RED='\033[0;31m'
NC='\033[0m' # No Color

BASE_URL="http://localhost:8080/api"

echo -e "${BLUE}=== Testing World Cup API ===${NC}\n"

# Test 1: Register a new user
echo -e "${GREEN}1. Testing User Registration${NC}"
REGISTER_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }')

HTTP_CODE=$(echo "$REGISTER_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$REGISTER_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "201" ]; then
    echo -e "${GREEN}✓ Registration successful!${NC}"
    echo "Response: $BODY"
    TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    echo -e "${BLUE}Token extracted: ${TOKEN:0:50}...${NC}\n"
else
    echo -e "${RED}✗ Registration failed (HTTP $HTTP_CODE)${NC}"
    echo "Response: $BODY"
    TOKEN=""
fi

# Test 2: Try to register the same user again (should fail)
echo -e "${GREEN}2. Testing Duplicate Registration (should fail)${NC}"
DUPLICATE_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }')

HTTP_CODE=$(echo "$DUPLICATE_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" = "409" ]; then
    echo -e "${GREEN}✓ Correctly rejected duplicate email${NC}\n"
else
    echo -e "${RED}✗ Unexpected response (HTTP $HTTP_CODE)${NC}\n"
fi

# Test 3: Login
echo -e "${GREEN}3. Testing User Login${NC}"
LOGIN_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }')

HTTP_CODE=$(echo "$LOGIN_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
BODY=$(echo "$LOGIN_RESPONSE" | sed '/HTTP_CODE/d')

if [ "$HTTP_CODE" = "200" ]; then
    echo -e "${GREEN}✓ Login successful!${NC}"
    echo "Response: $BODY"
    TOKEN=$(echo "$BODY" | grep -o '"token":"[^"]*' | cut -d'"' -f4)
    echo -e "${BLUE}Token extracted: ${TOKEN:0:50}...${NC}\n"
else
    echo -e "${RED}✗ Login failed (HTTP $HTTP_CODE)${NC}"
    echo "Response: $BODY"
    TOKEN=""
fi

# Test 4: Test invalid login
echo -e "${GREEN}4. Testing Invalid Login (should fail)${NC}"
INVALID_LOGIN=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$BASE_URL/auth/login" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "wrongpassword"
  }')

HTTP_CODE=$(echo "$INVALID_LOGIN" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" = "401" ] || [ "$HTTP_CODE" = "403" ]; then
    echo -e "${GREEN}✓ Correctly rejected invalid credentials${NC}\n"
else
    echo -e "${RED}✗ Unexpected response (HTTP $HTTP_CODE)${NC}\n"
fi

# Test 5: Test validation errors
echo -e "${GREEN}5. Testing Validation (should fail)${NC}"
VALIDATION_RESPONSE=$(curl -s -w "\nHTTP_CODE:%{http_code}" -X POST "$BASE_URL/auth/register" \
  -H "Content-Type: application/json" \
  -d '{
    "email": "invalid-email",
    "password": "123"
  }')

HTTP_CODE=$(echo "$VALIDATION_RESPONSE" | grep "HTTP_CODE" | cut -d: -f2)
if [ "$HTTP_CODE" = "400" ]; then
    echo -e "${GREEN}✓ Correctly rejected invalid input${NC}"
    echo "Response: $(echo "$VALIDATION_RESPONSE" | sed '/HTTP_CODE/d')"
else
    echo -e "${RED}✗ Unexpected response (HTTP $HTTP_CODE)${NC}"
fi

echo -e "\n${BLUE}=== Testing Complete ===${NC}"


