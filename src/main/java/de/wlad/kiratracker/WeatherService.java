package de.wlad.kiratracker;

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
import java.util.List;
import java.util.Map;

@Service
public class WeatherService {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

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
}
