package de.wlad.kiratracker;

import java.util.List;
import java.util.Map;

public class StatusDto {
    private boolean wasMorning;
    private boolean wasEvening;
    private List<WalkEntryDto> entries;
    private Map<String, Long> leaderboard;
    private WeatherDto weather;
    private int pendingRequestsCount;

    public StatusDto() {
    }

    public StatusDto(boolean wasMorning, boolean wasEvening, List<WalkEntryDto> entries,
                     Map<String, Long> leaderboard, WeatherDto weather, int pendingRequestsCount) {
        this.wasMorning = wasMorning;
        this.wasEvening = wasEvening;
        this.entries = entries;
        this.leaderboard = leaderboard;
        this.weather = weather;
        this.pendingRequestsCount = pendingRequestsCount;
    }

    // Getters and Setters
    public boolean isWasMorning() {
        return wasMorning;
    }

    public void setWasMorning(boolean wasMorning) {
        this.wasMorning = wasMorning;
    }

    public boolean isWasEvening() {
        return wasEvening;
    }

    public void setWasEvening(boolean wasEvening) {
        this.wasEvening = wasEvening;
    }

    public List<WalkEntryDto> getEntries() {
        return entries;
    }

    public void setEntries(List<WalkEntryDto> entries) {
        this.entries = entries;
    }

    public Map<String, Long> getLeaderboard() {
        return leaderboard;
    }

    public void setLeaderboard(Map<String, Long> leaderboard) {
        this.leaderboard = leaderboard;
    }

    public WeatherDto getWeather() {
        return weather;
    }

    public void setWeather(WeatherDto weather) {
        this.weather = weather;
    }

    public int getPendingRequestsCount() {
        return pendingRequestsCount;
    }

    public void setPendingRequestsCount(int pendingRequestsCount) {
        this.pendingRequestsCount = pendingRequestsCount;
    }
}
