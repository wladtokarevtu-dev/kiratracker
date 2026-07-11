package de.wlad.kiratracker;

import java.util.List;

/** Mehrtägige Vorhersage (OWM liefert 5 Tage / 3h) für den Tagesauswahl-Strip. */
public class WeekForecastDto {
    private List<DayForecastDto> days;

    public WeekForecastDto() {
    }

    public WeekForecastDto(List<DayForecastDto> days) {
        this.days = days;
    }

    public List<DayForecastDto> getDays() { return days; }
    public void setDays(List<DayForecastDto> days) { this.days = days; }
}
