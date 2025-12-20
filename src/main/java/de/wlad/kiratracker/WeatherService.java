package de.wlad.kiratracker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

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
                + "&lang=de;

        try {
            WeatherApiResponse response =
                    restTemplate.getForObject(url, WeatherApiResponse.class);

            return mapToDto(response);
        } catch (Exception ex) {
            // WICHTIG: Kein 500 mehr wegen Wetter-Fehlern
            WeatherDto dto = new WeatherDto();
            dto.setDescription("Wetter derzeit nicht verfügbar");
            dto.setTemperature(0.0);
            dto.setIcon("01d");
            return dto;
        }
    }

    private WeatherDto mapToDto(WeatherApiResponse response) {
        WeatherDto dto = new WeatherDto();
        dto.setDescription(response.getWeather().get(0).getDescription());
        dto.setTemperature(response.getMain().getTemp());
        dto.setIcon(response.getWeather().get(0).getIcon());
        return dto;
    }
}
