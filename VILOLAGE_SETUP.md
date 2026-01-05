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

