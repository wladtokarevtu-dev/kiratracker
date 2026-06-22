# KiraTracker

Familien-Tool zum Tracken von **Kira** (Hund): Wer war Gassi, wer hat gefüttert,
Erinnerungen, Fairness-Rotation. Läuft im Haushalt auf dem Handy.

## Stack & Deployment
- **Backend:** Java Spring Boot, PostgreSQL, Maven, Docker.
- **Push:** ntfy.sh.
- **Deployment:** Render (Free Tier → JVM-Kaltstart ~25 s nach Spin-down).
- **Frontend:** statische Seiten unter `src/main/resources/static/`
  (`index.html`, `admin.html`, `stats.html`, `nfc.html`), vom Spring-Server ausgeliefert.

## Wichtige Backend-Bausteine
- `HelloController` / `NfcController` — REST-Endpunkte (`/walk`, `/food`, `/status`,
  `/leaderboard`, `/walk/request`, `/notify`, `/admin/*`, `/pause`, NFC `/nfc/*`).
- `WalkService`, `FoodService`, `WalkRequestService`, `ReminderService`,
  `NotificationService`, `WeatherService`, `PauseState`/`PauseRepository`.

---

# Redesign — „the Stoic way"

Die Oberfläche wird **komplett neu** gestaltet: Funktionen bleiben, das Design wird
an die ruhige, monochrome, weißraumstarke Sprache der App **Stoic** angelehnt
(eigene Inhalte/Assets — **keine** kopierten Stoic-Texte, -Bilder oder -Logos).

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
- „Dran" = die Person mit den **wenigsten Runden im Zeitfenster** (rollierend, z. B. 7 Tage);
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

## Bekannte Altlast
- Das alte `index.html`-Frontend fällt bei API-Fehlern auf **Mock-Daten** zurück
  und verschleiert so echte Fehler. Im Redesign: echte Fehlerzustände zeigen,
  keine stillen Mocks im Produktivpfad.

---

# Design-System (Stoic-Look)

Quelle der Wahrheit = `docs/mockups/index.html` (pixelgenau übernehmen).

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
```
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
| Urlaub setzen/beenden | `POST /admin/pause {index}` · `DELETE /admin/pause` | vorhanden (admin-gated) |
| **Fairness** (wer dran, Counts) | `GET /fairness` | **neu** |
| **Selbst-Blockieren** | `GET /blocks` · `POST /block` · `DELETE /block/{id}` | **neu** |
| Erinnerungs-Push 11:00 / 22:00 | `ReminderService` Cron | **anpassen (10→11, 20→22)** |

**Slot-Ableitung:** Zeit `< 12:00` = *Morgens*, sonst *Abends* (wie `wasMorning/wasEvening`).
**Admin-Auth:** Basic-Auth (`app.security.*`), Lock fragt Creds ab und cacht in `sessionStorage`.

---

# Architektur-Entscheidungen (Defaults — gelten, bis Wlad widerspricht)

- **D1 Erinnerung:** 11:00 (morgens) / 22:00 (abends). Alten 7-Uhr-„Aaron"-Reminder
  entfernen — die Fairness deckt das ab.
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

**Implementierungsplan:** `docs/IMPLEMENTATION-PLAN.md` (autonom abarbeitbar).
