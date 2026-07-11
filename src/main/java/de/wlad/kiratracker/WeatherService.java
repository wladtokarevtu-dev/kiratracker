package de.wlad.kiratracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeatherService {

    private static final Logger log = LoggerFactory.getLogger(WeatherService.class);

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");
    private static final String[] WEEKDAYS = {"Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"};
    /** Empfehlungen nie vor 6:00 und nie nach 23:00 (Fenster-Ende wird gekappt). */
    private static final LocalTime DAY_END = LocalTime.of(23, 0);

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
        return new WeatherDto("Wetter nicht verfügbar", 0.0, 0, 0.0, "01d", null);
    }

    /**
     * Vorhersage nur für heute (Europe/Berlin) — genutzt vom 6-Uhr-Hitze-Push.
     */
    public WeatherForecastDto getTodayForecast() {
        LocalDate today = LocalDate.now(BERLIN);
        return buildForecast(fetchDays().getOrDefault(today, List.of()));
    }

    /** So viele Tage zeigt das Wetter-Panel (heute + morgen + übermorgen).
     *  Weiter in die Zukunft ist die Vorhersage ohnehin unzuverlässig. */
    private static final int FORECAST_DAYS = 3;

    /**
     * Mehrtägige Vorhersage (OWM: 5 Tage / 3h, gekappt auf {@link #FORECAST_DAYS})
     * für den Tagesauswahl-Strip im Frontend — je Tag Kurve + Ampel je Slot +
     * Gassi-Zeitfenster.
     */
    public WeekForecastDto getWeekForecast() {
        List<DayForecastDto> days = new ArrayList<>();
        for (Map.Entry<LocalDate, List<ForecastPointDto>> e : fetchDays().entrySet()) {
            if (days.size() >= FORECAST_DAYS) break;
            LocalDate d = e.getKey();
            WeatherForecastDto f = buildForecast(e.getValue());
            days.add(new DayForecastDto(
                    d.toString(),
                    WEEKDAYS[d.getDayOfWeek().getValue() - 1],
                    d.getDayOfMonth(),
                    f.getPoints(), f.getMaxRiskLevel(),
                    f.getMorningWindow(), f.getEveningWindow()));
        }
        return new WeekForecastDto(days);
    }

    /**
     * Holt die 3h-Forecast-Slots und gruppiert sie chronologisch nach Tag
     * (Europe/Berlin). Ein Netzabruf, von {@link #getTodayForecast()} und
     * {@link #getWeekForecast()} geteilt. Bei Fehler: leere Map (kein Werfen).
     */
    @SuppressWarnings("unchecked")
    private Map<LocalDate, List<ForecastPointDto>> fetchDays() {
        Map<LocalDate, List<ForecastPointDto>> byDay = new LinkedHashMap<>();
        try {
            String url = String.format("%s?q=%s,%s&appid=%s&units=metric&lang=de",
                    forecastUrl(), city, country, apiKey);

            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                log.warn("Wetter-Forecast: leere Antwort von {}", forecastUrl());
                return byDay;
            }

            List<Map<String, Object>> list = (List<Map<String, Object>>) response.get("list");
            if (list == null || list.isEmpty()) {
                log.warn("Wetter-Forecast: keine 'list' im Payload von {}", forecastUrl());
                return byDay;
            }

            for (Map<String, Object> item : list) {
                long dt = ((Number) item.get("dt")).longValue();
                ZonedDateTime zdt = Instant.ofEpochSecond(dt).atZone(BERLIN);

                Map<String, Object> main = (Map<String, Object>) item.get("main");
                double temp = ((Number) main.get("temp")).doubleValue();
                int humidity = ((Number) main.get("humidity")).intValue();

                byDay.computeIfAbsent(zdt.toLocalDate(), k -> new ArrayList<>())
                     .add(new ForecastPointDto(zdt.format(HM), temp, humidity, riskLevel(temp, humidity)));
            }
        } catch (RestClientException | NullPointerException | ClassCastException e) {
            log.warn("Wetter-Forecast-Abruf fehlgeschlagen: {}", e.getMessage());
        }
        return byDay;
    }

    private String forecastUrl() {
        return apiUrl.replace("/weather", "/forecast");
    }

    /**
     * Reine Berechnung aus bereits gemappten Punkten (kein Netzwerkzugriff,
     * daher direkt testbar). Vormittag = Slots 6:00–12:00, Abend = Slots ab
     * 17:00. Gesucht wird je Bereich der Slot mit dem niedrigsten Risiko
     * (Vormittag: frühester Treffer, Abend: spätester Treffer). Bleibt das
     * Minimum im Bereich Level 3, gibt es kein sicheres Fenster (null).
     * Es wird nie etwas vor 6:00 vorgeschlagen, das Fenster-Ende wird auf
     * 23:00 gekappt (keine Runden mitten in der Nacht).
     */
    static WeatherForecastDto buildForecast(List<ForecastPointDto> points) {
        int maxRisk = points.stream().mapToInt(ForecastPointDto::getRiskLevel).max().orElse(0);

        List<ForecastPointDto> morning = points.stream()
                .filter(p -> hour(p.getTime()) >= 6 && hour(p.getTime()) < 12)
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

    /** Roher Hitze-Score der Formel (tempF + Feuchte) — je kleiner, desto besser. */
    private static double heatScore(ForecastPointDto p) {
        return p.getTemperature() * 9.0 / 5.0 + 32 + p.getHumidity();
    }

    private static WeatherWindowDto bestWindow(List<ForecastPointDto> slots, boolean earliest) {
        if (slots.isEmpty()) return null;
        int min = slots.stream().mapToInt(ForecastPointDto::getRiskLevel).min().orElse(3);
        if (min >= 3) return null;

        // Nicht stur den frühesten/spätesten Slot, sondern den nach der Formel
        // tatsächlich KÜHLSTEN — so variiert das Fenster echt mit dem Wetter.
        // Bei Gleichstand: morgens der frühere, abends der spätere Slot.
        // (Slots sind chronologisch; earliest ersetzt nur bei echtem Minimum.)
        ForecastPointDto chosen = slots.get(0);
        double best = heatScore(chosen);
        for (int i = 1; i < slots.size(); i++) {
            double s = heatScore(slots.get(i));
            if (earliest ? s < best : s <= best) { best = s; chosen = slots.get(i); }
        }

        LocalTime start = LocalTime.parse(chosen.getTime());
        LocalTime end = start.plusHours(3);
        // Ende auf 23:00 kappen (auch wenn +3h über Mitternacht wrappt).
        if (end.isBefore(start) || !end.isBefore(DAY_END)) end = DAY_END;
        // Nach dem Kappen kein Null-/Negativ-Fenster (z. B. Slot 23:00 → „23:00–23:00").
        if (!end.isAfter(start)) return null;
        return new WeatherWindowDto(chosen.getTime(), end.format(HM));
    }
}
