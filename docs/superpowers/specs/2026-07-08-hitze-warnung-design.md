# Hitze-Warnung für Kira — Design

Datum: 2026-07-08

## Ziel

Im Sommer kann die Kombination aus Temperatur und Luftfeuchtigkeit für Kira
gefährlich werden. Die App soll das sichtbar machen: eine Ampel neben dem
Wetter auf der Startseite, ein Tagesverlauf-Diagramm mit Spazier-Empfehlung
auf einer neuen Detailseite, und an wirklich heftigen Tagen eine 6-Uhr-Push-
Nachricht mit den besten Zeitfenstern.

## 1. Risiko-Berechnung

Faustregel (etablierter Tierschutz-Richtwert): `tempF + humidity%`, wobei
`tempF = tempC × 9/5 + 32`.

| Summe    | Level | Bedeutung                              | Farbe (light/dark)              |
|----------|-------|-----------------------------------------|----------------------------------|
| < 150    | 0     | unbedenklich                            | `#3c8a5c` / aufgehellt           |
| 150–159  | 1     | Vorsicht                                | `#c9a227` / aufgehellt           |
| 160–179  | 2     | gefährlich                              | `#c97a27` / aufgehellt           |
| ≥ 180    | 3     | potenziell lebensgefährlich             | `#9a3636` / `#e6a5a5` (wie Nachtrag-Rot) |

Implementiert als reine Funktion (z.B. `WeatherService.riskLevel(tempC, humidityPct)`),
wiederverwendet für aktuelles Wetter, Forecast-Punkte und den 6-Uhr-Check.

`WeatherDto` bekommt ein neues Feld `riskLevel` (`Integer`, 0–3), berechnet aus dem
aktuellen `temperature`/`humidity` beim Abruf in `WeatherService.getCurrentWeather()`.
Der Default-Wetter-Fallback (API nicht erreichbar) liefert `riskLevel = null`
(keine Warnung ohne valide Daten).

## 2. Neuer Endpoint `GET /weather/forecast`

Neuer `WeatherController`, folgt dem Muster von `BlockController`/`FairnessController`.

`WeatherService` bekommt eine neue Methode `getTodayForecast()`, die
OpenWeatherMap `/data/2.5/forecast` (3h-Schritte, gleicher API-Key/Stadt wie
bisher) abruft, auf die 3h-Slots von heute (Europe/Berlin) filtert und mappt.

Antwortform:

```json
{
  "points": [
    { "time": "09:00", "temperature": 23.4, "humidity": 55, "riskLevel": 0 }
  ],
  "maxRiskLevel": 2,
  "morningWindow": { "start": "07:00", "end": "09:00" },
  "eveningWindow": { "start": "20:00", "end": "22:00" }
}
```

Neue DTOs: `ForecastPointDto` (time, temperature, humidity, riskLevel),
`WeatherWindowDto` (start, end), `WeatherForecastDto` (points, maxRiskLevel,
morningWindow, eveningWindow).

**Fenster-Logik:**
- Vormittagsbereich = Slots vor 12:00 Uhr. Gesucht wird der früheste Slot mit
  dem niedrigsten `riskLevel` im Vormittagsbereich.
- Abendbereich = Slots ab 17:00 Uhr. Gesucht wird der späteste Slot mit dem
  niedrigsten `riskLevel` im Abendbereich.
- Jedes Fenster ist der gefundene 3h-Slot (`start` = Slot-Zeit, `end` = Slot-Zeit + 3h).
- Ist im jeweiligen Bereich der niedrigste erreichbare Wert trotzdem
  `riskLevel == 3`, wird das Fenster als `null` zurückgegeben
  ("kein sicheres Fenster").
- `maxRiskLevel` = höchster `riskLevel` unter allen Punkten von heute.

Dieser Endpoint wird sowohl vom Detail-Screen als auch vom 6-Uhr-Cronjob
(serverseitig über `WeatherService`, nicht per HTTP-Call) genutzt.

## 3. Home: Ampel-Punkt + Infobox

In `.wx-top` (neben Icon/Temperatur/Beschreibung) ein neuer 8px-Punkt,
gefüllt in der Farbe des aktuellen `riskLevel` (siehe Tabelle oben). Bewusste
Ausnahme vom Ein-Akzent-Prinzip des Stoic-Designs, da es ein funktionales
Sicherheitssignal ist (wie das bestehende Nachtrag-Rot).

- Tap auf den Punkt: kleine Popover-Box direkt darunter (kein Modal,
  Card-Style `--card`/`--line`), listet alle 4 Stufen mit Farbe + Kurzbeschreibung,
  aktuelle Stufe hervorgehoben. Schließt bei Tap auf den Punkt erneut oder
  Tap außerhalb.
- Tap auf den Rest von `.wx-top` (Icon/Temperatur/Beschreibung): Navigation zu
  `weather.html`.

Falls kein valides Wetter vorliegt (`riskLevel` fehlt/API down): Punkt wird
nicht angezeigt.

## 4. Neue Seite `weather.html`

Volle Seite im Stoic-Look, analog zu `admin.html` (kein Phone-Frame-Chrome,
`‹ zurück` zu `/index.html`, `--bg`/Card-Tokens, Inter-Typo).

Aufbau von oben nach unten:
1. Titel `heute.` (lowercase, wie Home-Begrüßung) + aktuelle Ampel-Stufe als Text
   darunter (z.B. "gefährlich für Kira").
2. **Tagesverlauf-Diagramm**: handgebautes SVG (kein Chart-Framework). Linie/
   Fläche über die Temperaturkurve der `points`, x-Achse = Uhrzeiten. Jedes
   Segment/jeder Punkt eingefärbt nach seinem `riskLevel` (visuelles Modell
   des Risikoverlaufs über den Tag, nicht nur der Temperatur).
3. **Empfehlung**: Zeitfenster als Pillen, z.B.
   "spazieren gehen: 07:00–09:00 · 20:00–22:00". Ist ein Fenster `null`, wird
   es weggelassen; sind beide `null`: Hinweistext
   "heute kein sicheres Fenster — wenn möglich, kurze Runden im Schatten."

Lädt `GET /weather/forecast` beim Öffnen; zeigt bei Fehler einen ruhigen
Inline-Hinweis (kein Mock-Fallback, konsistent mit dem Rest der App).

## 5. 6-Uhr-Push bei heftigen Tagen

Neue `@Scheduled(cron = "0 0 6 * * *", zone = "Europe/Berlin")`-Methode in
`ReminderService` (gleiches Muster wie die bestehenden Reminder-Crons),
zusätzliche Abhängigkeit auf `WeatherService`.

- Überspringt den Push, wenn Urlaubsmodus aktiv ist (`pauseIndex != null`,
  gleiche Prüfung wie bei den bestehenden Morgen-/Abend-Remindern).
- Ruft `weatherService.getTodayForecast()` auf.
- Push nur, wenn `maxRiskLevel == 3` (rot). Bei niedrigeren Stufen: kein Push
  (vermeidet Push-Müdigkeit im Sommer).
- Nachrichtentext über `notificationService.sendCustomNotification(...)`:
  - Mit mindestens einem Fenster: `"🌡️ Heute wird's heiß für Kira — beste
    Zeiten: 07:00–09:00 und 20:00–22:00"` (nur vorhandene Fenster auflisten).
  - Ohne Fenster (beide `null`): `"🌡️ Heute wird's sehr heiß für Kira — heute
    lieber nur kurze, schattige Runden."`

## Zusammenfassung Endpoint-Mapping (Ergänzung zu CLAUDE.md)

| UI-Funktion | Endpoint | Status |
|---|---|---|
| Aktuelles Risiko (Ampel-Punkt Home) | `GET /status` → `weather.riskLevel` | neu |
| Tagesverlauf + Zeitfenster | `GET /weather/forecast` | neu |
| Hitze-Push bei Rot, 6:00 Uhr | `ReminderService` Cron | neu |

## Out of Scope

- Keine Berücksichtigung von Regen/Wind in der Ampel (nur Temp+Feuchte, wie
  angefragt).
- Keine Saison-Gate ("nur im Sommer aktiv") — die Berechnung ist ganzjährig
  aktiv, ergibt aber im Winter naturgemäß Level 0.
- Keine rassespezifische Anpassung der Schwellenwerte über die Standardregel
  hinaus.
