package de.wlad.kiratracker;
public class WeatherDto {
    private double temperature;
    private String description;
    private int code;
    public WeatherDto(double t, String d, int c) { this.temperature = t; this.description = d; this.code = c; }
    public double getTemperature() { return temperature; }
    public String getDescription() { return description; }
    public int getCode() { return code; }
}