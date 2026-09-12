# WorldCup — vilande app (nedsläckt) + databas-backup

> **Läs denna först om du (Claude eller människa) ska väcka appen igen.**

## Status

Den här appen är **helt nedsläckt** mellan mästerskap för att inte kosta något.
Alla Railway-tjänster och databasen är borttagna. Koden och en **full databas-dump**
finns kvar i repot — det är allt som behövs för att få tillbaka appen inför nästa
EM/VM.

- **Nedsläckt:** 2026-09-12
- **Databas-dump:** en full `pg_dump` (schema + all data: användare, ligor, tips,
  achievements, matcher) skapades 2026-09-12 som
  `db-backup/worldcup-db-2026-09-12.sql`.

  ⚠️ **Dumpen ligger INTE i git** (den innehåller PII: e-post, lösenordshashar,
  chatt) — den är `.gitignore`:ad. Den finns bara lokalt på den maskin där den
  skapades och bör kopieras till något durabelt (moln / extern disk). **För att
  väcka appen: skaffa `.sql`-filen (fråga Patrik) och lägg den i den här mappen
  innan du kör restore-steget nedan.**

## Så här väcker du appen igen

Arkitekturen är **en enda tjänst** (Spring Boot-backend + nginx-frontend i samma
container, byggd från `Dockerfile.railway` i repo-roten via `railway.json`) plus
**en Postgres-databas**.

### 1. Skapa en ny Postgres
Antingen Railway Postgres, eller Neon (neon.tech, gratis, skalar till noll).
Notera connection-uppgifterna (host, databasnamn, user, password).

### 2. Återställ datan
```bash
# Kör från repo-roten. Använd den PUBLIKA connection-URL:en till nya databasen.
# (Neon kräver ?sslmode=require i URL:en.)
psql "<NYA_DB_URL>" -f db-backup/worldcup-db-<datum>.sql
```
Restore in i en **tom** databas. Vid Neon: URL:en ser ut som
`postgresql://user:pass@ep-xxx.neon.tech/neondb?sslmode=require`.

### 3. Deploya appen (en tjänst)
I Railway: New Project → Deploy from GitHub repo
(`patrikBLMelander/worldcup-prediction-game`) → **Root Directory tom** (repo-roten)
så att `railway.json` → `Dockerfile.railway` används. Generera en domän efteråt.

### 4. Sätt env-variabler på tjänsten
```
SPRING_PROFILES_ACTIVE=prod
SPRING_JPA_HIBERNATE_DDL_AUTO=update
SPRING_DATASOURCE_URL=jdbc:postgresql://<host>/<db>?sslmode=require
SPRING_DATASOURCE_USERNAME=<user>
SPRING_DATASOURCE_PASSWORD=<password>
JWT_SECRET=<nytt: openssl rand -base64 32>
JAVA_OPTS=-Xms64m -Xmx256m -XX:+UseSerialGC

# Football-data.org (skaffa/rotera nyckel på football-data.org):
FOOTBALL_API_ENABLED=true
FOOTBALL_API_KEY=<din-nyckel>
FOOTBALL_API_BASE_URL=https://api.football-data.org/v4
FOOTBALL_API_COMPETITION_ID=<VM=2000, PL=2021, EM=2018>

# Slå på bakgrundsjobben (de var avstängda i viloläge):
MATCH_STATUS_SCHEDULER_ENABLED=true
LEAGUE_ACHIEVEMENT_SCHEDULER_ENABLED=true
```
Detaljer om intervall och säsongsnollställning finns i `../VILOLAGE_SETUP.md`
(särskilt avsnittet "Steg 4: Väcka appen inför nästa turnering").

### 5. Hämta in spelschemat
När appen är uppe, som admin:
```bash
curl -X POST "https://<din-domän>/api/admin/sync/fixtures" -H "Authorization: Bearer <admin-jwt>"
```

## Att uppdatera dumpen (om appen körs och du vill ta en ny backup)
```bash
pg_dump "<DATABASE_PUBLIC_URL>" --no-owner --no-privileges \
  --format=plain --file=db-backup/worldcup-db-$(date +%F).sql
```
Använd Railways **publika** URL (`DATABASE_PUBLIC_URL`, en `*.proxy.rlwy.net`-host),
inte den interna `postgres.railway.internal` (den fungerar bara inne i Railway).
