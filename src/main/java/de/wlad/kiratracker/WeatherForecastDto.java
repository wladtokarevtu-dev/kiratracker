package de.wlad.kiratracker;

import java.util.List;

public class WeatherForecastDto {
    private List<ForecastPointDto> points;
    private int maxRiskLevel;
    private WeatherWindowDto morningWindow;
    private WeatherWindowDto eveningWindow;

    public WeatherForecastDto() {
    }

    public WeatherForecastDto(List<ForecastPointDto> points, int maxRiskLevel,
                               WeatherWindowDto morningWindow, WeatherWindowDto eveningWindow) {
        this.points = points;
        this.maxRiskLevel = maxRiskLevel;
        this.morningWindow = morningWindow;
        this.eveningWindow = eveningWindow;
    }

    public List<ForecastPointDto> getPoints() {
        return points;
    }

    public void setPoints(List<ForecastPointDto> points) {
        this.points = points;
    }

    public int getMaxRiskLevel() {
        return maxRiskLevel;
    }

    public void setMaxRiskLevel(int maxRiskLevel) {
        this.maxRiskLevel = maxRiskLevel;
    }

    public WeatherWindowDto getMorningWindow() {
        return morningWindow;
    }

    public void setMorningWindow(WeatherWindowDto morningWindow) {
        this.morningWindow = morningWindow;
    }

    public WeatherWindowDto getEveningWindow() {
        return eveningWindow;
    }

    public void setEveningWindow(WeatherWindowDto eveningWindow) {
        this.eveningWindow = eveningWindow;
    }
}
