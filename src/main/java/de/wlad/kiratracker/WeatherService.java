package de.wlad.kiratracker;

import org.springframework.beans.factory.annotation.Value;
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
        this.restTemplate = new RestTemplate();
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

            return new WeatherDto(
                    (String) weather.get("description"),
                    ((Number) main.get("temp")).doubleValue(),
                    ((Number) main.get("humidity")).intValue(),
                    ((Number) wind.get("speed")).doubleValue(),
                    (String) weather.get("icon")
            );

        } catch (RestClientException | NullPointerException e) {
            return getDefaultWeather();
        }
    }

    private WeatherDto getDefaultWeather() {
        return new WeatherDto("Wetter nicht verfügbar", 0.0, 0, 0.0, "01d");
    }
}
