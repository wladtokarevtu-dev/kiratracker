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
