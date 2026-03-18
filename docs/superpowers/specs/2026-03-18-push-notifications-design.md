# Push Notifications via ntfy.sh — Design

## Goal

Notify all family members when someone logs a walk or feeding for Kira, and send reminders if Kira hasn't been walked by 10:00 or 20:00 Berlin time.

## Approach

Use [ntfy.sh](https://ntfy.sh) — a free, open-source push notification service. The server sends a plain HTTP POST; family members subscribe to a shared topic in the ntfy mobile app (iOS/Android). No new Maven dependencies needed.

## Notification Events

| Trigger | Message | Notes |
|---|---|---|
| Walk logged | `🐕 {person} ist mit Kira Gassi gegangen!` | Sent immediately after save |
| Food logged | `🍖 {person} hat Kira gefüttert!` | Sent immediately after save |
| 10:00 Berlin, no morning walk | `☀️ Kira war heute noch nicht draußen!` | Suppressed if pause active |
| 20:00 Berlin, no evening walk | `🌙 Kira braucht noch ihre Abendrunde!` | Suppressed if pause active |
| Neuglobsow pause (index 0) at reminder time | `✈️ Kira macht Urlaub in Neuglobsow, kein Gassi nötig!` | Fun message instead of suppressing |

All family members receive every notification (no per-user filtering).

## Components

### NotificationService (new)

- `sendWalkNotification(String person)` — called by WalkService after save
- `sendFoodNotification(String person)` — called by FoodService after save
- `checkMorningReminder()` — @Scheduled at 10:00 Berlin, checks wasMorning()
- `checkEveningReminder()` — @Scheduled at 20:00 Berlin, checks wasEvening()
- Private `send(String message)` — HTTP POST to ntfy.sh

`sendWalkNotification` and `sendFoodNotification` are annotated with `@Async` so they do not block the HTTP response. `KiratrackerApplication` gets both `@EnableScheduling` and `@EnableAsync`.

Creates its own `RestTemplate` with 3-second connect and read timeouts to prevent hung threads.

If ntfy.sh is unreachable, logs the error and continues silently — notifications are non-critical.

### WalkService (modified)

- Inject `NotificationService`
- Call `sendWalkNotification(person)` after `walkRepository.save()` in **both** `addEntry(String)` and `addWalk(String, String)` — both methods save a walk entry and must trigger the notification

### FoodService (modified)

- Inject `NotificationService`
- Call `sendFoodNotification(person)` after `foodRepository.save()`

### application.yml (modified)

```yaml
ntfy:
  url: https://ntfy.sh
  topic: ${NTFY_TOPIC}
```

### Scheduling

Spring `@Scheduled` with `@EnableScheduling` on `KiratrackerApplication`:
```
cron = "0 0 10 * * *", zone = "Europe/Berlin"
cron = "0 0 20 * * *", zone = "Europe/Berlin"
```

Pause state is checked at reminder time via `pauseRepository.findById(1L).orElse(new PauseState())`. If the row does not exist yet (fresh deployment), `getPauseIndex()` returns null — treated as no pause active.

## Pause Handling

- Neuglobsow (pause index 0): send fun message `✈️ Kira macht Urlaub in Neuglobsow, kein Gassi nötig!`
- All other pause states: suppress reminder silently
- Pause does NOT suppress walk/food event notifications (if someone logs anyway, notify)

## Note on "Evening" Walk Semantics

`wasEvening()` in `WalkService` considers any walk **after noon (12:00)** as the evening walk. This means a 13:00 afternoon walk would suppress the 20:00 reminder. This matches the existing app behavior and is intentional.

## Configuration

| Env Var | Description |
|---|---|
| `NTFY_TOPIC` | Secret topic name (e.g. `kira-geheim-abc123`). Keep private — anyone with the name can subscribe. Missing at startup → application fails to start (consistent with other env vars). |

## User Setup

1. Install ntfy app (iOS / Android) — free
2. Subscribe to the topic name set in `NTFY_TOPIC`
3. Done — notifications arrive natively

## Files Changed

| File | Change |
|---|---|
| `NotificationService.java` | New — all notification logic |
| `WalkService.java` | Inject NotificationService, call after save |
| `FoodService.java` | Inject NotificationService, call after save |
| `KiratrackerApplication.java` | Add `@EnableScheduling` and `@EnableAsync` |
| `application.yml` | Add ntfy config block |
