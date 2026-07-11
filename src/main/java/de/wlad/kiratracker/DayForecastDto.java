package de.wlad.kiratracker;

import java.util.List;

/**
 * Vorhersage eines einzelnen Tages: die 3h-Slots als Kurve plus die
 * abgeleiteten Gassi-Zeitfenster (Morgen/Abend). {@code weekday}/{@code dayNum}
 * sind für den Tagesauswahl-Strip im Frontend.
 */
public class DayForecastDto {
    private String date;      // yyyy-MM-dd
    private String weekday;   // "Sa"
    private int dayNum;       // 11
    private List<ForecastPointDto> points;
    private int maxRiskLevel;
    private WeatherWindowDto morningWindow;
    private WeatherWindowDto eveningWindow;

    public DayForecastDto() {
    }

    public DayForecastDto(String date, String weekday, int dayNum,
                          List<ForecastPointDto> points, int maxRiskLevel,
                          WeatherWindowDto morningWindow, WeatherWindowDto eveningWindow) {
        this.date = date;
        this.weekday = weekday;
        this.dayNum = dayNum;
        this.points = points;
        this.maxRiskLevel = maxRiskLevel;
        this.morningWindow = morningWindow;
        this.eveningWindow = eveningWindow;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getWeekday() { return weekday; }
    public void setWeekday(String weekday) { this.weekday = weekday; }

    public int getDayNum() { return dayNum; }
    public void setDayNum(int dayNum) { this.dayNum = dayNum; }

    public List<ForecastPointDto> getPoints() { return points; }
    public void setPoints(List<ForecastPointDto> points) { this.points = points; }

    public int getMaxRiskLevel() { return maxRiskLevel; }
    public void setMaxRiskLevel(int maxRiskLevel) { this.maxRiskLevel = maxRiskLevel; }

    public WeatherWindowDto getMorningWindow() { return morningWindow; }
    public void setMorningWindow(WeatherWindowDto morningWindow) { this.morningWindow = morningWindow; }

    public WeatherWindowDto getEveningWindow() { return eveningWindow; }
    public void setEveningWindow(WeatherWindowDto eveningWindow) { this.eveningWindow = eveningWindow; }
}
