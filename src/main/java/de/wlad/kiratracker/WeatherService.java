package de.wlad.kiratracker;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;

@Service
public class WeatherService {
    // API URL für Berlin
    private static final String URL = "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&current_weather=true&timezone=Europe%2FBerlin";

    public WeatherDto getCurrentWeather() {
        try {
            RestTemplate t = new RestTemplate();
            // Wir holen die Rohdaten als Map
            Map<String, Object> r = t.getForObject(URL, Map.class);
            
            if (r != null && r.containsKey("current_weather")) {
                Map<String, Object> c = (Map<String, Object>) r.get("current_weather");
                
                // Sicherer Cast für Zahlen (kann Integer oder Double sein)
                double temp = 0.0;
                Object tempObj = c.get("temperature");
                if (tempObj instanceof Number) {
                    temp = ((Number) tempObj).doubleValue();
                }

                int code = 0;
                Object codeObj = c.get("weathercode");
                if (codeObj instanceof Number) {
                    code = ((Number) codeObj).intValue();
                }
                
                return new WeatherDto(temp, "Wetter", code);
            }
        } catch (Exception e) {
            // Fehler stillschweigend ignorieren, damit App nicht crasht
            System.out.println("Wetter-Fehler: " + e.getMessage());
        }
        // Fallback: Code 0 (Klar), 0 Grad, damit Frontend nicht leer bleibt
        return new WeatherDto(0, "Wetter", 0);
    }
}