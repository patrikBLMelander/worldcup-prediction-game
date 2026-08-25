# Viloläge/Konserveringsläge för Produktion

Detta dokument beskriver hur man sätter appen i viloläge för att minska externa API-anrop och resursanvändning i produktion, samtidigt som appen fortfarande fungerar normalt.

## Översikt

I viloläge minskas frekvensen för:
- Externa API-anrop till Football-Data.org
- Match status-uppdateringar
- Live score-synkronisering

Appen fungerar fortfarande normalt, men uppdaterar data mindre ofta.

## Konfiguration

### Standardinställningar (Normal drift)

| Scheduler | Standard intervall | Beskrivning |
|-----------|-------------------|-------------|
| Fixtures Sync | 1 timme (3600000 ms) | Hämtar kommande matcher |
| Live Scores | 60 sekunder (60000 ms) | Uppdaterar live-resultat |
| Finished Matches | 5 minuter (300000 ms) | Uppdaterar avslutade matcher |
| Match Status | 30 sekunder (30000 ms) | Uppdaterar match-status internt |

### Viloläge-inställningar (Reducerad frekvens)

| Scheduler | Viloläge intervall | Beskrivning |
|-----------|-------------------|-------------|
| Fixtures Sync | 6 timmar (21600000 ms) | Hämtar kommande matcher |
| Live Scores | 5 minuter (300000 ms) | Uppdaterar live-resultat |
| Finished Matches | 30 minuter (1800000 ms) | Uppdaterar avslutade matcher |
| Match Status | 2 minuter (120000 ms) | Uppdaterar match-status internt |

## Så här aktiverar du viloläge

### Alternativ 1: Via Environment Variables (Rekommenderat)

Sätt följande environment variables i din produktionsmiljö:

```bash
# Extern API-synkronisering (viloläge)
FOOTBALL_API_SYNC_FIXTURES_INTERVAL=21600000    # 6 timmar
FOOTBALL_API_SYNC_LIVE_INTERVAL=300000         # 5 minuter
FOOTBALL_API_SYNC_FINISHED_INTERVAL=1800000    # 30 minuter

# Intern match-status uppdatering
MATCH_STATUS_UPDATE_INTERVAL=120000            # 2 minuter
```

### Alternativ 2: Via application-prod.properties

Redigera `backend/src/main/resources/application-prod.properties`:

```properties
# Scheduler Intervals (in milliseconds) - Viloläge
football.api.sync.fixtures.interval=21600000
football.api.sync.live.interval=300000
football.api.sync.finished.interval=1800000
match.status.update.interval=120000
```

### Alternativ 3: Temporärt stänga av API-synkronisering

Om du vill stänga av alla externa API-anrop helt:

```bash
FOOTBALL_API_ENABLED=false
```

**OBS:** Om API-synkronisering är avstängd kommer matcher inte att uppdateras automatiskt. Du kan fortfarande manuellt uppdatera matcher via admin-gränssnittet.

## Effekter av viloläge

### Positiva effekter
- ✅ Minskat antal externa API-anrop (sparar API-quota)
- ✅ Lägre resursanvändning (CPU, minne, nätverk)
- ✅ Minskad risk för rate limiting från API-leverantören
- ✅ Lägre kostnader om du betalar per API-anrop

### Vad som fortfarande fungerar
- ✅ Alla användarfunktioner fungerar normalt
- ✅ Matcher och resultat uppdateras (men mindre ofta)
- ✅ Live-resultat uppdateras (var 5:e minut istället för varje minut)
- ✅ Poängberäkningar fungerar normalt
- ✅ Leaderboards och ligor fungerar normalt

### Vad som kan påverkas
- ⚠️ Live-resultat kan vara några minuter gamla (max 5 minuter)
- ⚠️ Nya matcher synkroniseras var 6:e timme istället för varje timme
- ⚠️ Avslutade matcher kan ta upp till 30 minuter att uppdateras

## Ytterligare optimeringar för viloläge

### 1. Minska loggningsnivåer

I `application-prod.properties`:

```properties
# Minska loggningsnivåer för mindre diskutrymme
logging.level.root=WARN
logging.level.com.worldcup=INFO
logging.level.com.worldcup.config.FootballApiSyncScheduler=WARN
logging.level.com.worldcup.config.MatchStatusScheduler=WARN
```

### 2. Optimera databasanslutningar

Kontrollera att connection pool är korrekt konfigurerad:

```properties
# HikariCP Connection Pool - minska för viloläge
spring.datasource.hikari.maximum-pool-size=5
spring.datasource.hikari.minimum-idle=2
```

### 3. Stäng av onödiga features

Om du inte använder vissa features, kan du stänga av dem:

```properties
# Stäng av H2 console i produktion (om den är aktiverad)
spring.h2.console.enabled=false
```

## Återställa till normal drift

För att återställa till normal drift, ta bort environment variables eller ändra tillbaka värdena i `application-prod.properties`:

```properties
# Normal drift
football.api.sync.fixtures.interval=3600000
football.api.sync.live.interval=60000
football.api.sync.finished.interval=300000
match.status.update.interval=30000
```

## Efter turneringen: fullt viloläge + arkivering

När ett mästerskap är avslutat vill man vanligtvis två saker: minska kostnaderna
så långt det går utan att ta ner appen, och lägga undan ligorna så att nästa
turnering startar från ett rent blad. Appen är fortfarande online och alla
achievements, placeringar och gamla tips finns kvar – ingenting raderas.

### Steg 1: Sätt Railway-variabler (backend-tjänsten)

```bash
# Inga externa API-anrop alls
FOOTBALL_API_ENABLED=false

# Stäng av bakgrundsjobben helt (ingen periodisk databastrafik)
MATCH_STATUS_SCHEDULER_ENABLED=false
LEAGUE_ACHIEVEMENT_SCHEDULER_ENABLED=false

# Mindre loggvolym
LOG_LEVEL_ROOT=WARN
LOG_LEVEL_APP=INFO
LOG_LEVEL_SCHEDULING=WARN

# Mindre minne och färre databasanslutningar
JAVA_OPTS=-Xms128m -Xmx256m -XX:+UseSerialGC
DB_POOL_MAX_SIZE=3
DB_POOL_MIN_IDLE=1

# Nollställ den globala topplistan för nästa säsong (se steg 3)
APP_SEASON_START=2026-09-01T00:00
```

`JAVA_OPTS` kräver en ny deploy (den läses av `ENTRYPOINT` i
`backend/Dockerfile`); övriga variabler räcker det med en omstart för.

Effekt: JVM:en tar ~256 MB istället för att växa mot containergränsen,
schedulers gör inga databasanrop när ingen är inne, och loggarna innehåller
bara det som betyder något. Frontend-tjänsten (nginx med statiska filer) är
redan billig och behöver inget.

**Detta kostar fortfarande pengar:** Postgres-volymen (lagring debiteras även
när ingen använder appen) och den lilla baskostnaden för två tjänster som är
uppe dygnet runt. Vill du ner till nästan noll får du stoppa
backend-/frontend-tjänsterna helt och bara behålla databasen – då är appen
otillgänglig tills du deployar igen.

### Steg 2: Arkivera alla ligor

```bash
curl -X POST "https://<backend>/api/admin/leagues/archive-all?confirm=YES_ARCHIVE_LEAGUES" \
  -H "Authorization: Bearer <admin-jwt>"
```

Svar: `{"leaguesArchived": N, "achievementsProcessedFirst": true}`

Vad som händer:

1. Alla avslutade ligor som ännu inte fått sina leaderboard-achievements
   utdelade behandlas först, så slutplaceringarna hinner delas ut innan ligorna
   försvinner. (Hoppa över med `&awardAchievementsFirst=false`.)
2. Varje liga sätts till `hidden = TRUE` – samma mjuka borttagning som en
   ligaägare gör från UI:t.

Vad som bevaras: medlemskap, chatthistorik, tips, poäng, utdelade achievements
och placeringar. Ligorna slutar bara dyka upp i `/leagues/mine`, på
leaderboard-flikarna och i chatt-widgeten.

Så här ångrar du (en enskild liga eller alla):

```sql
UPDATE leagues SET hidden = FALSE WHERE id = <id>;
```

Obs: `POST /api/admin/cleanup-test-data` gömmer också ligor, men den raderar
matcher, tips, achievements och notifikationer på vägen. Använd
`archive-all` när det bara är ligorna som ska bort.

### Steg 3: Nollställ den globala topplistan

Den globala topplistan (fliken 🌍 Global) är en summa av alla tips någonsin, så
utan gräns följer förra turneringens poäng med in i nästa. `APP_SEASON_START`
(ISO-datumtid, t.ex. `2026-09-01T00:00`) gör att topplistan bara räknar matcher
som startar den tidpunkten eller senare.

- Inget raderas – äldre tips ligger kvar i databasen.
- Profilsidans totalpoäng är fortsatt "all time" (karriärsiffra), liksom
  achievements och placeringar.
- Tom variabel = gamla beteendet (räkna allt).

Sätt den till strax efter att förra turneringen tog slut, och flytta den framåt
när nästa säsong ska nollställas.

### Steg 4: Väcka appen inför nästa turnering

1. `FOOTBALL_API_COMPETITION_ID` till rätt tävling (VM = 2000, PL = 2021).
2. `FOOTBALL_API_ENABLED=true` och rotera `FOOTBALL_API_KEY` om den är gammal.
3. `MATCH_STATUS_SCHEDULER_ENABLED=true`,
   `LEAGUE_ACHIEVEMENT_SCHEDULER_ENABLED=true`.
4. Intervallen till normal drift (se avsnittet ovan).
5. `APP_SEASON_START` till turneringens start (eller töm den).
6. `DB_POOL_MAX_SIZE=10`, höj `-Xmx` i `JAVA_OPTS` om många är inne samtidigt.
7. `POST /api/admin/sync/fixtures` för att hämta in spelschemat.

## Övervakning

Kontrollera loggarna för att se när schedulers körs:

```bash
# Se när fixtures synkas
grep "Syncing fixtures" logs/application.log

# Se när live scores synkas
grep "Syncing.*live matches" logs/application.log

# Se när finished matches synkas
grep "Syncing.*finished matches" logs/application.log
```

## Rekommendationer

1. **Under aktiv säsong**: Använd normal drift eller mild viloläge (t.ex. 2-3 timmar för fixtures)
2. **Mellan säsonger**: Använd full viloläge (6 timmar för fixtures, 5 minuter för live)
3. **Under matcher**: Överväg att temporärt öka frekvensen för live scores om det är viktigt
4. **API-quota**: Övervaka din API-quota och justera intervall efter behov

## Support

Om du har frågor eller behöver hjälp med konfiguration, kontakta utvecklingsteamet.

