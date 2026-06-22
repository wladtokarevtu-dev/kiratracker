# RUNBOOK — KiraTracker

Lokaler Betrieb, Tests und Smoke-Checks für Entwicklung & Deployment.

## Build & Test

```bash
./mvnw -q -DskipTests package   # Build
./mvnw -q test                  # Tests (H2-Profil, keine DB/Env nötig)
```

Die Tests laufen mit `@ActiveProfiles("test")` gegen `src/test/resources/application-test.yml`
(H2 in-memory, `MODE=PostgreSQL`, `ddl-auto: create-drop`). ntfy/weather zeigen auf
`localhost:0` (failen schnell, ohne Netz). **Keine** Postgres- oder Env-Vars nötig.

## Env-Vars (Produktivbetrieb / lokaler Start gegen echte DB)

| Variable | Zweck |
|---|---|
| `SPRING_DATASOURCE_URL` | Postgres JDBC-URL |
| `SPRING_DATASOURCE_USERNAME` | DB-User |
| `SPRING_DATASOURCE_PASSWORD` | DB-Passwort |
| `APP_SECURITY_USERNAME` | Basic-Auth-User für `/admin/**` |
| `APP_SECURITY_PASSWORD` | Basic-Auth-Passwort |
| `WEATHER_API_KEY` | Wetter-API-Key |
| `WEATHER_API_URL` | Wetter-API-Basis-URL |
| `WEATHER_CITY` | Stadt (z. B. `Berlin`) |
| `WEATHER_COUNTRY` | Land (z. B. `DE`) |
| `NTFY_TOPIC` | ntfy.sh-Topic für Push |
| `PORT` | optional, Default 8080 |

`ntfy.url` ist auf `https://ntfy.sh` festgelegt (siehe `application.yml`).

## Lokaler Start (echte DB)

```bash
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/kira
export SPRING_DATASOURCE_USERNAME=kira
export SPRING_DATASOURCE_PASSWORD=...
export APP_SECURITY_USERNAME=admin
export APP_SECURITY_PASSWORD=...
export WEATHER_API_KEY=... WEATHER_API_URL=... WEATHER_CITY=Berlin WEATHER_COUNTRY=DE
export NTFY_TOPIC=...
./mvnw spring-boot:run
```

App läuft danach auf http://localhost:8080 (UI = `static/index.html`).

## Smoke-Checks (curl)

```bash
curl -s localhost:8080/status
curl -s localhost:8080/fairness
curl -s -XPOST localhost:8080/walk -H 'Content-Type: application/json' -d '{"person":"Aaron"}'
# Nachtragen (kein Push, time=dd.MM.yy HH:mm):
curl -s -XPOST localhost:8080/walk -H 'Content-Type: application/json' -d '{"person":"Mama","time":"20.06.26 08:15"}'
# Selbst-Blockieren:
curl -s -XPOST localhost:8080/block -H 'Content-Type: application/json' -d '{"person":"Aaron","slots":["EVENING"],"note":"Spätschicht"}'
curl -s localhost:8080/blocks
curl -s localhost:8080/fairness   # dranEvening darf nicht mehr Aaron sein
# Urlaub/Pause (ungated):
curl -s -XPOST localhost:8080/pause -H 'Content-Type: application/json' -d '{"index":0}'
curl -s localhost:8080/pause       # active:true
curl -s -XDELETE localhost:8080/pause
```

Admin-Endpoints (`/admin/**`) brauchen Basic-Auth:

```bash
curl -s -u "$APP_SECURITY_USERNAME:$APP_SECURITY_PASSWORD" -XDELETE localhost:8080/admin/walk/1
```

## Deployment (Render)

- Free Tier → JVM-Kaltstart ~25 s nach Spin-down (kein Blocker, nur Geduld beim ersten Call).
- Bestehende Pipeline (Docker). Env-Vars im Render-Dashboard setzen.
- Reminder-Crons greifen serverseitig (Zone `Europe/Berlin`): 11:00 Morgen, 22:00 Abend.
