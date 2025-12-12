# Railway Backend Setup with PostgreSQL

Based on your Railway PostgreSQL variables, here's how to configure your backend service:

## Backend Environment Variables

After connecting PostgreSQL to your backend service, Railway will automatically add these variables:
- `PGHOST` (Railway private domain)
- `PGPORT` (5432)
- `PGDATABASE` (railway)
- `PGUSER` (postgres)
- `PGPASSWORD` (your password)

## Step-by-Step Backend Configuration

1. **In your Backend Service → Variables tab**, add these variables:

```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://${PGHOST}:${PGPORT}/${PGDATABASE}
SPRING_DATASOURCE_USERNAME=${PGUSER}
SPRING_DATASOURCE_PASSWORD=${PGPASSWORD}
SPRING_JPA_HIBERNATE_DDL_AUTO=update
JWT_SECRET=OuCTQN1DdoYy8y0+L3VGDhEW6fJpa+e1ErMX3P/Viy8=
FOOTBALL_API_ENABLED=true
FOOTBALL_API_KEY=d5a9dfb9de70485b9e3f3678c486f23a
FOOTBALL_API_BASE_URL=https://api.football-data.org/v4
FOOTBALL_API_COMPETITION_ID=2021
```

**Important Notes:**
- Railway will automatically replace `${PGHOST}`, `${PGPORT}`, etc. with actual values
- The `JWT_SECRET` above is a generated secure key (you can regenerate if needed)
- The `FOOTBALL_API_KEY` is your existing key

## Alternative: Use Direct Values (if template variables don't work)

If Railway's template variables don't work, you can use the actual values from your PostgreSQL service:

```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://${RAILWAY_PRIVATE_DOMAIN}:5432/railway
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=wSSnKNPFEfUAcxLCBKHCyGwXeXwWtcHM
SPRING_JPA_HIBERNATE_DDL_AUTO=update
JWT_SECRET=OuCTQN1DdoYy8y0+L3VGDhEW6fJpa+e1ErMX3P/Viy8=
FOOTBALL_API_ENABLED=true
FOOTBALL_API_KEY=d5a9dfb9de70485b9e3f3678c486f23a
FOOTBALL_API_BASE_URL=https://api.football-data.org/v4
FOOTBALL_API_COMPETITION_ID=2021
```

**Note:** Replace `${RAILWAY_PRIVATE_DOMAIN}` with the actual private domain value from your PostgreSQL service variables.

## Verify Connection

After setting these variables:
1. Railway will automatically redeploy your backend
2. Check the logs to see if the database connection succeeds
3. Look for: "Started WorldCupApplication" in the logs

## Next Steps

1. Generate backend domain: Settings → Generate Domain
2. Deploy frontend service (see RAILWAY_QUICK_FIX.md)
3. Update CORS with frontend domain

