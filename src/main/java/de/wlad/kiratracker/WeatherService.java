package de.wlad.kiratracker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class WeatherService {

    @Value("${WEATHER_API_KEY}")
    private String apiKey;

    @Value("${WEATHER_CITY:Berlin}")
    private String city;

    @Value("${WEATHER_COUNTRY:DE}")
    private String country;

    // Cache für Wetterdaten
    private WeatherDto cachedWeather;
    private long lastFetchTime = 0;
    private static final long CACHE_DURATION_MS = 300000; // 5 Minuten

    public WeatherDto getCurrentWeather() {
        // Prüfe ob Cache noch gültig ist
        long now = System.currentTimeMillis();
        if (cachedWeather != null && (now - lastFetchTime) < CACHE_DURATION_MS) {
            System.out.println("Wetter aus Cache geladen");
            return cachedWeather;
        }

        // Cache abgelaufen oder nicht vorhanden - neu abrufen
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                System.out.println("WEATHER_API_KEY ist leer oder nicht gesetzt");
                return new WeatherDto(0, "API Key fehlt", 0);
            }

            String url = String.format(
                    "https://api.openweathermap.org/data/2.5/weather?q=%s,%s&appid=%s&units=metric&lang=de",
                    city, country, apiKey
            );

            System.out.println("Wetter-API wird aufgerufen: " + city + ", " + country);

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("main") && response.containsKey("weather")) {
                Map<String, Object> main = (Map<String, Object>) response.get("main");
                Object tempObj = main.get("temp");
                double temp = tempObj instanceof Number ? ((Number) tempObj).doubleValue() : 0.0;

                java.util.List<Map<String, Object>> weatherList =
                        (java.util.List<Map<String, Object>>) response.get("weather");
                String description = weatherList.get(0).get("description").toString();

                Object idObj = weatherList.get(0).get("id");
                int code = idObj instanceof Number ? ((Number) idObj).intValue() : 0;

                System.out.println("Wetter erfolgreich abgerufen: " + temp + "°C, " + description);

                // Cache aktualisieren
                cachedWeather = new WeatherDto(temp, description, code);
                lastFetchTime = now;
                return cachedWeather;
            }

            WeatherDto fallback = new WeatherDto(0, "Keine Daten", 0);
            cachedWeather = fallback;
            lastFetchTime = now;
            return fallback;

        } catch (Exception e) {
            System.err.println("Wetter-Fehler: " + e.getMessage());
            e.printStackTrace();

            // Bei Fehler: Cache zurückgeben falls vorhanden, sonst Fehlermeldung
            if (cachedWeather != null) {
                System.out.println("Verwende alten Wetter-Cache aufgrund von Fehler");
                return cachedWeather;
            }

            return new WeatherDto(0, "Wetter nicht verfügbar", 0);
        }
    }

    // Optional: Methode um Cache manuell zu invalidieren
    public void clearCache() {
        cachedWeather = null;
        lastFetchTime = 0;
    }
}