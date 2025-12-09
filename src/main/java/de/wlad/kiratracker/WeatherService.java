package de.wlad.kiratracker;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
@Service
public class WeatherService {
    private static final String URL = "https://api.open-meteo.com/v1/forecast?latitude=52.52&longitude=13.41&current_weather=true&timezone=Europe%2FBerlin";
    public WeatherDto getCurrentWeather() {
        try {
            RestTemplate t = new RestTemplate();
            Map<String, Object> r = t.getForObject(URL, Map.class);
            Map<String, Object> c = (Map<String, Object>) r.get("current_weather");
            double temp = ((Number) c.get("temperature")).doubleValue();
            int code = ((Number) c.get("weathercode")).intValue();
            return new WeatherDto(temp, "Wetter", code);
        } catch (Exception e) { return new WeatherDto(0, "Keine Daten", 0); }
    }
}