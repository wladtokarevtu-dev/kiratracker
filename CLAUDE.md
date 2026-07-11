# KiraTracker

Familien-Tool zum Tracken von **Kira** (Hund): Wer war Gassi, wer hat gefüttert,
Erinnerungen, Fairness-Rotation. Läuft im Haushalt auf dem Handy.

## Stack & Deployment
- **Backend:** Java Spring Boot, PostgreSQL, Maven, Docker.
- **Push:** ntfy.sh.
- **Deployment:** Render (Free Tier → JVM-Kaltstart ~25 s nach Spin-down).
- **Frontend:** statische Seiten unter `src/main/resources/static/`
  (`index.html`, `admin.html`, `stats.html`, `nfc.html`), vom Spring-Server ausgeliefert.
  Das Wetter-/Hitze-Detail ist **kein eigenes HTML mehr**, sondern ein In-App-Panel
  in `index.html` (Takeover) — schnellerer Aufruf, kein zweiter Seitenload.

## Wichtige Backend-Bausteine
- `HelloController` / `NfcController` — REST-Endpunkte (`/walk`, `/food`, `/status`,
  `/leaderboard`, `/walk/request`, `/notify`, `/admin/*`, `/pause`, NFC `/nfc/*`).
- `BlockController` (`/blocks`, `/block`, `/block/{id}`) · `FairnessController` (`/fairness`) ·
  `WeatherController` (`/weather/forecast`).
- `WalkService`, `FoodService`, `WalkRequestService`, `ReminderService`,
  `NotificationService`, `WeatherService`, `PauseState`/`PauseRepository`.
- `WalkBlock`/`WalkBlockRepository`/`WalkBlockService` (Selbst-Blockieren),
  `FairnessService` (Rotation).

## Bauen & Testen
- Build: `./mvnw -q -DskipTests package` · Tests: `./mvnw -q test`.
- DB-freies Testen via H2-Profil: `src/test/resources/application-test.yml`
  (`@ActiveProfiles("test")`). Env-Vars & Smoke-curls: `docs/RUNBOOK.md`.

---

# Redesign — „the Stoic way" (umgesetzt & in `master` gemerged)

Die Oberfläche **ist neu** gestaltet (Plan `docs/IMPLEMENTATION-PLAN.md` Phase 0–7
abgearbeitet): Funktionen bleiben, das Design ist an die ruhige, monochrome,
weißraumstarke Sprache der App **Stoic** angelehnt (eigene Inhalte/Assets —
**keine** kopierten Stoic-Texte, -Bilder oder -Logos).

**Stand der Umsetzung (Abweichungen/Ergänzungen zum ursprünglichen Plan):**
- `index.html` neu, an echte API verdrahtet, **kein Mock-Fallback** (Altlast erledigt).
- **iPhone-Mockup-Chrome entfernt** (keine Statusbar/Uhr/Dynamic-Island/Akku) — der
  Phone-Frame bleibt als App-Container, auf dem Handy randlos.
- **Verlauf-Liste gecappt auf 4**, „mehr anzeigen" expandiert bis **max 10**.
- Unter „Verlauf" nur noch **Admin-Link** (Statistik/NFC-Links entfernt).
- `admin.html` komplett im Stoic-Design neu; enthält **„Verlauf auf 0 setzen"**
  (`POST /admin/reset`) mit **zusätzlichem hardcodierten Passwort-Gate** (clientseitig,
  zusätzlich zum serverseitigen Admin-Login).
- `stats.html` / `nfc.html` weiter funktional, noch im alten Stil.

- **Format:** Handy (Phone-First, ein zentraler Telefon-Frame im Browser-Mockup).
- **Optik:** monochrom, viel Weißraum, Lowercase-Begrüßung, **ein** Akzent
  (schwarz gefüllte Pillen), feine Hairlines, runde Karten, Line-Icons.
- **Navigation:** untere Tab-Bar `Heute · Verlauf · ＋ · Futter · Rufen`, zentraler
  schwarzer **+**-Button öffnet den Eintrag-Flow.
- **Interaktionen the Stoic way:** geführte Mehrschritt-Flows mit Progress-Dots
  (Auswahl-Grid → optionale Notiz → „Erledigt" + Streak), statt Formularen.
- **Home bewusst karg:** Begrüßung · Wochenstrip · zwei Ritual-Karten
  (Morgenrunde/Abendrunde) · eine Featured-Karte. Listen liegen in ihren Tabs,
  **nicht** doppelt auf Home.

**Interaktives Mockup:** `docs/mockups/index.html`
(lokal: `python3 -m http.server 4555` im Ordner → http://localhost:4555).

---

# Produktregeln (verbindlich für UI **und** Backend)

## 1. Eintragen & Kalender
- Eine Runde wird über den geführten Flow eingetragen (Wer → optional Tags → optional Notiz).
- Eintragen dürfen: **Wlad, Mama, Ilja, Aaron, Dajen**.
- **Wochenstrip:** unter jeder Datumszahl **ein Punkt pro Spaziergang** des Tages
  (also bis zu zwei: Morgen- und Abendrunde).

## 2. Tab „Verlauf" — Struktur (von oben nach unten)
1. **Verlauf** — Kiras Runden, nach Tag gruppiert (Wer · Slot · Zeit).
2. **Rangliste** — Top-Gassigänger mit Segmented Control **3 / 7 / 14 / 30 Tage**.
3. **Fairness** — siehe unten.

## 3. Fairness-Score (im Tab „Verlauf", unten)
- Zeigt, **wer als nächstes dran sein sollte**.
- **Rotation nur unter Wlad, Mama, Ilja, Aaron** (Dajen darf eintragen, zählt aber
  nicht in die Fairness-Rotation).
- „Dran" = die Person mit den **wenigsten Runden im Zeitfenster** (rollierend **14 Tage**);
  bei Gleichstand die, die am längsten nicht dran war.

## 4. Selbst-Blockieren
- Die Person, die dran wäre, kann sich für **Morgens und/oder Abends** sperren.
- **Pflicht:** eine kurze Begründung (Bemerkung), warum es nicht geht.
- Folge: Für den blockierten Slot **rückt die nächste faire Person nach**;
  die Sperre + Bemerkung ist für die Familie sichtbar.

## 5. Erinnerungs-Push & Nachtragen
- Hat sich **bis 11:00** (Morgenrunde) bzw. **bis 22:00** (Abendrunde) niemand eingetragen,
  geht eine **Push-Nachricht an die Familie** mit der Frage, ob jemand schon mit Kira
  raus war oder **vergessen hat, sich einzutragen**.
- Parallel erscheint in der App **unter dem Kalender** ein **leicht rot markierter
  Hinweis** zum **Nachtragen**: Über ihn lassen sich **Name und Uhrzeit** der verpassten
  Runde **nachträglich eintragen**.
- Der Hinweis **bleibt stehen, bis** sich jemand einträgt **oder** ein Admin ihn per
  **Dismiss (✕)** ausblendet (Dismiss nur im Admin-Modus).
- Sind **mehrere** Runden offen (z. B. Morgen- **und** Abendrunde), stehen die Hinweise
  **gestapelt untereinander** — jeder einzeln nachtragbar bzw. dismissbar.
- Bereits erledigte Slots lösen keine Erinnerung / keinen Nachtrag-Hinweis aus.

## 6. Urlaubsmodus (= Pause)
- Schaltbar **nur im Tab „Verlauf"** (unterste Zeile).
- Wenn Kira **keine Runden braucht** (verreist), ist der Urlaubsmodus aktiv;
  Statuszeile dann: **„App inaktiv — Kira ist im Urlaub."**
- Solange er aktiv ist, muss die UI das **deutlich anzeigen** und:
  - **neue Einträge blockieren** (Eintragen/Füttern/Nachtragen gesperrt, mit Hinweis),
  - den **Fairness-Score ausgrauen** (in der Zeit ohne Relevanz),
  - **Erinnerungs-Pushes pausieren**.
- Beim Beenden des Urlaubsmodus gilt wieder die normale Dynamik.

## 7. Admin-Modus
- Einstieg über ein **Lock-Symbol oben** auf der Startseite.
- Im Admin-Modus ist **jeder Eintrag löschbar** und die **Uhrzeit jedes Eintrags
  verstellbar** (in der Verlaufsliste).

## 8. Hitze-Warnung (Ampel · Wetter-Panel · 6-Uhr-Push)
- **Formel:** `tempF + humidity%` (`tempF = tempC × 9/5 + 32`) →
  `<150` = 0 grün (unbedenklich) · `150–159` = 1 gelb (Vorsicht) ·
  `160–179` = 2 orange (gefährlich) · `≥180` = 3 rot (potenziell lebensgefährlich).
- **Ampel-Punkt** sitzt **inline direkt neben der Gradzahl** im Wetter-Widget
  (8px). Kein separates Tap-Target/Popover mehr. Punkt **versteckt**, wenn
  `weather.riskLevel == null` (API down — kein falsches Grün).
- **Tap aufs Wetter** (Icon/Temp/Punkt/Beschreibung) → **In-App-Wetter-Panel**
  (Takeover in `index.html`, kein zweites HTML). Inhalt:
  - **Tagesauswahl-Strip** (OWM liefert 5 Tage / 3h) — Wochentag + Datum, aktiver
    Tag als gefüllte Pille, kleiner Ampel-Punkt (`maxRiskLevel`) je Tag.
  - **Diagramm** je Tag: **kombiniert Temperatur (Linie, geglättet) + Luftfeuchtigkeit
    (gestrichelt)**, Punkte in Ampel-Farben, X-Achse 0–24 Uhr, Jetzt-Marker (nur heute).
    Die empfohlenen **Zeitfenster sind als grüne Bänder** markiert.
  - **Empfehlungs-Pillen** (Vormittag/Abend); sind beide `null`: „kein sicheres Fenster …".
- **Zeitfenster:** Vormittag = Slots `6:00–12:00` (frühester Slot mit minimalem Risiko),
  Abend = Slots `≥ 17:00` (spätester Slot mit minimalem Risiko), Fenster = Slot + 3h;
  `null`, wenn das Minimum dort Level 3 ist. **Nichts vor 6:00 vorschlagen; Fenster-Ende
  auf 23:00 gekappt** (keine Runden mitten in der Nacht — ersetzt die alte
  „über-Mitternacht"-Eigenheit).
- **6-Uhr-Push** (Europe/Berlin): nur wenn `maxRiskLevel == 3` und kein Urlaub —
  nennt die besten Fenster, ohne Fenster: „nur kurze, schattige Runden".
  Forecast-API down um 6:00 → kein Push (keine Warnung ohne valide Daten), WARN im Log.

## 9. Spaß-Modi auf Home (Featured-Karten)
- **Würfeln** (`Am Zug`): ein zufälliger Name aus der Familie. Timer sind getrackt
  und werden bei „Nochmal"/Schließen gestoppt (früher überlappten Intervalle →
  Ergebnis sprang / manche Namen fehlten).
- **Elfmeterschießen:** zwei Gassigänger (per Dropdown/`datalist` wählen **oder**
  frei tippen) treten an — **animierte** Best-of-5-Schießerei (Torwart/Ball, Sudden
  Death bei Gleichstand), Verlierer „geht mit Kira raus". Reine Client-Logik,
  im Stoic-Design (monochrom, ein Akzent).

## Bekannte Altlast (erledigt)
- ~~Das alte `index.html`-Frontend fällt bei API-Fehlern auf Mock-Daten zurück.~~
  Im Redesign behoben: echte Fehlerzustände (ruhiger Inline-Hinweis), keine Mocks.

---

# Design-System (Stoic-Look)

Design-Sprache (Tokens/Komponenten) = `docs/mockups/index.html`. **Umgesetzter Stand
= `src/main/resources/static/index.html`** (an echte API verdrahtet; bewusste Abweichung:
iPhone-Chrome entfernt, Home luftiger, Verlauf gecappt). Bei UI-Arbeit die Live-Datei
als Wahrheit nehmen, das Mockup als Stil-Referenz.

**Farb-Tokens**
```
                 Light            Dark
--bg       #ececef          #000000
--card     #ffffff          #151517
--ink      #0d0d0f          #f4f4f6
--ink2     #6c6c74          #9b9ba3
--ink3     #a3a3ab          #65656d
--line     #e4e4e8          #262629
--fill     #0d0d0f          #f4f4f6   ← einziger Akzent (gefüllte Pillen/FAB/Check)
--onfill   #ffffff          #0d0d0f
--soft     #f4f4f6          #1d1d20
Nachtrag (leicht rot): bg #fdecec / Text #9a3636   ·  Dark: bg #291616 / Text #e6a5a5
Hitze-Ampel (--wxc0..3): #3c8a5c / #c9a227 / #c97a27 / #9a3636
                   Dark: #5fae7f / #e0c157 / #e0a157 / #e6a5a5
```
> Die Ampel-Farben sind (wie das Nachtrag-Rot) eine bewusste Ausnahme vom
> Ein-Akzent-Prinzip: funktionales Sicherheitssignal, kein Deko-Akzent.
**Typo:** Inter. Begrüßung & Screen-Titel **lowercase**, fett, `letter-spacing≈-.045em`.
Eyebrow-Labels: 0.7rem, 700, uppercase, `--ink3`.

**Prinzipien:** monochrom, viel Weißraum, **ein** Akzent (schwarz/weiß gefüllt),
Hairlines statt Rahmen, runde Karten (16–24px), Line-Icons, keine Chrome-Buttons,
native `input[type=time]` für Uhrzeiten.

**Komponenten** (alle im Mockup vorhanden):
- Phone-Frame, **Tab-Bar** `Heute · Verlauf · ＋ · Futter · Rufen` (zentraler ＋ = Eintragen)
- **Ritual-Karten** (Morgen-/Abendrunde, offen→Begin-Pille / erledigt→Chip)
- **Wochenstrip** mit **1 Punkt je Spaziergang** unter der Datumszahl
- **Featured-Karte** („Am Zug" → Würfeln)
- **Geführte Takeover-Flows** mit Progress-Dots: Auswahl-Grid → optionale/Pflicht-Notiz
  → Uhrzeit-Step → „Erledigt" + Streak. Genutzt für: Eintragen, Futter, Rufen,
  Blockieren, Nachtragen, Würfeln, Admin-Uhrzeit.
- **Segmented Control** (Rangliste 3/7/14/30)
- **Fairness-Karte**, **Nachtrag-Stack** (leicht rot), **Urlaubs-Banner**, **Lock/Admin**
- **Hitze-Ampel-Punkt** (inline neben der Gradzahl auf Home) · **Wetter-Panel**
  (In-App-Takeover: Tagesauswahl + kombiniertes Temp/Feuchte-Diagramm + Fenster-Pillen)
- **Featured-Karten** Würfeln & **Elfmeterschießen** (animiert, `datalist`-Namenswahl)

---

# Funktion → Endpoint-Mapping

| UI-Funktion | Endpoint | Status |
|---|---|---|
| Status (wasMorning/Evening, Einträge, LB7, Wetter) | `GET /status` | vorhanden |
| Runde eintragen (frisch → ntfy) | `POST /walk` `{person}` | vorhanden |
| Runde **nachtragen** (mit Uhrzeit, **kein** ntfy) | `POST /walk` `{person, time}` (`time`=`dd.MM.yy HH:mm`) | vorhanden |
| Alle Runden | `GET /walk` | vorhanden |
| Rangliste 3/7/14/30 | `GET /leaderboard?days=N` | vorhanden |
| Futter lesen/eintragen | `GET /food` · `POST /food` `{person,food}` | vorhanden |
| Rufen (Push) | `POST /notify` `{person, message}` (`walk\|urgent\|food\|evening`) | vorhanden |
| Admin: Eintrag löschen | `DELETE /admin/walk/{id}` (Basic-Auth) | vorhanden |
| Admin: Uhrzeit/Name ändern | `PUT /admin/walk/{id}` `{person,time}` (Basic-Auth) | vorhanden |
| Urlaub/Pause lesen | `GET /pause` → `{active,index}` | vorhanden |
| Urlaub setzen/beenden (UI) | `POST /pause {index}` · `DELETE /pause` (ungated) | vorhanden |
| Urlaub setzen/beenden (admin) | `POST /admin/pause {index}` · `DELETE /admin/pause` | vorhanden |
| **Fairness** (wer dran, Counts) | `GET /fairness` (14-Tage-Fenster, blockiert-aware) | vorhanden |
| **Hitze-Ampel** (aktuelles Risiko 0–3) | `GET /status` → `weather.riskLevel` | vorhanden |
| **Tagesverlauf heute + Zeitfenster** (6-Uhr-Push) | `GET /weather/forecast` → `WeatherForecastDto` | vorhanden |
| **Mehrtages-Forecast** (Wetter-Panel/Diagramm) | `GET /weather/week` → `WeekForecastDto{days:[DayForecastDto]}` | vorhanden |
| **Selbst-Blockieren** | `GET /blocks` · `POST /block {person,slots,note}` · `DELETE /block/{id}` (ungated, Notiz Pflicht) | vorhanden |
| Verlauf zurücksetzen | `POST /admin/reset` (Basic-Auth + Client-Passwort-Gate) | vorhanden |
| Erinnerungs-Push 11:00 / 22:00 | `ReminderService` Cron (Aaron-7-Uhr entfernt) | vorhanden |
| Hitze-Push bei Rot (Level 3), 6:00 Uhr | `ReminderService` Cron | vorhanden |

**Slot-Ableitung:** Zeit `< 12:00` = *Morgens*, sonst *Abends* (wie `wasMorning/wasEvening`).
**Admin-Auth:** Basic-Auth (`app.security.*`), Lock fragt Creds ab und cacht in `sessionStorage`.

---

# Architektur-Entscheidungen (umgesetzt — gelten, bis Wlad widerspricht)

> Alle D1–D6 sind in `master` implementiert. D4 ist als **ungated** `POST/DELETE /pause`
> umgesetzt (UI-Toggle nutzt diese); `/admin/pause` bleibt zusätzlich bestehen.

- **D1 Erinnerung:** 11:00 (morgens) / 22:00 (abends). Alter 7-Uhr-„Aaron"-Reminder
  entfernt — die Fairness deckt das ab.
- **D2 Fairness:** Rotation **Wlad·Mama·Ilja·Aaron**; „dran" = wenigste Runden in den
  letzten **14 Tagen**, Tie-Break = längste Zeit seit letzter Runde. Dajen zählt nicht.
- **D3 Blockieren:** gilt für den nächsten betroffenen Slot (heute, sonst morgen),
  **Notiz Pflicht**; blockierte Person wird im Slot übersprungen, nächste rückt nach.
- **D4 Urlaub:** `pauseIndex != null` = aktiv. Schaltbar **nur im Verlauf-Tab**.
  Default: neue **ungated** `POST /pause` + `DELETE /pause` (Haushalts-App).
  Alternative (falls gewünscht): admin-gated über bestehende `/admin/pause`.
- **D5 Nachtrag-Hinweis:** rein clientseitig aus `/status` + Uhrzeit abgeleitet
  (wasMorning=false nach 11:00 / wasEvening=false nach 22:00, nicht im Urlaub).
  **Dismiss nur Admin**, pro Tag+Slot in `localStorage` — kein Backend nötig.
- **D6 Frontend-Umfang:** zuerst neues `index.html` (Home + Verlauf + Flows).
  `stats.html` / `admin.html` / `nfc.html` bleiben funktional verlinkt, Restyling später.
  **Mock-Fallback raus**, echte Fehlerzustände zeigen.
- **D7 Hitze-Warnung:** Risiko-Level als reine Funktion `WeatherService.riskLevel()`,
  Fenster-Logik netzwerkfrei in `buildForecast()` (direkt testbar, kein HTTP-Mocking).
  `WeatherDto.riskLevel` ist `Integer` — `null` = API down (Frontend versteckt den Punkt).
  Forecast-URL wird aus `WEATHER_API_URL` abgeleitet (`/weather` → `/forecast`);
  **die Env-Var muss auf `/weather` enden**, sonst bleibt der Forecast leer (WARN im Log).
  Spec: `docs/superpowers/specs/2026-07-08-hitze-warnung-design.md`.

**Implementierungsplan:** `docs/IMPLEMENTATION-PLAN.md` (autonom abarbeitbar).
