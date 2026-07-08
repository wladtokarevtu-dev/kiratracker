package de.wlad.kiratracker;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WeatherController {

    private final WeatherService weatherService;

    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    @GetMapping("/weather/forecast")
    public ResponseEntity<WeatherForecastDto> forecast() {
        return ResponseEntity.ok(weatherService.getTodayForecast());
    }
}
