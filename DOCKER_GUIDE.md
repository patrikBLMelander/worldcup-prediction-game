# Docker Guide - World Cup 2026 Application

## Quick Start

### Start All Services
```bash
./start-docker.sh
```

Or manually:
```bash
docker compose up --build -d
```

### Stop All Services
```bash
docker compose down
```

### Stop and Remove Volumes (Clean Database)
```bash
docker compose down -v
```

## Services

When running with Docker, the application is available at:

- **Frontend**: http://localhost:3000
- **Backend API**: http://localhost:8080
- **PostgreSQL Database**: localhost:5432
  - Database: `worldcupdb`
  - Username: `worldcup`
  - Password: `worldcup123`

## Useful Commands

### View Logs
```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres
```

### Check Status
```bash
docker compose ps
```

### Restart a Service
```bash
docker compose restart backend
docker compose restart frontend
```

### Rebuild After Code Changes
```bash
# Rebuild and restart
docker compose up --build -d

# Rebuild specific service
docker compose up --build -d backend
```

### Access Database
```bash
# Connect to PostgreSQL
docker exec -it worldcup-postgres psql -U worldcup -d worldcupdb
```

### Access Container Shell
```bash
# Backend container
docker exec -it worldcup-backend sh

# Frontend container
docker exec -it worldcup-frontend sh
```

## Environment Variables

Create a `.env` file in the root directory to customize:

```env
JWT_SECRET=your-secret-key-here
```

## Troubleshooting

### Services Not Starting
1. Check logs: `docker compose logs`
2. Check if ports are already in use
3. Ensure Docker has enough resources allocated

### Database Connection Issues
- Wait for PostgreSQL to be healthy (check with `docker compose ps`)
- Backend waits for database to be ready automatically

### Frontend Not Loading
- Check if backend is running: `curl http://localhost:8080/actuator/health`
- Check frontend logs: `docker compose logs frontend`

### Rebuild Everything
```bash
docker compose down -v
docker compose build --no-cache
docker compose up -d
```

## Development vs Production

### Development
- Use `npm run dev` in frontend for hot reload
- Use `mvn spring-boot:run` in backend for hot reload
- Database: H2 (in-memory) or PostgreSQL

### Production (Docker)
- Frontend: Built and served via Nginx
- Backend: JAR file in container
- Database: PostgreSQL with persistent volume


