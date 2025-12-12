# World Cup 2026 Prediction Game

A web application where friends can sign up and predict football World Cup 2026 game results.

## Project Structure

```
.
├── backend/          # Java Spring Boot backend
└── frontend/         # React frontend
```

## Tech Stack

### Backend
- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Security** - Authentication & Authorization
- **Spring Data JPA** - Database access
- **H2 Database** - Development database
- **PostgreSQL** - Production database
- **JWT** - Token-based authentication

### Frontend
- **React 19**
- **Vite** - Build tool and dev server
- **React Router** - Routing
- **Axios** - HTTP client

## Getting Started

### Prerequisites

**For Docker (Recommended):**
- Docker 20.10+
- Docker Compose 2.0+

**For Local Development:**
- Java 17 or higher
- Maven 3.6+
- Node.js 18+ and npm

### Backend Setup

1. Navigate to the backend directory:
```bash
cd backend
```

2. Build and run the application:
```bash
mvn spring-boot:run
```

The backend will start on `http://localhost:8080`

### Frontend Setup

1. Navigate to the frontend directory:
```bash
cd frontend
```

2. Install dependencies (if not already done):
```bash
npm install
```

3. Start the development server:
```bash
npm run dev
```

The frontend will start on `http://localhost:5173`

## Development

- Backend API: `http://localhost:8080`
- Frontend Dev Server: `http://localhost:5173`
- H2 Console (dev): `http://localhost:8080/h2-console`

The frontend is configured to proxy API requests to the backend during development.

## Docker Setup

### Running with Docker Compose (Recommended)

The easiest way to run the entire application is using Docker Compose:

1. **Build and start all services:**
```bash
docker-compose up --build
```

2. **Run in detached mode:**
```bash
docker-compose up -d --build
```

3. **View logs:**
```bash
docker-compose logs -f
```

4. **Stop all services:**
```bash
docker-compose down
```

5. **Stop and remove volumes (clean database):**
```bash
docker-compose down -v
```

### Services

When running with Docker Compose, the following services are available:

- **Frontend**: `http://localhost:3000`
- **Backend API**: `http://localhost:8080`
- **PostgreSQL Database**: `localhost:5432`
  - Database: `worldcupdb`
  - Username: `worldcup`
  - Password: `worldcup123`

### Environment Variables

Create a `.env` file in the root directory to customize settings:

```env
JWT_SECRET=your-secret-key-here
```

### Building Individual Services

**Backend only:**
```bash
cd backend
docker build -t worldcup-backend .
docker run -p 8080:8080 worldcup-backend
```

**Frontend only:**
```bash
cd frontend
docker build -t worldcup-frontend .
docker run -p 3000:80 worldcup-frontend
```

## Next Steps

- [ ] Implement user authentication (sign up, login)
- [ ] Create user model and repository
- [ ] Set up database schema for matches and predictions
- [ ] Create API endpoints for matches and predictions
- [ ] Build React components for authentication
- [ ] Build React components for match predictions
- [ ] Add World Cup 2026 match data

## License

Private project

