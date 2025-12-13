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

**The Issue:** Railway's template variables `${PGHOST}` don't work in Spring Boot. We need to use Railway's reference syntax or direct values.

### Option 1: Use Railway's Reference Syntax (Recommended)

1. **In your Backend Service → Variables tab**, after connecting PostgreSQL, add these variables:

```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
SPRING_DATASOURCE_USERNAME=${{Postgres.PGUSER}}
SPRING_DATASOURCE_PASSWORD=${{Postgres.PGPASSWORD}}
SPRING_JPA_HIBERNATE_DDL_AUTO=update
JWT_SECRET=OuCTQN1DdoYy8y0+L3VGDhEW6fJpa+e1ErMX3P/Viy8=
FOOTBALL_API_ENABLED=true
FOOTBALL_API_KEY=d5a9dfb9de70485b9e3f3678c486f23a
FOOTBALL_API_BASE_URL=https://api.football-data.org/v4
FOOTBALL_API_COMPETITION_ID=2021
```

**Note:** Railway uses `${{ServiceName.VariableName}}` syntax. Replace `Postgres` with your actual PostgreSQL service name if different.

### Option 2: Use Direct Values (If Option 1 doesn't work)

If Railway's reference syntax doesn't work, use the actual values from your PostgreSQL service. Go to your PostgreSQL service → Variables tab and copy the actual values:

```
SPRING_PROFILES_ACTIVE=prod
SPRING_DATASOURCE_URL=jdbc:postgresql://<RAILWAY_PRIVATE_DOMAIN_VALUE>:5432/railway
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=wSSnKNPFEfUAcxLCBKHCyGwXeXwWtcHM
SPRING_JPA_HIBERNATE_DDL_AUTO=update
JWT_SECRET=OuCTQN1DdoYy8y0+L3VGDhEW6fJpa+e1ErMX3P/Viy8=
FOOTBALL_API_ENABLED=true
FOOTBALL_API_KEY=d5a9dfb9de70485b9e3f3678c486f23a
FOOTBALL_API_BASE_URL=https://api.football-data.org/v4
FOOTBALL_API_COMPETITION_ID=2021
```

**To find RAILWAY_PRIVATE_DOMAIN:**
- Go to your PostgreSQL service → Variables tab
- Look for `PGHOST` or `RAILWAY_PRIVATE_DOMAIN`
- Copy the actual value (it will be something like `postgres.railway.internal` or similar)

## Verify Connection

After setting these variables:
1. Railway will automatically redeploy your backend
2. Check the logs to see if the database connection succeeds
3. Look for: "Started WorldCupApplication" in the logs

## Next Steps

1. Generate backend domain: Settings → Generate Domain
2. Deploy frontend service (see RAILWAY_QUICK_FIX.md)
3. Update CORS with frontend domain

