package de.wlad.kiratracker;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

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

    @org.junit.jupiter.api.Test
    void buildForecast_picksEarliestLowestRiskInMorning_andLatestInEvening() {
        List<ForecastPointDto> points = List.of(
                new ForecastPointDto("07:00", 18, 60, 0),
                new ForecastPointDto("10:00", 28, 70, 1),
                new ForecastPointDto("13:00", 34, 80, 3),
                new ForecastPointDto("16:00", 34, 80, 3),
                new ForecastPointDto("19:00", 30, 75, 1),
                new ForecastPointDto("22:00", 24, 65, 0)
        );

        WeatherForecastDto forecast = WeatherService.buildForecast(points);

        assertThat(forecast.getMaxRiskLevel()).isEqualTo(3);
        assertThat(forecast.getMorningWindow().getStart()).isEqualTo("07:00");
        assertThat(forecast.getMorningWindow().getEnd()).isEqualTo("10:00");
        assertThat(forecast.getEveningWindow().getStart()).isEqualTo("22:00");
        assertThat(forecast.getEveningWindow().getEnd()).isEqualTo("23:00");
    }

    @org.junit.jupiter.api.Test
    void buildForecast_ignoresSlotsBeforeSix_andCapsEveningEndAt2300() {
        List<ForecastPointDto> points = List.of(
                new ForecastPointDto("03:00", 16, 55, 0),   // vor 6:00 → nicht vorschlagen
                new ForecastPointDto("09:00", 26, 60, 1),
                new ForecastPointDto("21:00", 22, 60, 0)    // +3h = 00:00 → auf 23:00 kappen
        );

        WeatherForecastDto forecast = WeatherService.buildForecast(points);

        assertThat(forecast.getMorningWindow().getStart()).isEqualTo("09:00");
        assertThat(forecast.getEveningWindow().getStart()).isEqualTo("21:00");
        assertThat(forecast.getEveningWindow().getEnd()).isEqualTo("23:00");
    }

    @org.junit.jupiter.api.Test
    void buildForecast_amongTiedRiskLevels_picksCoolestSlotByFormula() {
        // Alle Level 0 (mildes Wetter), aber unterschiedlich warm → das Fenster
        // soll den nach der Formel kühlsten Slot treffen, nicht stur früh/spät.
        List<ForecastPointDto> points = List.of(
                new ForecastPointDto("06:00", 20, 60, 0),  // Score 68.0+60=128
                new ForecastPointDto("09:00", 14, 55, 0),  // Score 57.2+55=112.2  ← kühlster Vormittag
                new ForecastPointDto("18:00", 21, 50, 0),  // Score 69.8+50=119.8  ← kühlster Abend
                new ForecastPointDto("21:00", 24, 52, 0)   // Score 75.2+52=127.2
        );

        WeatherForecastDto forecast = WeatherService.buildForecast(points);

        assertThat(forecast.getMorningWindow().getStart()).isEqualTo("09:00");
        assertThat(forecast.getEveningWindow().getStart()).isEqualTo("18:00");
    }

    @org.junit.jupiter.api.Test
    void buildForecast_dropsDegenerateEveningWindow_whenOnlySlotIsAt2300() {
        // Spätabends bleibt heute evtl. nur der 23:00-Slot übrig → +3h wird auf
        // 23:00 gekappt, das ergäbe „23:00–23:00" → soll null sein.
        List<ForecastPointDto> points = List.of(
                new ForecastPointDto("23:00", 22, 49, 0)
        );

        WeatherForecastDto forecast = WeatherService.buildForecast(points);

        assertThat(forecast.getEveningWindow()).isNull();
    }

    @org.junit.jupiter.api.Test
    void buildForecast_returnsNullWindow_whenNoSafeSlotInRange() {
        List<ForecastPointDto> points = List.of(
                new ForecastPointDto("07:00", 34, 80, 3),
                new ForecastPointDto("10:00", 35, 82, 3),
                new ForecastPointDto("19:00", 33, 79, 3)
        );

        WeatherForecastDto forecast = WeatherService.buildForecast(points);

        assertThat(forecast.getMaxRiskLevel()).isEqualTo(3);
        assertThat(forecast.getMorningWindow()).isNull();
        assertThat(forecast.getEveningWindow()).isNull();
    }

    @org.junit.jupiter.api.Test
    void buildForecast_handlesEmptyPoints() {
        WeatherForecastDto forecast = WeatherService.buildForecast(List.of());

        assertThat(forecast.getMaxRiskLevel()).isEqualTo(0);
        assertThat(forecast.getMorningWindow()).isNull();
        assertThat(forecast.getEveningWindow()).isNull();
    }
}
