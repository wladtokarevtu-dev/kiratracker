# Hitze-Warnung für Kira Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Eine Temp+Feuchte-basierte Hitze-Ampel für Kira: farbiger Punkt neben dem Wetter auf der Startseite mit Info-Popover, eine neue Detailseite mit Tagesverlaufs-Diagramm und Zeitfenster-Empfehlung, und ein 6-Uhr-Push an besonders heißen Tagen.

**Architecture:** Eine reine Risiko-Formel (`WeatherService.riskLevel`) wird sowohl beim aktuellen Wetter (`GET /status` → `WeatherDto.riskLevel`) als auch bei einem neuen 3h-Forecast-Endpoint (`GET /weather/forecast`) verwendet. Der Forecast-Endpoint liefert Tagespunkte plus die besten Vormittags-/Abend-Zeitfenster; dieselbe Logik nutzt ein neuer 6-Uhr-Cronjob in `ReminderService` für die Push-Nachricht. Frontend: `index.html` bekommt einen Ampel-Punkt + Popover, eine neue `weather.html` zeigt ein handgebautes SVG-Diagramm.

**Tech Stack:** Java 17 / Spring Boot (bestehend), OpenWeatherMap REST-API via `RestTemplate` (bestehend), Vanilla JS + handgebautes SVG im Frontend (keine neuen Abhängigkeiten).

## Global Constraints

- Ampel-Formel: `tempF + humidity%` mit `tempF = tempC × 9/5 + 32`. `<150`→0 (grün) · `150–159`→1 (gelb) · `160–179`→2 (orange) · `≥180`→3 (rot).
- Forecast-Datenquelle: OpenWeatherMap `/forecast` (3h-Schritte), abgeleitet aus der bestehenden `weather.api.url` (kein neuer Env-Var).
- Fenster-Logik: Vormittag = Slots vor 12:00, Abend = Slots ab 17:00. Frühester/spätester Slot mit minimalem `riskLevel` im jeweiligen Bereich; `null`, wenn das Minimum dort trotzdem Level 3 ist.
- 6-Uhr-Push nur bei `maxRiskLevel == 3`, übersprungen wenn Urlaubsmodus aktiv (`pauseIndex != null`).
- Design: Stoic-Farbtokens aus `CLAUDE.md` (`--bg/--card/--ink/--ink2/--ink3/--line/--fill/--onfill/--soft`), Ampel-Farben als Ausnahme vom Ein-Akzent-Prinzip: `#3c8a5c`/`#c9a227`/`#c97a27`/`#9a3636` (light), aufgehellt im Dark-Mode (`#5fae7f`/`#e0c157`/`#e0a157`/`#e6a5a5`).
- `weather.html` folgt dem Vollseiten-Muster von `admin.html` (kein Phone-Frame, `‹ zurück`, `--bg`-Body, Dark-Mode-Toggle mit `localStorage['mk3-dark']`).
- Kein Chart-Framework — handgebautes SVG.
- Bestehende Testkonventionen: reine Logik über AssertJ/JUnit5 direkt testen (siehe `FairnessServiceTest`), Scheduler-Services über Plain-Mockito ohne Spring-Context testen (siehe `ReminderServiceTest`), volle Verdrahtung über `SmokeIntegrationTest` (`@SpringBootTest`, `RANDOM_PORT`, `@ActiveProfiles("test")`).

---

### Task 1: Risiko-Formel + `WeatherDto.riskLevel`

**Files:**
- Modify: `src/main/java/de/wlad/kiratracker/WeatherDto.java`
- Modify: `src/main/java/de/wlad/kiratracker/WeatherService.java`
- Test: `src/test/java/de/wlad/kiratracker/WeatherServiceTest.java`

**Interfaces:**
- Produces: `WeatherService.riskLevel(double tempC, int humidityPct) -> int` (public static, 0–3). `WeatherDto` bekommt Feld `riskLevel` (int) mit Getter `getRiskLevel()`/Setter `setRiskLevel(int)`, 6-Parameter-Konstruktor `(description, temperature, humidity, windSpeed, icon, riskLevel)`.

- [ ] **Step 1: `WeatherDto` um `riskLevel` erweitern**

Ersetze den kompletten Inhalt von `src/main/java/de/wlad/kiratracker/WeatherDto.java`:

```java
package de.wlad.kiratracker;

public class WeatherDto {
    private String description;
    private double temperature;
    private int humidity;
    private double windSpeed;
    private String icon;
    private int riskLevel;

    public WeatherDto() {
    }

    public WeatherDto(String description, double temperature, int humidity, double windSpeed, String icon, int riskLevel) {
        this.description = description;
        this.temperature = temperature;
        this.humidity = humidity;
        this.windSpeed = windSpeed;
        this.icon = icon;
        this.riskLevel = riskLevel;
    }

    // Getters and Setters
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public double getWindSpeed() {
        return windSpeed;
    }

    public void setWindSpeed(double windSpeed) {
        this.windSpeed = windSpeed;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public int getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(int riskLevel) {
        this.riskLevel = riskLevel;
    }
}
```

- [ ] **Step 2: `WeatherService` um `riskLevel()` erweitern und in `getCurrentWeather()`/`getDefaultWeather()` verdrahten**

Ersetze den kompletten Inhalt von `src/main/java/de/wlad/kiratracker/WeatherService.java`:

```java
package de.wlad.kiratracker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.util.Map;

@Service
public class WeatherService {

    @Value("${weather.api.key}")
    private String apiKey;

    @Value("${weather.api.url}")
    private String apiUrl;

    @Value("${weather.city}")
    private String city;

    @Value("${weather.country}")
    private String country;

    private final RestTemplate restTemplate;

    public WeatherService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(5000);
        this.restTemplate = new RestTemplate(factory);
    }

    /**
     * Faustregel für Hitzegefahr bei Hunden: Temperatur(°F) + Luftfeuchtigkeit(%).
     * <150 unbedenklich, 150–159 Vorsicht, 160–179 gefährlich, ≥180 potenziell lebensgefährlich.
     */
    public static int riskLevel(double tempC, int humidityPct) {
        double tempF = tempC * 9.0 / 5.0 + 32;
        double sum = tempF + humidityPct;
        if (sum >= 180) return 3;
        if (sum >= 160) return 2;
        if (sum >= 150) return 1;
        return 0;
    }

    public WeatherDto getCurrentWeather() {
        try {
            String url = String.format("%s?q=%s,%s&appid=%s&units=metric&lang=de",
                    apiUrl, city, country, apiKey);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                return getDefaultWeather();
            }

            Map<String, Object> main = (Map<String, Object>) response.get("main");
            Map<String, Object> wind = (Map<String, Object>) response.get("wind");
            var weatherList = (java.util.List<Map<String, Object>>) response.get("weather");
            Map<String, Object> weather = weatherList.get(0);

            double temperature = ((Number) main.get("temp")).doubleValue();
            int humidity = ((Number) main.get("humidity")).intValue();

            return new WeatherDto(
                    (String) weather.get("description"),
                    temperature,
                    humidity,
                    ((Number) wind.get("speed")).doubleValue(),
                    (String) weather.get("icon"),
                    riskLevel(temperature, humidity)
            );

        } catch (RestClientException | NullPointerException e) {
            return getDefaultWeather();
        }
    }

    private WeatherDto getDefaultWeather() {
        return new WeatherDto("Wetter nicht verfügbar", 0.0, 0, 0.0, "01d", 0);
    }
}
```

- [ ] **Step 3: Testdatei mit Grenzwert-Tests für `riskLevel()` anlegen**

Erstelle `src/test/java/de/wlad/kiratracker/WeatherServiceTest.java`:

```java
package de.wlad.kiratracker;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherServiceTest {

    // tempC ist bewusst ein Vielfaches von 5, damit tempF exakt (ohne
    // Rundungsfehler) berechnet wird: F = tempC*9/5+32 = 9k+32 für tempC=5k.
    @ParameterizedTest(name = "{0}°C + {1}% Feuchte → Level {2}")
    @CsvSource({
            "20.0, 50, 0",
            "40.0, 45, 0",
            "40.0, 46, 1",
            "40.0, 55, 1",
            "40.0, 56, 2",
            "40.0, 75, 2",
            "40.0, 76, 3"
    })
    void riskLevel_matchesTempHumidityThresholds(double tempC, int humidity, int expected) {
        assertThat(WeatherService.riskLevel(tempC, humidity)).isEqualTo(expected);
    }
}
```

- [ ] **Step 4: Tests laufen lassen**

Run: `./mvnw -q -Dtest=WeatherServiceTest test`
Expected: BUILD SUCCESS, 7 Tests grün.

- [ ] **Step 5: Build sicherstellen (kein anderer Aufrufer des alten 5-Parameter-Konstruktors)**

Run: `./mvnw -q -DskipTests compile`
Expected: BUILD SUCCESS (bestätigt, dass keine andere Stelle den alten `WeatherDto`-Konstruktor nutzt).

- [ ] **Step 6: Commit**

```bash
git add src/main/java/de/wlad/kiratracker/WeatherDto.java src/main/java/de/wlad/kiratracker/WeatherService.java src/test/java/de/wlad/kiratracker/WeatherServiceTest.java
git commit -m "feat(weather): Hitze-Risiko-Level aus Temp+Feuchte berechnen"
```

---

### Task 2: Forecast-DTOs

**Files:**
- Create: `src/main/java/de/wlad/kiratracker/ForecastPointDto.java`
- Create: `src/main/java/de/wlad/kiratracker/WeatherWindowDto.java`
- Create: `src/main/java/de/wlad/kiratracker/WeatherForecastDto.java`

**Interfaces:**
- Consumes: nichts (reine Datenklassen).
- Produces: `ForecastPointDto(String time, double temperature, int humidity, int riskLevel)` mit Gettern `getTime()/getTemperature()/getHumidity()/getRiskLevel()`. `WeatherWindowDto(String start, String end)` mit `getStart()/getEnd()`. `WeatherForecastDto(List<ForecastPointDto> points, int maxRiskLevel, WeatherWindowDto morningWindow, WeatherWindowDto eveningWindow)` mit `getPoints()/getMaxRiskLevel()/getMorningWindow()/getEveningWindow()`. Diese Typen werden von Task 3 (`WeatherService`), Task 4 (`WeatherController`) und Task 5 (`ReminderService`) verwendet.

- [ ] **Step 1: `ForecastPointDto` anlegen**

Erstelle `src/main/java/de/wlad/kiratracker/ForecastPointDto.java`:

```java
package de.wlad.kiratracker;

public class ForecastPointDto {
    private String time;
    private double temperature;
    private int humidity;
    private int riskLevel;

    public ForecastPointDto() {
    }

    public ForecastPointDto(String time, double temperature, int humidity, int riskLevel) {
        this.time = time;
        this.temperature = temperature;
        this.humidity = humidity;
        this.riskLevel = riskLevel;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public int getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(int riskLevel) {
        this.riskLevel = riskLevel;
    }
}
```

- [ ] **Step 2: `WeatherWindowDto` anlegen**

Erstelle `src/main/java/de/wlad/kiratracker/WeatherWindowDto.java`:

```java
package de.wlad.kiratracker;

public class WeatherWindowDto {
    private String start;
    private String end;

    public WeatherWindowDto() {
    }

    public WeatherWindowDto(String start, String end) {
        this.start = start;
        this.end = end;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }
}
```

- [ ] **Step 3: `WeatherForecastDto` anlegen**

Erstelle `src/main/java/de/wlad/kiratracker/WeatherForecastDto.java`:

```java
package de.wlad.kiratracker;

import java.util.List;

public class WeatherForecastDto {
    private List<ForecastPointDto> points;
    private int maxRiskLevel;
    private WeatherWindowDto morningWindow;
    private WeatherWindowDto eveningWindow;

    public WeatherForecastDto() {
    }

    public WeatherForecastDto(List<ForecastPointDto> points, int maxRiskLevel,
                               WeatherWindowDto morningWindow, WeatherWindowDto eveningWindow) {
        this.points = points;
        this.maxRiskLevel = maxRiskLevel;
        this.morningWindow = morningWindow;
        this.eveningWindow = eveningWindow;
    }

    public List<ForecastPointDto> getPoints() {
        return points;
    }

    public void setPoints(List<ForecastPointDto> points) {
        this.points = points;
    }

    public int getMaxRiskLevel() {
        return maxRiskLevel;
    }

    public void setMaxRiskLevel(int maxRiskLevel) {
        this.maxRiskLevel = maxRiskLevel;
    }

    public WeatherWindowDto getMorningWindow() {
        return morningWindow;
    }

    public void setMorningWindow(WeatherWindowDto morningWindow) {
        this.morningWindow = morningWindow;
    }

    public WeatherWindowDto getEveningWindow() {
        return eveningWindow;
    }

    public void setEveningWindow(WeatherWindowDto eveningWindow) {
        this.eveningWindow = eveningWindow;
    }
}
```

- [ ] **Step 4: Build sicherstellen**

Run: `./mvnw -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/de/wlad/kiratracker/ForecastPointDto.java src/main/java/de/wlad/kiratracker/WeatherWindowDto.java src/main/java/de/wlad/kiratracker/WeatherForecastDto.java
git commit -m "feat(weather): DTOs für Tagesverlauf-Forecast"
```

---

### Task 3: Forecast-Abruf + Zeitfenster-Logik in `WeatherService`

**Files:**
- Modify: `src/main/java/de/wlad/kiratracker/WeatherService.java`
- Test: `src/test/java/de/wlad/kiratracker/WeatherServiceTest.java`

**Interfaces:**
- Consumes: `ForecastPointDto`, `WeatherWindowDto`, `WeatherForecastDto` (Task 2); `WeatherService.riskLevel()` (Task 1).
- Produces: `WeatherService.getTodayForecast() -> WeatherForecastDto` (Instanzmethode, ruft OpenWeatherMap `/forecast` ab). `WeatherService.buildForecast(List<ForecastPointDto>) -> WeatherForecastDto` (package-private static, reine Fensterlogik ohne Netzwerkzugriff — von Task 5 und den Tests dieser Task direkt genutzt).

- [ ] **Step 1: Forecast-Methoden zu `WeatherService` hinzufügen**

Füge in `src/main/java/de/wlad/kiratracker/WeatherService.java` die Imports

```java
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
```

zu den bestehenden Imports hinzu, füge die Konstanten

```java
private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
```

direkt nach der Klassendeklaration (vor den `@Value`-Feldern) ein, und hänge folgende Methoden ans Ende der Klasse (nach `getDefaultWeather()`, vor der schließenden `}`):

```java
    /**
     * Holt die 3h-Vorhersage-Slots von heute (Europe/Berlin) und berechnet
     * daraus die Ampel je Slot sowie die besten Gassi-Zeitfenster.
     */
    @SuppressWarnings("unchecked")
    public WeatherForecastDto getTodayForecast() {
        try {
            String url = String.format("%s?q=%s,%s&appid=%s&units=metric&lang=de",
                    forecastUrl(), city, country, apiKey);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return buildForecast(List.of());

            List<Map<String, Object>> list = (List<Map<String, Object>>) response.get("list");
            if (list == null) return buildForecast(List.of());

            LocalDate today = LocalDate.now(BERLIN);
            List<ForecastPointDto> points = new ArrayList<>();
            for (Map<String, Object> item : list) {
                long dt = ((Number) item.get("dt")).longValue();
                ZonedDateTime zdt = Instant.ofEpochSecond(dt).atZone(BERLIN);
                if (!zdt.toLocalDate().equals(today)) continue;

                Map<String, Object> main = (Map<String, Object>) item.get("main");
                double temp = ((Number) main.get("temp")).doubleValue();
                int humidity = ((Number) main.get("humidity")).intValue();

                points.add(new ForecastPointDto(zdt.format(HM), temp, humidity, riskLevel(temp, humidity)));
            }
            return buildForecast(points);
        } catch (RestClientException | NullPointerException | ClassCastException e) {
            return buildForecast(List.of());
        }
    }

    private String forecastUrl() {
        return apiUrl.replace("/weather", "/forecast");
    }

    /**
     * Reine Berechnung aus bereits gemappten Punkten (kein Netzwerkzugriff,
     * daher direkt testbar). Vormittag = Slots vor 12:00, Abend = Slots ab
     * 17:00. Gesucht wird je Bereich der Slot mit dem niedrigsten Risiko
     * (Vormittag: frühester Treffer, Abend: spätester Treffer). Bleibt das
     * Minimum im Bereich Level 3, gibt es kein sicheres Fenster (null).
     */
    static WeatherForecastDto buildForecast(List<ForecastPointDto> points) {
        int maxRisk = points.stream().mapToInt(ForecastPointDto::getRiskLevel).max().orElse(0);

        List<ForecastPointDto> morning = points.stream()
                .filter(p -> hour(p.getTime()) < 12)
                .collect(java.util.stream.Collectors.toList());
        List<ForecastPointDto> evening = points.stream()
                .filter(p -> hour(p.getTime()) >= 17)
                .collect(java.util.stream.Collectors.toList());

        WeatherWindowDto morningWindow = bestWindow(morning, true);
        WeatherWindowDto eveningWindow = bestWindow(evening, false);

        return new WeatherForecastDto(points, maxRisk, morningWindow, eveningWindow);
    }

    private static int hour(String hm) {
        return Integer.parseInt(hm.split(":")[0]);
    }

    private static WeatherWindowDto bestWindow(List<ForecastPointDto> slots, boolean earliest) {
        if (slots.isEmpty()) return null;
        int min = slots.stream().mapToInt(ForecastPointDto::getRiskLevel).min().orElse(3);
        if (min >= 3) return null;

        List<ForecastPointDto> candidates = slots.stream()
                .filter(p -> p.getRiskLevel() == min)
                .collect(java.util.stream.Collectors.toList());
        ForecastPointDto chosen = earliest ? candidates.get(0) : candidates.get(candidates.size() - 1);

        LocalTime start = LocalTime.parse(chosen.getTime());
        LocalTime end = start.plusHours(3);
        return new WeatherWindowDto(chosen.getTime(), end.format(HM));
    }
```

- [ ] **Step 2: Fensterlogik-Tests zu `WeatherServiceTest` hinzufügen**

Füge in `src/test/java/de/wlad/kiratracker/WeatherServiceTest.java` den Import

```java
import java.util.List;
```

hinzu und ergänze folgende Tests in der Klasse (nach der bestehenden `riskLevel_matchesTempHumidityThresholds`-Methode):

```java
    @org.junit.jupiter.api.Test
    void buildForecast_picksEarliestLowestRiskInMorning_andLatestInEvening() {
        List<ForecastPointDto> points = List.of(
                new ForecastPointDto("07:00", 18, 60, 0),
                new ForecastPointDto("10:00", 28, 70, 1),
                new ForecastPointDto("13:00", 34, 80, 3),
                new ForecastPointDto("16:00", 34, 80, 3),
                new ForecastPointDto("19:00", 30, 75, 1),
                new ForecastPointDto("22:00", 24, 65, 0)
        );

        WeatherForecastDto forecast = WeatherService.buildForecast(points);

        assertThat(forecast.getMaxRiskLevel()).isEqualTo(3);
        assertThat(forecast.getMorningWindow().getStart()).isEqualTo("07:00");
        assertThat(forecast.getMorningWindow().getEnd()).isEqualTo("10:00");
        assertThat(forecast.getEveningWindow().getStart()).isEqualTo("22:00");
        assertThat(forecast.getEveningWindow().getEnd()).isEqualTo("01:00");
    }

    @org.junit.jupiter.api.Test
    void buildForecast_returnsNullWindow_whenNoSafeSlotInRange() {
        List<ForecastPointDto> points = List.of(
                new ForecastPointDto("07:00", 34, 80, 3),
                new ForecastPointDto("10:00", 35, 82, 3),
                new ForecastPointDto("19:00", 33, 79, 3)
        );

        WeatherForecastDto forecast = WeatherService.buildForecast(points);

        assertThat(forecast.getMaxRiskLevel()).isEqualTo(3);
        assertThat(forecast.getMorningWindow()).isNull();
        assertThat(forecast.getEveningWindow()).isNull();
    }

    @org.junit.jupiter.api.Test
    void buildForecast_handlesEmptyPoints() {
        WeatherForecastDto forecast = WeatherService.buildForecast(List.of());

        assertThat(forecast.getMaxRiskLevel()).isEqualTo(0);
        assertThat(forecast.getMorningWindow()).isNull();
        assertThat(forecast.getEveningWindow()).isNull();
    }
```

- [ ] **Step 3: Tests laufen lassen**

Run: `./mvnw -q -Dtest=WeatherServiceTest test`
Expected: BUILD SUCCESS, 10 Tests grün (7 aus Task 1 + 3 neue).

- [ ] **Step 4: Commit**

```bash
git add src/main/java/de/wlad/kiratracker/WeatherService.java src/test/java/de/wlad/kiratracker/WeatherServiceTest.java
git commit -m "feat(weather): Tagesverlauf-Forecast mit Zeitfenster-Logik"
```

---

### Task 4: `GET /weather/forecast` Endpoint

**Files:**
- Create: `src/main/java/de/wlad/kiratracker/WeatherController.java`
- Modify: `src/test/java/de/wlad/kiratracker/SmokeIntegrationTest.java`

**Interfaces:**
- Consumes: `WeatherService.getTodayForecast()` (Task 3).
- Produces: `GET /weather/forecast -> 200 WeatherForecastDto` (JSON). Wird von Task 5 (Push-Text, serverseitig direkt über `WeatherService`) und Task 8 (Frontend-Chart) konsumiert.

- [ ] **Step 1: `WeatherController` anlegen**

Erstelle `src/main/java/de/wlad/kiratracker/WeatherController.java`:

```java
package de.wlad.kiratracker;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather/forecast")
    public ResponseEntity<WeatherForecastDto> forecast() {
        return ResponseEntity.ok(weatherService.getTodayForecast());
    }
}
```

- [ ] **Step 2: Integrationstest ergänzen — Endpoint antwortet auch wenn die Wetter-API nicht erreichbar ist**

Füge in `src/test/java/de/wlad/kiratracker/SmokeIntegrationTest.java` folgenden Test hinzu (nach `statusAndLeaderboardRespond()`); die Testkonfiguration (`application-test.yml`) zeigt `weather.api.url` bereits auf `http://localhost:0/weather`, das schnell fehlschlägt:

```java
    @Test
    @SuppressWarnings("unchecked")
    void weatherForecastRespondsEvenWhenApiUnreachable() {
        ResponseEntity<Map<String,Object>> res = rest.exchange(
                "/weather/forecast", HttpMethod.GET, null,
                new org.springframework.core.ParameterizedTypeReference<Map<String,Object>>() {});
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().get("maxRiskLevel")).isEqualTo(0);
        assertThat((List<Object>) res.getBody().get("points")).isEmpty();
    }
```

- [ ] **Step 3: Test laufen lassen**

Run: `./mvnw -q -Dtest=SmokeIntegrationTest test`
Expected: BUILD SUCCESS, alle Smoke-Tests grün (inkl. neuem).

- [ ] **Step 4: Manueller Smoke-Check**

Run: `./mvnw -q spring-boot:run &` dann `curl -s localhost:8080/weather/forecast` (mit `application-test.yml`-artigen Dummy-Env-Vars falls lokal ohne Postgres nicht startbar — sonst reicht der Integrationstest aus Step 3 als Nachweis).
Expected: JSON mit `points`, `maxRiskLevel`, `morningWindow`, `eveningWindow`.

- [ ] **Step 5: Commit**

```bash
git add src/main/java/de/wlad/kiratracker/WeatherController.java src/test/java/de/wlad/kiratracker/SmokeIntegrationTest.java
git commit -m "feat(weather): GET /weather/forecast Endpoint"
```

---

### Task 5: 6-Uhr-Push bei heftigen Tagen

**Files:**
- Modify: `src/main/java/de/wlad/kiratracker/ReminderService.java`
- Modify: `src/test/java/de/wlad/kiratracker/ReminderServiceTest.java`

**Interfaces:**
- Consumes: `WeatherService.getTodayForecast() -> WeatherForecastDto` (Task 3), `NotificationService.sendCustomNotification(String)` (bestehend), `PauseRepository.findById(Long)` (bestehend).
- Produces: `ReminderService.checkHeatWarning()` (neue `@Scheduled`-Methode, cron `0 0 6 * * *`, Zone `Europe/Berlin`).

- [ ] **Step 1: `ReminderService` um Konstruktor-Parameter und Cron-Methode erweitern**

Ersetze den kompletten Inhalt von `src/main/java/de/wlad/kiratracker/ReminderService.java`:

```java
package de.wlad.kiratracker;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled reminders. Separated from NotificationService to break the
 * WalkService → NotificationService → WalkService circular dependency.
 */
@Service
public class ReminderService {

    private final WalkService walkService;
    private final NotificationService notificationService;
    private final PauseRepository pauseRepository;
    private final WeatherService weatherService;

    public ReminderService(WalkService walkService, NotificationService notificationService,
                           PauseRepository pauseRepository, WeatherService weatherService) {
        this.walkService = walkService;
        this.notificationService = notificationService;
        this.pauseRepository = pauseRepository;
        this.weatherService = weatherService;
    }

    @Scheduled(cron = "0 0 11 * * *", zone = "Europe/Berlin")
    public void checkMorningReminder() {
        if (walkService.wasMorning()) return;
        Integer pauseIndex = getPauseIndex();
        if (pauseIndex == null) {
            notificationService.sendCustomNotification(
                    "☀️ War jemand schon mit Kira raus oder hat es jemand vergessen einzutragen?");
        }
        // Bei aktivem Urlaub (pauseIndex != null): keine Erinnerung.
    }

    @Scheduled(cron = "0 0 21 * * *", zone = "Europe/Berlin")
    public void aaronBedtime() {
        notificationService.sendCustomNotification("Schlafenszeit für Aaron, ab ins Bett!");
    }

    @Scheduled(cron = "0 0 22 * * *", zone = "Europe/Berlin")
    public void checkEveningReminder() {
        if (walkService.wasEvening()) return;
        Integer pauseIndex = getPauseIndex();
        if (pauseIndex == null) {
            notificationService.sendCustomNotification(
                    "🌙 War jemand schon mit Kira raus oder hat es jemand vergessen einzutragen?");
        }
        // Bei aktivem Urlaub (pauseIndex != null): keine Erinnerung.
    }

    /**
     * Warnt an heftigen Tagen (Ampel-Level 3) morgens um 6 Uhr mit den besten
     * Gassi-Zeitfenstern. Übersprungen im Urlaubsmodus.
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "Europe/Berlin")
    public void checkHeatWarning() {
        if (getPauseIndex() != null) return;

        WeatherForecastDto forecast = weatherService.getTodayForecast();
        if (forecast.getMaxRiskLevel() < 3) return;

        StringBuilder windows = new StringBuilder();
        if (forecast.getMorningWindow() != null) {
            windows.append(forecast.getMorningWindow().getStart())
                   .append("–").append(forecast.getMorningWindow().getEnd());
        }
        if (forecast.getEveningWindow() != null) {
            if (windows.length() > 0) windows.append(" und ");
            windows.append(forecast.getEveningWindow().getStart())
                   .append("–").append(forecast.getEveningWindow().getEnd());
        }

        String message = windows.length() > 0
                ? "🌡️ Heute wird's heiß für Kira — beste Zeiten: " + windows
                : "🌡️ Heute wird's sehr heiß für Kira — heute lieber nur kurze, schattige Runden.";
        notificationService.sendCustomNotification(message);
    }

    private Integer getPauseIndex() {
        return pauseRepository.findById(1L)
                .orElse(new PauseState())
                .getPauseIndex();
    }
}
```

- [ ] **Step 2: `ReminderServiceTest` um `WeatherService`-Mock und Heat-Warning-Tests erweitern**

Ersetze den kompletten Inhalt von `src/test/java/de/wlad/kiratracker/ReminderServiceTest.java`:

```java
package de.wlad.kiratracker;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReminderServiceTest {

    private final WalkService walkService = mock(WalkService.class);
    private final NotificationService notificationService = mock(NotificationService.class);
    private final PauseRepository pauseRepository = mock(PauseRepository.class);
    private final WeatherService weatherService = mock(WeatherService.class);

    private ReminderService service() {
        return new ReminderService(walkService, notificationService, pauseRepository, weatherService);
    }

    private void pause(Integer index) {
        PauseState state = new PauseState();
        state.setPauseIndex(index);
        when(pauseRepository.findById(1L)).thenReturn(Optional.of(state));
    }

    @Test
    void morningReminder_sendsOneMessage_whenNotWalkedAndNoPause() {
        when(walkService.wasMorning()).thenReturn(false);
        pause(null);

        service().checkMorningReminder();

        verify(notificationService, times(1)).sendCustomNotification(anyString());
    }

    @Test
    void morningReminder_silent_whenAlreadyWalked() {
        when(walkService.wasMorning()).thenReturn(true);

        service().checkMorningReminder();

        verifyNoInteractions(notificationService);
    }

    @Test
    void morningReminder_silent_whenPaused() {
        when(walkService.wasMorning()).thenReturn(false);
        pause(0);

        service().checkMorningReminder();

        verifyNoInteractions(notificationService);
    }

    @Test
    void eveningReminder_sendsOneMessage_whenNotWalkedAndNoPause() {
        when(walkService.wasEvening()).thenReturn(false);
        pause(null);

        service().checkEveningReminder();

        verify(notificationService, times(1)).sendCustomNotification(anyString());
    }

    @Test
    void eveningReminder_silent_whenPaused() {
        when(walkService.wasEvening()).thenReturn(false);
        pause(0);

        service().checkEveningReminder();

        verifyNoInteractions(notificationService);
    }

    @Test
    void heatWarning_sendsMessageWithWindows_whenRedAndNoPause() {
        pause(null);
        WeatherWindowDto morning = new WeatherWindowDto("07:00", "10:00");
        WeatherWindowDto evening = new WeatherWindowDto("20:00", "22:00");
        when(weatherService.getTodayForecast())
                .thenReturn(new WeatherForecastDto(List.of(), 3, morning, evening));

        service().checkHeatWarning();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(1)).sendCustomNotification(captor.capture());
        assertThat(captor.getValue()).contains("07:00–10:00").contains("20:00–22:00");
    }

    @Test
    void heatWarning_sendsShortRoundsMessage_whenNoSafeWindow() {
        pause(null);
        when(weatherService.getTodayForecast())
                .thenReturn(new WeatherForecastDto(List.of(), 3, null, null));

        service().checkHeatWarning();

        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(notificationService, times(1)).sendCustomNotification(captor.capture());
        assertThat(captor.getValue()).contains("kurze, schattige Runden");
    }

    @Test
    void heatWarning_silent_whenBelowRed() {
        pause(null);
        when(weatherService.getTodayForecast())
                .thenReturn(new WeatherForecastDto(List.of(), 2, null, null));

        service().checkHeatWarning();

        verifyNoInteractions(notificationService);
    }

    @Test
    void heatWarning_silent_whenPaused() {
        pause(0);

        service().checkHeatWarning();

        verifyNoInteractions(notificationService);
        verifyNoInteractions(weatherService);
    }
}
```

- [ ] **Step 3: Tests laufen lassen**

Run: `./mvnw -q -Dtest=ReminderServiceTest test`
Expected: BUILD SUCCESS, 9 Tests grün.

- [ ] **Step 4: Commit**

```bash
git add src/main/java/de/wlad/kiratracker/ReminderService.java src/test/java/de/wlad/kiratracker/ReminderServiceTest.java
git commit -m "feat(reminder): 6-Uhr-Push mit Gassi-Zeitfenstern an heftigen Hitzetagen"
```

---

### Task 6: Backend-Gesamtlauf verifizieren

**Files:**
- (keine Änderungen — reiner Verifikationsschritt vor dem Frontend)

**Interfaces:**
- Consumes: alle Ergebnisse aus Task 1–5.
- Produces: nichts — Gate vor dem Frontend.

- [ ] **Step 1: Vollständige Testsuite laufen lassen**

Run: `./mvnw -q test`
Expected: BUILD SUCCESS, keine Fehler in `WeatherServiceTest`, `ReminderServiceTest`, `SmokeIntegrationTest`, `FairnessServiceTest`, `WalkBlockServiceTest`, `KiratrackerApplicationTests`.

- [ ] **Step 2: Paket-Build**

Run: `./mvnw -q -DskipTests package`
Expected: BUILD SUCCESS, `target/*.jar` erzeugt.

(Kein Commit — reiner Verifikationsschritt.)

---

### Task 7: Frontend — Ampel-Punkt + Popover auf der Startseite

**Files:**
- Modify: `src/main/resources/static/index.html`

**Interfaces:**
- Consumes: `STATE.status.weather.riskLevel` (int 0–3, aus `GET /status`, Task 1).
- Produces: sichtbarer Ampel-Punkt (`#wxRisk`) + Popover (`#wxPop`) auf Home; Tap auf Icon/Temperatur/Beschreibung navigiert zu `/weather.html` (Task 8).

- [ ] **Step 1: CSS-Tokens für die Ampel-Farben ergänzen**

In `src/main/resources/static/index.html`, im `<style>`-Block, ergänze die `--wxc0..3`-Variablen in `:root` und `.dark`:

Ersetze:
```css
:root{
  --bg:#ececef;--card:#ffffff;--ink:#0d0d0f;--ink2:#6c6c74;--ink3:#a3a3ab;
  --line:#e4e4e8;--fill:#0d0d0f;--onfill:#ffffff;--soft:#f4f4f6;
}
.dark{
  --bg:#000000;--card:#151517;--ink:#f4f4f6;--ink2:#9b9ba3;--ink3:#65656d;
  --line:#262629;--fill:#f4f4f6;--onfill:#0d0d0f;--soft:#1d1d20;
}
```

durch:
```css
:root{
  --bg:#ececef;--card:#ffffff;--ink:#0d0d0f;--ink2:#6c6c74;--ink3:#a3a3ab;
  --line:#e4e4e8;--fill:#0d0d0f;--onfill:#ffffff;--soft:#f4f4f6;
  --wxc0:#3c8a5c;--wxc1:#c9a227;--wxc2:#c97a27;--wxc3:#9a3636;
}
.dark{
  --bg:#000000;--card:#151517;--ink:#f4f4f6;--ink2:#9b9ba3;--ink3:#65656d;
  --line:#262629;--fill:#f4f4f6;--onfill:#0d0d0f;--soft:#1d1d20;
  --wxc0:#5fae7f;--wxc1:#e0c157;--wxc2:#e0a157;--wxc3:#e6a5a5;
}
```

- [ ] **Step 2: CSS für Ampel-Punkt und Popover ergänzen**

Ersetze im selben `<style>`-Block:
```css
.wx-top{display:flex;align-items:center;gap:8px;min-width:0;color:var(--ink2);font-size:.88rem;font-weight:500}
.wx-top svg{display:block;flex-shrink:0}
.wx-top .wt{font-weight:800;color:var(--ink);font-size:.94rem}
.wx-top .wd{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
```

durch:
```css
.wx-top{display:flex;align-items:center;gap:8px;min-width:0;color:var(--ink2);font-size:.88rem;font-weight:500;position:relative}
.wx-top svg{display:block;flex-shrink:0}
.wx-top .wt{font-weight:800;color:var(--ink);font-size:.94rem}
.wx-top .wd{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.wx-tap{display:flex;align-items:center;gap:8px;min-width:0;padding:0;text-align:left}
.wx-risk{width:8px;height:8px;border-radius:50%;padding:0;flex-shrink:0;display:none}
.wx-risk[data-level="0"]{background:var(--wxc0)}
.wx-risk[data-level="1"]{background:var(--wxc1)}
.wx-risk[data-level="2"]{background:var(--wxc2)}
.wx-risk[data-level="3"]{background:var(--wxc3)}
.wx-pop{display:none;position:absolute;top:26px;left:0;z-index:60;background:var(--card);border:1px solid var(--line);border-radius:16px;padding:12px 14px;font-size:.76rem;font-weight:600;color:var(--ink2);width:230px;box-shadow:0 12px 30px rgba(0,0,0,.18)}
.wx-pop.open{display:block}
.wx-pop .wpr{display:flex;align-items:center;gap:8px;padding:5px 0}
.wx-pop .wpr i{width:8px;height:8px;border-radius:50%;flex-shrink:0}
.wx-pop .wpr i.c0{background:var(--wxc0)}
.wx-pop .wpr i.c1{background:var(--wxc1)}
.wx-pop .wpr i.c2{background:var(--wxc2)}
.wx-pop .wpr i.c3{background:var(--wxc3)}
.wx-pop .wpr.cur{color:var(--ink);font-weight:800}
```

- [ ] **Step 3: Markup anpassen — Tap-Ziel + Ampel-Punkt + Popover-Container**

Ersetze:
```html
        <div class="wx-top">
          <svg width="22" height="16" viewBox="0 0 22 16" fill="none"><path d="M6 13h9a4 4 0 0 0 .5-7.97A5 5 0 0 0 6 6.5 3.25 3.25 0 0 0 6 13Z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/></svg>
          <span class="wt" id="wt">–°</span><span class="wd" id="wd">…</span>
        </div>
```

durch:
```html
        <div class="wx-top">
          <button class="wx-tap" onclick="location.href='/weather.html'" aria-label="Wetter-Details für Kira">
            <svg width="22" height="16" viewBox="0 0 22 16" fill="none"><path d="M6 13h9a4 4 0 0 0 .5-7.97A5 5 0 0 0 6 6.5 3.25 3.25 0 0 0 6 13Z" stroke="currentColor" stroke-width="1.3" stroke-linejoin="round"/></svg>
            <span class="wt" id="wt">–°</span><span class="wd" id="wd">…</span>
          </button>
          <button class="wx-risk" id="wxRisk" data-level="0" onclick="toggleWxPop(event)" aria-label="Hitze-Ampel für Kira"></button>
          <div class="wx-pop" id="wxPop"></div>
        </div>
```

- [ ] **Step 4: JS — `paintWeather()` um Ampel-Punkt erweitern, Popover-Logik ergänzen**

Ersetze:
```js
function paintWeather(){
  const w=STATE.status&&STATE.status.weather;
  if(w){$('wt').textContent=Math.round(w.temperature)+'°';$('wd').textContent=w.description||'';}
  else{$('wt').textContent='';$('wd').textContent='';}
}
```

durch:
```js
const WX_LEVELS=[
  {label:'unbedenklich',desc:'Normale Runden ohne Einschränkung.'},
  {label:'Vorsicht',desc:'Wasser mitnehmen, kürzere Runden im Schatten.'},
  {label:'gefährlich',desc:'Nur kurze Runden, direkte Sonne meiden.'},
  {label:'potenziell lebensgefährlich',desc:'Wenn möglich nur früh morgens oder spät abends raus.'}
];
function paintWeather(){
  const w=STATE.status&&STATE.status.weather;
  if(w){
    $('wt').textContent=Math.round(w.temperature)+'°';$('wd').textContent=w.description||'';
    $('wxRisk').style.display='block';$('wxRisk').dataset.level=w.riskLevel;
    renderWxPop(w.riskLevel);
  }else{
    $('wt').textContent='';$('wd').textContent='';
    $('wxRisk').style.display='none';closeWxPop();
  }
}
function renderWxPop(level){
  $('wxPop').innerHTML=WX_LEVELS.map((l,i)=>
    `<div class="wpr ${i===level?'cur':''}"><i class="c${i}"></i>${l.label} — ${l.desc}</div>`
  ).join('');
}
function toggleWxPop(e){e.stopPropagation();$('wxPop').classList.toggle('open')}
function closeWxPop(){$('wxPop').classList.remove('open')}
document.addEventListener('click',e=>{
  if(!e.target.closest('#wxPop')&&!e.target.closest('#wxRisk'))closeWxPop();
});
```

- [ ] **Step 5: Manuell im Browser verifizieren**

Run: App lokal starten (siehe `docs/RUNBOOK.md`, H2/Testprofil reicht: `SPRING_PROFILES_ACTIVE=test ./mvnw spring-boot:run` — oder Preview-Tool nutzen) und `/index.html` öffnen.
Expected: Ampel-Punkt neben dem Wetter sichtbar (Farbe je nach `riskLevel` aus `/status`), Tap auf den Punkt öffnet die Popover-Box mit 4 Zeilen (aktuelle hervorgehoben), Tap daneben schließt sie, Tap auf Icon/Temperatur/Beschreibung navigiert zu `/weather.html` (404 bis Task 8 abgeschlossen ist — das ist an dieser Stelle erwartet).

- [ ] **Step 6: Commit**

```bash
git add src/main/resources/static/index.html
git commit -m "feat(ui): Hitze-Ampel-Punkt mit Info-Popover neben dem Wetter"
```

---

### Task 8: Frontend — neue Seite `weather.html` (Tagesverlauf + Empfehlung)

**Files:**
- Create: `src/main/resources/static/weather.html`

**Interfaces:**
- Consumes: `GET /weather/forecast` (Task 4) → `{points, maxRiskLevel, morningWindow, eveningWindow}`.
- Produces: eigenständige Seite, erreichbar über `/weather.html`, verlinkt von Task 7 (`wx-tap`-Button in `index.html`).

- [ ] **Step 1: `weather.html` anlegen**

Erstelle `src/main/resources/static/weather.html`:

```html
<!DOCTYPE html>
<html lang="de">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>heute — kiratracker</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800;900&display=swap" rel="stylesheet">
<style>
:root{--bg:#ececef;--card:#ffffff;--ink:#0d0d0f;--ink2:#6c6c74;--ink3:#a3a3ab;--line:#e4e4e8;--fill:#0d0d0f;--onfill:#ffffff;--soft:#f4f4f6;--wxc0:#3c8a5c;--wxc1:#c9a227;--wxc2:#c97a27;--wxc3:#9a3636}
.dark{--bg:#000000;--card:#151517;--ink:#f4f4f6;--ink2:#9b9ba3;--ink3:#65656d;--line:#262629;--fill:#f4f4f6;--onfill:#0d0d0f;--soft:#1d1d20;--wxc0:#5fae7f;--wxc1:#e0c157;--wxc2:#e0a157;--wxc3:#e6a5a5}
*{margin:0;padding:0;box-sizing:border-box;-webkit-tap-highlight-color:transparent}
body{background:var(--bg);color:var(--ink);font-family:"Inter",-apple-system,system-ui,sans-serif;-webkit-font-smoothing:antialiased;min-height:100vh;transition:background .4s,color .4s}
button{font:inherit;color:inherit;background:none;border:0;cursor:pointer}
a{color:inherit}
.wrap{max-width:560px;margin:0 auto;padding:28px 22px 80px}

.top{display:flex;align-items:center;justify-content:space-between;margin-bottom:6px}
.back{display:inline-flex;align-items:center;gap:7px;color:var(--ink2);font-weight:700;font-size:.9rem;text-decoration:none}
.back:active{color:var(--ink)}
.icbtn{color:var(--ink2);display:flex}.icbtn:active{color:var(--ink)}
h1.title{font-size:2.15rem;font-weight:800;letter-spacing:-.045em;line-height:1.02;margin:8px 0 2px}
.sub{color:var(--ink2);font-weight:500;font-size:.95rem;margin-bottom:6px}

.eyebrow{font-size:.7rem;font-weight:700;letter-spacing:.06em;text-transform:uppercase;color:var(--ink3);margin:0 0 12px}
.chartcard{background:var(--card);border-radius:24px;padding:22px 18px;margin-top:22px}
.chartcard svg{display:block;width:100%;height:auto}
.chart-axis text{font-size:9px;fill:var(--ink3);font-family:"Inter",sans-serif;font-weight:600}
.legend{display:flex;gap:14px;flex-wrap:wrap;margin-top:14px;font-size:.72rem;font-weight:600;color:var(--ink2)}
.legend span{display:inline-flex;align-items:center;gap:5px}
.legend i{width:8px;height:8px;border-radius:50%}

.rec{background:var(--card);border-radius:24px;padding:20px 18px;margin-top:16px}
.pills{display:flex;flex-wrap:wrap;gap:8px}
.pill{padding:9px 16px;border-radius:100px;background:var(--soft);font-weight:700;font-size:.86rem;color:var(--ink)}
.none{color:var(--ink3);font-weight:600;font-size:.88rem;line-height:1.4}
.empty{color:var(--ink3);font-weight:600;font-size:.86rem;padding:18px 2px}
</style>
</head>
<body>
<div class="wrap">
  <div class="top">
    <a class="back" href="/index.html">‹ zurück</a>
    <button class="icbtn" onclick="toggleDark()" aria-label="Theme">
      <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M16 11.5A6.5 6.5 0 0 1 8.5 4c0-.5.06-1 .17-1.46A7 7 0 1 0 17.5 11.3c-.47.13-.97.2-1.5.2Z" stroke="currentColor" stroke-width="1.4" stroke-linejoin="round"/></svg>
    </button>
  </div>
  <h1 class="title">heute.</h1>
  <div class="sub" id="stageText">lädt …</div>

  <div class="eyebrow" style="margin-top:22px">Tagesverlauf</div>
  <div class="chartcard" id="chartCard"><div class="empty">Lade Tagesverlauf …</div></div>

  <div class="eyebrow" style="margin-top:22px">Empfehlung</div>
  <div class="rec" id="recCard"><div class="none">lädt …</div></div>
</div>

<script>
const $=id=>document.getElementById(id);
const STAGES=['unbedenklich','Vorsicht','gefährlich','potenziell lebensgefährlich'];
function toggleDark(){const d=document.body.classList.toggle('dark');localStorage.setItem('mk3-dark',d?'1':'0')}
if(localStorage.getItem('mk3-dark')==='1')document.body.classList.add('dark');

async function load(){
  try{
    const r=await fetch('/weather/forecast');
    if(!r.ok)throw new Error('HTTP '+r.status);
    render(await r.json());
  }catch(e){
    $('stageText').textContent='Vorhersage nicht verfügbar';
    $('chartCard').innerHTML='<div class="empty">Tagesverlauf konnte nicht geladen werden.</div>';
    $('recCard').innerHTML='<div class="none">Keine Empfehlung verfügbar.</div>';
  }
}

function render(data){
  const points=data.points||[];
  $('stageText').textContent=points.length
    ? ('heute: '+STAGES[data.maxRiskLevel]+' für Kira')
    : 'keine Vorhersagedaten für heute';
  $('chartCard').innerHTML=points.length?chartSvg(points):'<div class="empty">Keine Vorhersagedaten für heute.</div>';
  renderRec(data);
}

function chartSvg(points){
  const W=320,H=160,PAD_L=18,PAD_R=18,PAD_T=14,PAD_B=22;
  const temps=points.map(p=>p.temperature);
  const minT=Math.min(...temps)-2, maxT=Math.max(...temps)+2;
  const x=i=>PAD_L+(W-PAD_L-PAD_R)*(i/(Math.max(points.length-1,1)));
  const y=t=>PAD_T+(H-PAD_T-PAD_B)*(1-(t-minT)/((maxT-minT)||1));

  const linePath=points.map((p,i)=>`${i===0?'M':'L'}${x(i).toFixed(1)},${y(p.temperature).toFixed(1)}`).join(' ');
  const areaPath=linePath+` L${x(points.length-1).toFixed(1)},${(H-PAD_B).toFixed(1)} L${x(0).toFixed(1)},${(H-PAD_B).toFixed(1)} Z`;
  const dots=points.map((p,i)=>`<circle cx="${x(i).toFixed(1)}" cy="${y(p.temperature).toFixed(1)}" r="3.5" fill="var(--wxc${p.riskLevel})"/>`).join('');
  const labels=points.map((p,i)=>i%2===0?`<text x="${x(i).toFixed(1)}" y="${H-4}" text-anchor="middle">${p.time}</text>`:'').join('');

  return `<svg viewBox="0 0 ${W} ${H}" xmlns="http://www.w3.org/2000/svg" class="chart-axis">
    <path d="${areaPath}" fill="var(--soft)"/>
    <path d="${linePath}" fill="none" stroke="var(--ink2)" stroke-width="1.5"/>
    ${dots}
    ${labels}
  </svg>
  <div class="legend">
    <span><i style="background:var(--wxc0)"></i>unbedenklich</span>
    <span><i style="background:var(--wxc1)"></i>Vorsicht</span>
    <span><i style="background:var(--wxc2)"></i>gefährlich</span>
    <span><i style="background:var(--wxc3)"></i>lebensgefährlich</span>
  </div>`;
}

function renderRec(data){
  const pills=[];
  if(data.morningWindow)pills.push(`<span class="pill">${data.morningWindow.start}–${data.morningWindow.end}</span>`);
  if(data.eveningWindow)pills.push(`<span class="pill">${data.eveningWindow.start}–${data.eveningWindow.end}</span>`);
  $('recCard').innerHTML=pills.length
    ? `<div class="pills">${pills.join('')}</div>`
    : `<div class="none">Heute kein sicheres Zeitfenster — wenn möglich, kurze Runden im Schatten.</div>`;
}

load();
</script>
</body>
</html>
```

- [ ] **Step 2: Manuell im Browser verifizieren**

Run: App lokal starten und `/weather.html` direkt öffnen sowie über den Tap auf das Wetter in `/index.html` (Task 7) navigieren.
Expected: Titel „heute.“ + Stufentext, SVG-Kurve mit farbigen Punkten je Ampel-Level, Legende darunter, Empfehlungs-Pillen (oder Hinweistext falls kein Fenster), `‹ zurück` führt zu `/index.html`, Dark-Mode-Toggle synchron mit der restlichen App (`localStorage['mk3-dark']`).

- [ ] **Step 3: Commit**

```bash
git add src/main/resources/static/weather.html
git commit -m "feat(ui): weather.html mit Tagesverlauf-Diagramm und Zeitfenster-Empfehlung"
```

---

### Task 9: Dokumentation aktualisieren

**Files:**
- Modify: `CLAUDE.md`
- Modify: `docs/RUNBOOK.md`

**Interfaces:**
- Consumes: nichts.
- Produces: aktualisierte Doku, konsistent mit dem Rest des Projekts (siehe bestehende „Funktion → Endpoint-Mapping“-Tabelle).

- [ ] **Step 1: `CLAUDE.md` — Endpoint-Mapping-Tabelle ergänzen**

Füge in `CLAUDE.md` in der Tabelle unter „# Funktion → Endpoint-Mapping“ nach der Zeile `| **Fairness** (wer dran, Counts) | ... |` folgende zwei Zeilen ein:

```
| **Hitze-Ampel** (aktuelles Risiko 0–3) | `GET /status` → `weather.riskLevel` | vorhanden |
| **Tagesverlauf + Zeitfenster** | `GET /weather/forecast` | vorhanden |
```

und ergänze nach der Zeile `| Erinnerungs-Push 11:00 / 22:00 | ... |`:

```
| Hitze-Push bei Rot (Level 3), 6:00 Uhr | `ReminderService` Cron | vorhanden |
```

- [ ] **Step 2: `CLAUDE.md` — neue Seite in der Datei-Übersicht erwähnen**

Ersetze im Abschnitt „## Stack & Deployment“:
```
- **Frontend:** statische Seiten unter `src/main/resources/static/`
  (`index.html`, `admin.html`, `stats.html`, `nfc.html`), vom Spring-Server ausgeliefert.
```

durch:
```
- **Frontend:** statische Seiten unter `src/main/resources/static/`
  (`index.html`, `admin.html`, `stats.html`, `nfc.html`, `weather.html`), vom Spring-Server ausgeliefert.
```

- [ ] **Step 3: `docs/RUNBOOK.md` — Smoke-Check und Cron-Hinweis ergänzen**

Füge in `docs/RUNBOOK.md` im Abschnitt „## Smoke-Checks (curl)“ nach `curl -s localhost:8080/fairness` folgende Zeile ein:

```
curl -s localhost:8080/weather/forecast
```

und ersetze im Abschnitt „## Deployment (Render)“:
```
- Reminder-Crons greifen serverseitig (Zone `Europe/Berlin`): 11:00 Morgen, 22:00 Abend.
```

durch:
```
- Reminder-Crons greifen serverseitig (Zone `Europe/Berlin`): 06:00 Hitze-Warnung (nur bei
  Ampel-Level 3), 11:00 Morgen, 22:00 Abend.
```

- [ ] **Step 4: Commit**

```bash
git add CLAUDE.md docs/RUNBOOK.md
git commit -m "docs: Hitze-Warnung in CLAUDE.md und RUNBOOK.md dokumentieren"
```
