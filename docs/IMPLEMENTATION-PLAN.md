# Implementierungsplan — KiraTracker Redesign „the Stoic way"

> **Zweck:** Diesen Plan **von oben nach unten autonom abarbeiten**. Alle
> Entscheidungen sind in `CLAUDE.md` (Abschnitt „Architektur-Entscheidungen")
> vorab getroffen — **nicht nachfragen**, Defaults anwenden. Visuelle Wahrheit =
> `docs/mockups/index.html`. Nach jeder Phase committen.
>
> **So startest du den autonomen Lauf** (eine der Optionen):
> - Frische Claude-Code-Session im Repo: *„Arbeite docs/IMPLEMENTATION-PLAN.md
>   Phase für Phase ab, committe nach jeder Phase, frag nur bei rot markierten
>   Checkpoints."*
> - Oder `/gsd:autonomous` falls GSD genutzt wird.

## Voraussetzungen / Umgebung
- Build: `./mvnw -q -DskipTests package` · Tests: `./mvnw -q test`
- Lokaler Start braucht Env-Vars (Postgres, ntfy, weather, app.security) —
  siehe `docs/RUNBOOK.md`. Ohne DB: H2-Profil aus Phase 0 nutzen.
- Branch: `redesign-stoic`. Atomare Commits, deutsche Commit-Messages.

## Checkpoints (die einzigen Stellen, an denen Wlad evtl. ran muss) 🔴
- **CP-1 (Env/DB):** Wenn keine lokale Postgres/Env vorhanden → Phase 0 richtet
  H2-Test-Profil ein, damit der Rest ohne Wlad läuft. Kein Eingriff nötig.
- **CP-2 (Deploy):** Render-Deploy + echte ntfy-Zustellung kann nur Wlad final
  bestätigen (Phase 7). Alles davor ist verifizierbar autonom.

---

## Phase 0 — Branch, Baseline, Test-Profil
**Ziel:** reproduzierbar bauen/testen ohne Wlad.
1. `git switch -c redesign-stoic`.
2. Baseline-Build: `./mvnw -q -DskipTests package` muss grün sein (sonst erst fixen).
3. Test-Profil für DB-freies Laufen anlegen:
   - `src/test/resources/application-test.yml` mit H2 (`jdbc:h2:mem:kira;MODE=PostgreSQL`,
     `ddl-auto: create-drop`) + Dummy-Werte für `app.security.*`, `ntfy.topic`,
     `weather.*`. ntfy/weather im Test mocken bzw. leer lassen.
   - Sicherstellen, dass `@SpringBootTest` mit `@ActiveProfiles("test")` startet.
4. `docs/RUNBOOK.md` schreiben: nötige Env-Vars + lokaler Start + Smoke-curls
   (siehe Phase 6).
**Done-Kriterium:** `./mvnw test` grün. **Commit:** „chore: Branch + H2-Test-Profil + Runbook".

---

## Phase 1 — Erinnerungszeiten & -texte (klein, isoliert)
**Datei:** `ReminderService.java`
1. Morgen-Cron `0 0 10 * * *` → `0 0 11 * * *`. Abend-Cron `0 0 20 * * *` → `0 0 22 * * *`.
2. Texte auf die neue Semantik: *„War jemand schon mit Kira raus oder hat es
   jemand vergessen einzutragen?"* (statt nur „war noch nicht draußen").
3. `checkAaronReminder` (7:00) **entfernen** (Fairness ersetzt das, D1).
4. Pause/Urlaub-Verhalten beibehalten: bei `pauseIndex != null` keine
   Erinnerung (bzw. Urlaubstext wie gehabt).
**Test:** Unit-Test, der mit gemocktem `WalkService`/`NotificationService` prüft,
dass bei `wasMorning()==false` & keine Pause genau eine Nachricht rausgeht, bei Pause keine.
**Commit:** „feat(reminder): 11:00/22:00 + neue Texte, Aaron-Reminder entfernt".

---

## Phase 2 — Selbst-Blockieren (Backend)
**Neu:** `WalkBlock` Entity, Repo, Endpoints.
1. `WalkBlock` (`@Entity`): `id`, `person`, `slot` (`MORNING`/`EVENING` als Enum/String),
   `note` (nicht null), `day` (`LocalDate`, Standard heute), `createdAt`.
2. `WalkBlockRepository`: `findByDay(LocalDate)`; Cleanup alter Blöcke optional.
3. `WalkBlockService`: `add(person, List<slot>, note, day)`, `activeToday()`,
   `isBlocked(person, slot, day)`, `delete(id)`. Notiz Pflicht → sonst
   `IllegalArgumentException` (→ 400/404 via Handler).
4. Endpoints in `HelloController` (oder neuem `FairnessController`):
   - `GET /blocks` → heutige aktive Blöcke (`[{id,person,slot,note}]`).
   - `POST /block` `{person, slots:["MORNING","EVENING"], note}` → 200.
   - `DELETE /block/{id}` → 200. (Nicht admin-gated; Haushalts-App, vgl. D4.)
**Test:** Block anlegen ohne Notiz → Fehler; mit Notiz → erscheint in `GET /blocks`;
löschen entfernt ihn.
**Commit:** „feat(block): Selbst-Blockieren mit Pflicht-Notiz (Entity+API)".

---

## Phase 3 — Fairness (Backend)
**Neu:** `FairnessService` + `GET /fairness`.
1. Personenkreis konstant: `["Wlad","Mama","Ilja","Aaron"]` (D2).
2. Counts der letzten **14 Tage** je Person (`walkRepository`-Query analog
   `getLeaderboardSince`, aber auf den Kreis gefiltert; fehlende = 0).
3. „dran(slot)" = Person mit wenigsten Counts, **die für diesen Slot nicht
   geblockt ist**; Tie-Break = längste Zeit seit letzter Runde.
4. `GET /fairness` →
   ```json
   { "window":14,
     "people":[{"name":"Aaron","count":3,"lastWalk":"...","blocked":["EVENING"]}, ...],
     "dranMorning":"Aaron", "dranEvening":"Ilja",
     "paused": false }
   ```
   `paused` aus `PauseState` (für Ausgrauen im UI).
**Test:** bei bekannten Mock-Walks ist die Person mit wenigsten Runden „dran";
ein Block auf diese Person verschiebt „dran" auf die nächste.
**Commit:** „feat(fairness): /fairness mit 14-Tage-Fenster, blockiert-aware".

---

## Phase 4 — Frontend: neue Hauptseite (Home + Verlauf + Flows)
**Datei:** `src/main/resources/static/index.html` **ersetzen** durch eine an
`docs/mockups/index.html` angelehnte Version, **verdrahtet mit echten Endpoints**.
1. Markup/CSS/Komponenten 1:1 aus dem Mockup übernehmen (Design-System exakt).
2. **Daten echt anbinden** (Mock-Arrays raus, **kein** stiller Fallback, D6):
   - Home: `GET /status` → Begrüßung, Ritual-Zustände (wasMorning/Evening),
     Wochenstrip-Punkte (aus `GET /walk`, 1 Punkt je Runde/Tag), Wetter, Streak.
   - `GET /pause` → Urlaubs-Banner + Sperren + Fairness ausgrauen.
   - Verlauf: `GET /walk` (gruppiert nach Tag, Slot aus Uhrzeit < 12:00 = Morgens),
     `GET /leaderboard?days=3|7|14|30` (Segmented Control), `GET /fairness`.
3. **Flows verdrahten:**
   - Eintragen (frisch): `POST /walk {person}` → ntfy läuft serverseitig.
   - **Nachtragen:** `POST /walk {person, time}` mit `time` = `dd.MM.yy HH:mm`
     (heutiges Datum + gewählte Uhrzeit) → **kein** ntfy (Backend unterdrückt bei `time`).
   - Futter: `POST /food {person,food}`. Rufen: `POST /notify {person,message}`.
   - **Blockieren:** `POST /block {person,slots,note}` → danach `GET /fairness` neu.
   - **Würfeln:** rein clientseitig (Zufall aus Namensliste).
4. **Nachtrag-Hinweis (D5):** clientseitig aus `/status`+Uhrzeit ableiten,
   gestapelt, **Dismiss nur im Admin-Modus**, pro `Tag+Slot` in `localStorage`.
5. **Admin (Lock):** beim Aktivieren Basic-Auth-Creds abfragen (prompt) + in
   `sessionStorage` cachen; Verlaufs-Einträge bekommen ✎ (`PUT /admin/walk/{id}`)
   und ✕ (`DELETE /admin/walk/{id}`). Bei 401 Creds verwerfen + Hinweis.
6. **Urlaub-Toggle (Verlauf):** `POST /pause`/`DELETE /pause` (Phase 5 liefert die
   ungated Endpoints; bis dahin gegen `/admin/pause` mit Admin-Creds).
7. **Dark Mode:** Toggle wie im Mockup, Präferenz in `localStorage`.
8. **Fehlerzustände:** bei fehlgeschlagenem Fetch ruhiger Inline-Hinweis statt Mock.
**Test/Verify:** lokal starten, alle Flows klicken; Netzwerk-Tab zeigt echte Calls;
kein Mock mehr im Bundle.
**Commit:** „feat(ui): neue Stoic-Hauptseite (Home+Verlauf+Flows) an echte API".

---

## Phase 5 — Urlaub ungated + Restseiten-Anschluss
1. **Ungated Pause-Endpoints** (D4): `POST /pause {index}` + `DELETE /pause`
   (delegieren auf dieselbe Logik wie `/admin/pause`). Verlauf-Toggle darauf umstellen.
2. `stats.html` / `admin.html` / `nfc.html`: funktional verlinkt lassen
   (aus „Mehr"/Admin erreichbar). **Kein** Vollumbau jetzt — nur sicherstellen,
   dass Links existieren und nichts bricht. Optionales leichtes Token-Restyling
   nur, wenn ohne Risiko.
**Commit:** „feat(pause): ungated /pause + Restseiten verlinkt".

---

## Phase 6 — Verifikation (autonom)
1. `./mvnw test` grün (inkl. neuer Tests Phasen 1–3).
2. App mit Test-Profil starten, Smoke-curls (Beispiele):
   ```
   curl -s localhost:8080/status
   curl -s localhost:8080/fairness
   curl -s -XPOST localhost:8080/walk -H 'Content-Type: application/json' -d '{"person":"Aaron"}'
   curl -s -XPOST localhost:8080/walk -H 'Content-Type: application/json' -d '{"person":"Mama","time":"20.06.26 08:15"}'
   curl -s -XPOST localhost:8080/block -H 'Content-Type: application/json' -d '{"person":"Aaron","slots":["EVENING"],"note":"Spätschicht"}'
   curl -s localhost:8080/fairness   # dranEvening darf nicht mehr Aaron sein
   curl -s -XPOST localhost:8080/pause -H 'Content-Type: application/json' -d '{"index":0}'
   curl -s localhost:8080/pause       # active:true
   ```
3. UI-Smoke im Browser (lokal): Eintragen, Nachtragen, Blockieren, Urlaub an/aus,
   Admin löschen/Uhrzeit, Rangliste-Zeiträume, Dark Mode.
4. Kurzer Abgleich gegen `docs/mockups/index.html` (Optik deckungsgleich).
**Done-Kriterium:** alle Checks grün, keine Mock-Daten, keine Konsolenfehler.
**Commit:** „test: Smoke-Verifikation Redesign".

---

## Phase 7 — Deploy 🔴 CP-2 (Wlad)
1. PR `redesign-stoic` → main mit Zusammenfassung der Änderungen.
2. Render-Deploy (bestehende Pipeline). **Wlad bestätigt:** echte ntfy-Pushes
   kommen an, Reminder-Crons greifen (11/22 Uhr), Urlaub blockt wie erwartet.
3. Cold-Start (~25 s) im Hinterkopf — nur Doku, kein Blocker.

---

## Definition of Done (gesamt)
- [x] Optik = `docs/mockups/index.html` (Stoic-Look, Tab-Bar, Flows). _(1:1 übernommen)_
- [x] Alle Altfunktionen erhalten (Gassi, Futter, Rangliste, Rufen, Würfeln,
      Admin, NFC, Walk-Requests, Pause).
- [x] Neu: Fairness (Wlad·Mama·Ilja·Aaron, 14T), Selbst-Blockieren m. Pflicht-Notiz,
      Nachtragen gestapelt (Dismiss nur Admin), Urlaub im Verlauf, Punkt-je-Runde,
      Reminder 11/22.
- [x] Kein Mock-Fallback; echte Fehlerzustände.
- [x] `./mvnw test` grün (21 Tests); Smoke als Integrationstest mit echtem HTTP-Port.
- [ ] **CP-2 (Wlad, live):** Render-Deploy, echte ntfy-Zustellung, Crons 11/22 Uhr,
      UI-Klick-Durchlauf am Handy.
