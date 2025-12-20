package de.wlad.kiratracker;

public class WeatherDto {
    private double temp;  // Frontend erwartet "temp", nicht "temperature"
    private String description;
    private int code;

    public WeatherDto(double temp, String description, int code) {
        this.temp = temp;
        this.description = description;
        this.code = code;
    }

    // Getter mit korrektem Namen für Frontend
    public double getTemp() {
        return temp;
    }

    public String getDescription() {
        return description;
    }

    public int getCode() {
        return code;
    }

    public void setTemp(double temp) {
        this.temp = temp;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCode(int code) {
        this.code = code;
    }
}