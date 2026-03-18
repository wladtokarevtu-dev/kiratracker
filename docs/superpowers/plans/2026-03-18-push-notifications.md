# Push Notifications + Admin Time Editing Implementation Plan

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Notify family members via ntfy.sh when a walk or feeding is logged, send reminders at 10:00/20:00 if Kira wasn't walked, and allow admins to edit both person and time of walk entries.

**Architecture:** A new `NotificationService` handles all ntfy.sh HTTP calls and scheduled reminders. It is injected into `WalkService` and `FoodService` to fire after saves. The frontend `editWalk()` function is extended to prompt for time in `HH:mm` format; the backend gains a second time-parse format to accept this.

**Tech Stack:** Spring Boot 3.2, Java 17, `RestTemplate` (already in spring-boot-starter-web), `@Async`/`@Scheduled`, ntfy.sh HTTP API, vanilla JS.

---

## Chunk 1: Config + NotificationService

### Task 1: Add ntfy config to application.yml and enable scheduling/async

**Files:**
- Modify: `src/main/resources/application.yml`
- Modify: `src/main/java/de/wlad/kiratracker/KiratrackerApplication.java`

- [ ] **Step 1: Add ntfy block to application.yml**

Add after the `server:` block:
```yaml
ntfy:
  url: https://ntfy.sh
  topic: ${NTFY_TOPIC}
```

- [ ] **Step 2: Add @EnableScheduling and @EnableAsync to KiratrackerApplication**

```java
package de.wlad.kiratracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class KiratrackerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KiratrackerApplication.class, args);
    }
}
```

- [ ] **Step 3: Verify compile**

```bash
./mvnw compile -q
```
Expected: no output (clean compile).

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/application.yml src/main/java/de/wlad/kiratracker/KiratrackerApplication.java
git commit -m "config: add ntfy settings and enable scheduling/async"
```

---

### Task 2: Create NotificationService

**Files:**
- Create: `src/main/java/de/wlad/kiratracker/NotificationService.java`

- [ ] **Step 1: Create the service**

```java
package de.wlad.kiratracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final RestTemplate restTemplate;
    private final String ntfyUrl;
    private final String ntfyTopic;
    private final WalkService walkService;
    private final PauseRepository pauseRepository;

    // @Lazy on WalkService breaks the circular dependency:
    // WalkService → NotificationService → WalkService
    public NotificationService(
            @Value("${ntfy.url}") String ntfyUrl,
            @Value("${ntfy.topic}") String ntfyTopic,
            @Lazy WalkService walkService,
            PauseRepository pauseRepository) {
        this.ntfyUrl = ntfyUrl;
        this.ntfyTopic = ntfyTopic;
        this.walkService = walkService;
        this.pauseRepository = pauseRepository;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Async
    public void sendWalkNotification(String person) {
        send("🐕 " + person + " ist mit Kira Gassi gegangen!");
    }

    @Async
    public void sendFoodNotification(String person) {
        send("🍖 " + person + " hat Kira gefüttert!");
    }

    @Scheduled(cron = "0 0 10 * * *", zone = "Europe/Berlin")
    public void checkMorningReminder() {
        if (walkService.wasMorning()) return;
        Integer pauseIndex = getPauseIndex();
        if (pauseIndex == null) {
            send("☀️ Kira war heute noch nicht draußen!");
        } else if (pauseIndex == 0) {
            send("✈️ Kira macht Urlaub in Neuglobsow, kein Gassi nötig!");
        }
        // other pause types: silent
    }

    @Scheduled(cron = "0 0 20 * * *", zone = "Europe/Berlin")
    public void checkEveningReminder() {
        if (walkService.wasEvening()) return;
        Integer pauseIndex = getPauseIndex();
        if (pauseIndex == null) {
            send("🌙 Kira braucht noch ihre Abendrunde!");
        } else if (pauseIndex == 0) {
            send("✈️ Kira macht Urlaub in Neuglobsow, kein Gassi nötig!");
        }
        // other pause types: silent
    }

    private Integer getPauseIndex() {
        return pauseRepository.findById(1L)
                .orElse(new PauseState())
                .getPauseIndex();
    }

    private void send(String message) {
        try {
            restTemplate.postForEntity(ntfyUrl + "/" + ntfyTopic, message, String.class);
        } catch (Exception e) {
            log.warn("ntfy.sh notification failed: {}", e.getMessage());
        }
    }
}
```

- [ ] **Step 2: Verify compile**

```bash
./mvnw compile -q
```
Expected: no output.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/de/wlad/kiratracker/NotificationService.java
git commit -m "feat: add NotificationService with ntfy.sh push and scheduled reminders"
```

---

## Chunk 2: Wire Notifications + Backend Time Parsing

### Task 3: Wire NotificationService into WalkService

**Files:**
- Modify: `src/main/java/de/wlad/kiratracker/WalkService.java`

- [ ] **Step 1: Add NotificationService field and constructor injection**

In `WalkService.java`, add the new field (FORMATTER and BERLIN_ZONE already exist — do NOT re-declare them):

```java
private final NotificationService notificationService;
```

Replace the existing constructor:
```java
public WalkService(WalkRepository walkRepository, NotificationService notificationService) {
    this.walkRepository = walkRepository;
    this.notificationService = notificationService;
}
```

- [ ] **Step 2: Call notification in addEntry()**

Note: `addEntry` is currently unused (all walk logging goes through `addWalk`), so this call will never fire at runtime. Add it anyway for consistency in case the method gets used in future.

```java
@Transactional
public WalkEntry addEntry(String person) {
    ZonedDateTime now = ZonedDateTime.now(BERLIN_ZONE);
    WalkEntry entry = new WalkEntry(person, now);
    WalkEntry saved = walkRepository.save(entry);
    notificationService.sendWalkNotification(person);
    return saved;
}
```

- [ ] **Step 3: Call notification in addWalk()**

```java
@Transactional
public void addWalk(String person, String time) {
    ZonedDateTime walkTime;
    if (time != null && !time.isEmpty()) {
        try {
            walkTime = ZonedDateTime.parse(time);
        } catch (Exception e) {
            walkTime = ZonedDateTime.now(BERLIN_ZONE);
        }
    } else {
        walkTime = ZonedDateTime.now(BERLIN_ZONE);
    }
    walkRepository.save(new WalkEntry(person, walkTime));
    notificationService.sendWalkNotification(person);
}
```

- [ ] **Step 4: Verify compile**

```bash
./mvnw compile -q
```
Expected: no output.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/de/wlad/kiratracker/WalkService.java
git commit -m "feat: send push notification when walk is logged"
```

---

### Task 4: Wire NotificationService into FoodService

**Files:**
- Modify: `src/main/java/de/wlad/kiratracker/FoodService.java`

- [ ] **Step 1: Add NotificationService field and constructor injection**

In `FoodService.java`, add the new field (BERLIN_ZONE and FORMATTER already exist — do NOT re-declare them):

```java
private final NotificationService notificationService;
```

Replace the existing constructor:
```java
public FoodService(FoodRepository foodRepository, NotificationService notificationService) {
    this.foodRepository = foodRepository;
    this.notificationService = notificationService;
}
```

- [ ] **Step 2: Call notification in addFood()**

```java
@Transactional
public FoodEntryDto addFood(String person, String food) {
    if (person == null || person.trim().isEmpty()) {
        throw new IllegalArgumentException("Person darf nicht leer sein.");
    }
    FoodEntry entry = new FoodEntry(
            person.trim(),
            food != null ? food.trim() : "",
            ZonedDateTime.now(BERLIN_ZONE)
    );
    FoodEntry saved = foodRepository.save(entry);
    notificationService.sendFoodNotification(person.trim());
    return toDto(saved);
}
```

- [ ] **Step 3: Verify compile**

```bash
./mvnw compile -q
```
Expected: no output.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/de/wlad/kiratracker/FoodService.java
git commit -m "feat: send push notification when feeding is logged"
```

---

### Task 5: Extend time parsing in WalkService to accept "dd.MM.yy HH:mm"

**Note on format:** The spec originally suggested ISO 8601 from the frontend, but sending `"dd.MM.yy HH:mm"` is simpler (no DST offset calculation in JS) and requires only a small backend change. This is an intentional deviation.

The admin frontend will send time as `"dd.MM.yy HH:mm"` (same format as already displayed). The backend needs to parse this into a Berlin-zoned `ZonedDateTime`.

**Files:**
- Modify: `src/main/java/de/wlad/kiratracker/WalkService.java`

- [ ] **Step 1: Add input formatter constant**

In `WalkService`, add alongside the existing `FORMATTER`:
```java
private static final DateTimeFormatter INPUT_FORMATTER =
        DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");
```

- [ ] **Step 2: Add helper methods**

Add the import at the top of `WalkService.java`: `import java.time.LocalDateTime;`

Add two private helpers — one that parses silently (for `addWalk`, where bad input falls back to now), and one that throws (for `updateEntry`, to preserve the existing 400-error behavior):

```java
// Used in addWalk: falls back to now on bad input
private ZonedDateTime parseTimeSoft(String timeString) {
    try { return ZonedDateTime.parse(timeString); } catch (Exception ignored) {}
    try { return LocalDateTime.parse(timeString, INPUT_FORMATTER).atZone(BERLIN_ZONE); } catch (Exception ignored) {}
    return ZonedDateTime.now(BERLIN_ZONE);
}

// Used in updateEntry: throws on bad input (returns HTTP 404 via @ExceptionHandler(IllegalArgumentException.class))
private ZonedDateTime parseTimeStrict(String timeString) {
    try { return ZonedDateTime.parse(timeString); } catch (Exception ignored) {}
    try { return LocalDateTime.parse(timeString, INPUT_FORMATTER).atZone(BERLIN_ZONE); } catch (Exception ignored) {}
    throw new IllegalArgumentException("Ungueltige Zeitformat: " + timeString);
}
```

- [ ] **Step 3: Use the helpers in addWalk() and updateEntry()**

In `addWalk()`, replace the existing try/catch block:
```java
if (time != null && !time.isEmpty()) {
    walkTime = parseTimeSoft(time);
} else {
    walkTime = ZonedDateTime.now(BERLIN_ZONE);
}
```

In `updateEntry()`, replace the existing try/catch block:
```java
if (timeString != null && !timeString.isEmpty()) {
    entry.setTime(parseTimeStrict(timeString));
}
```

- [ ] **Step 4: Verify compile**

```bash
./mvnw compile -q
```
Expected: no output.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/de/wlad/kiratracker/WalkService.java
git commit -m "feat: accept 'dd.MM.yy HH:mm' format in walk time parsing"
```

---

## Chunk 3: Frontend

### Task 6: Extend editWalk() in index.html to prompt for time

**Files:**
- Modify: `src/main/resources/static/index.html`

- [ ] **Step 1: Update editWalk() function**

Find the existing `editWalk` function (line ~386) and replace it:

```javascript
async function editWalk(id,p,tf){
    if(!document.body.classList.contains('admin-mode'))return;
    const np=prompt('Person:',p);
    if(np===null)return;
    const parts=(tf||'').trim().split(' ');
    const curTime=parts[0]||'12:00'; // "HH:mm"
    const curDate=parts[1]||'';      // "dd.MM.yy"
    const nt=prompt('Uhrzeit (HH:mm):',curTime);
    if(nt===null)return;
    if(!/^\d{1,2}:\d{2}$/.test(nt)){toast('Ungültige Zeit — Format: HH:mm');return;}
    haptic('m');
    try{
        const timeStr=curDate?curDate+' '+nt:null;
        await api(API+'/admin/walk/'+id,{method:'PUT',headers:ah(),body:JSON.stringify({person:np,time:timeStr})});
        await Promise.all([loadH(),loadStatus(),loadLb(),loadStats()]);
    }catch{toast(tr('e-s'));}
}
```

- [ ] **Step 2: Update renderH() to pass timeFormatted to editWalk()**

Find `renderH()` (line ~400). The button currently calls `editWalk(${w.id},'${x(w.person)}')`. Change it to also pass `timeFormatted`:

Find:
```javascript
<button class="bi" onclick="editWalk('+w.id+',\''+x(w.person)+'\')">✏️</button>
```

Replace with:
```javascript
<button class="bi" onclick="editWalk('+w.id+',\''+x(w.person)+'\',\''+x(w.timeFormatted||'')+'\')">✏️</button>
```

- [ ] **Step 3: Verify compile**

```bash
./mvnw compile -q
```
Expected: no output.

- [ ] **Step 4: Manual smoke test (optional, if DB available)**

In admin mode, click ✏️ on a walk entry. Two prompts should appear: one for the person, one for the time (pre-filled with the existing time). Change the time to e.g. `08:30`. The entry should update and show `08:30` in Berlin time.

- [ ] **Step 5: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat: allow editing both person and time of walk entries in admin mode"
```

---

## Post-Implementation: Setup Instructions for the User

After deploying, each family member:

1. Install the **ntfy** app (iOS: App Store / Android: Play Store or F-Droid)
2. Open the app → tap **+** → Subscribe to topic
3. Enter the topic name (the value of `NTFY_TOPIC` env var)
4. Done — notifications will arrive natively

Add `NTFY_TOPIC` to the hosting environment (e.g. Render, Railway) with a secret name like `kira-familie-abc123`.
