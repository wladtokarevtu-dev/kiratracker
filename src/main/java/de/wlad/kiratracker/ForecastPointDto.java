package de.wlad.kiratracker;

public class ForecastPointDto {
    private String time;
    private double temperature;
    private int humidity;
    private int riskLevel;

    public ForecastPointDto() {
    }

    public ForecastPointDto(String time, double temperature, int humidity, int riskLevel) {
        this.time = time;
        this.temperature = temperature;
        this.humidity = humidity;
        this.riskLevel = riskLevel;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public double getTemperature() {
        return temperature;
    }

    public void setTemperature(double temperature) {
        this.temperature = temperature;
    }

    public int getHumidity() {
        return humidity;
    }

    public void setHumidity(int humidity) {
        this.humidity = humidity;
    }

    public int getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(int riskLevel) {
        this.riskLevel = riskLevel;
    }
}
