package de.wlad.kiratracker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class WeatherService {

    @Value("${WEATHER_API_KEY:}")
    private String apiKey;

    @Value("${WEATHER_CITY:Berlin}")
    private String city;

    @Value("${WEATHER_COUNTRY:DE}")
    private String country;

    public WeatherDto getCurrentWeather() {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                return new WeatherDto(0, "API Key fehlt", 0);
            }

            // OpenWeatherMap API URL
            String url = String.format(
                    "https://api.openweathermap.org/data/2.5/weather?q=%s,%s&appid=%s&units=metric&lang=de",
                    city, country, apiKey
            );

            RestTemplate restTemplate = new RestTemplate();
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("main") && response.containsKey("weather")) {
                Map<String, Object> main = (Map<String, Object>) response.get("main");
                Object tempObj = main.get("temp");
                double temp = tempObj instanceof Number ? ((Number) tempObj).doubleValue() : 0.0;

                // Weather description
                java.util.List<Map<String, Object>> weatherList = (java.util.List<Map<String, Object>>) response.get("weather");
                String description = weatherList.get(0).get("description").toString();

                // Weather code
                Object idObj = weatherList.get(0).get("id");
                int code = idObj instanceof Number ? ((Number) idObj).intValue() : 0;

                return new WeatherDto(temp, description, code);
            }

            return new WeatherDto(0, "Keine Daten", 0);

        } catch (Exception e) {
            System.out.println("Wetter-Fehler: " + e.getMessage());
            return new WeatherDto(0, "Wetter nicht verfügbar", 0);
        }
    }
}
