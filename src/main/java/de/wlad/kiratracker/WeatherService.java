package de.wlad.kiratracker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class WeatherService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${WEATHER_API_KEY}")
    private String apiKey;

    public WeatherDto getCurrentWeather() {
        String url = "https://api.openweathermap.org/data/2.5/weather"
                + "?q=Berlin,de"
                + "&appid=" + apiKey
                + "&units=metric"
                + "&lang=de";

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response =
                    restTemplate.getForObject(url, Map.class);

            // main.temp
            Map<String, Object> main = (Map<String, Object>) response.get("main");
            double temp = main != null && main.get("temp") != null
                    ? ((Number) main.get("temp")).doubleValue()
                    : 0.0;

            // weather[0].description + weather[0].id
            List<Map<String, Object>> weatherList =
                    (List<Map<String, Object>>) response.get("weather");
            String description = "Unbekannt";
            int code = 0;
            if (weatherList != null && !weatherList.isEmpty()) {
                Map<String, Object> w = weatherList.get(0);
                if (w.get("description") != null) {
                    description = w.get("description").toString();
                }
                if (w.get("id") != null) {
                    code = ((Number) w.get("id")).intValue();
                }
            }

            return new WeatherDto(temp, description, code);
        } catch (Exception ex) {
            // WICHTIG: Kein 500 bei Wetterfehlern
            return new WeatherDto(
                    0.0,
                    "Wetter derzeit nicht verfügbar",
                    0
            );
        }
    }
}
