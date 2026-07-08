package de.wlad.kiratracker;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherServiceTest {

    // tempC ist bewusst ein Vielfaches von 5, damit tempF exakt (ohne
    // Rundungsfehler) berechnet wird: F = tempC*9/5+32 = 9k+32 für tempC=5k.
    @ParameterizedTest(name = "{0}°C + {1}% Feuchte → Level {2}")
    @CsvSource({
            "20.0, 50, 0",
            "40.0, 45, 0",
            "40.0, 46, 1",
            "40.0, 55, 1",
            "40.0, 56, 2",
            "40.0, 75, 2",
            "40.0, 76, 3"
    })
    void riskLevel_matchesTempHumidityThresholds(double tempC, int humidity, int expected) {
        assertThat(WeatherService.riskLevel(tempC, humidity)).isEqualTo(expected);
    }
}
