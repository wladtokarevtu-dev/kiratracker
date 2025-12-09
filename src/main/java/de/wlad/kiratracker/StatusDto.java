package de.wlad.kiratracker;
import java.util.List;
import java.util.Map;

public class StatusDto {
    private boolean wasMorning;
    private boolean wasEvening;
    private List<WalkEntryDto> entries;
    private List<String> foodToday; // NEU: Liste der Fütterungen heute (Text)
    private Map<String, Long> leaderboard;
    private WeatherDto weather;
    private long pendingRequests;

    public StatusDto(boolean m, boolean e, List<WalkEntryDto> en, Map<String, Long> lb, WeatherDto w, long req, List<String> fd) {
        this.wasMorning = m;
        this.wasEvening = e;
        this.entries = en;
        this.leaderboard = lb;
        this.weather = w;
        this.pendingRequests = req;
        this.foodToday = fd;
    }
    // Getters für JSON Serialisierung nötig
    public boolean isWasMorning(){return wasMorning;}
    public boolean isWasEvening(){return wasEvening;}
    public List<WalkEntryDto> getEntries(){return entries;}
    public Map<String,Long> getLeaderboard(){return leaderboard;}
    public WeatherDto getWeather(){return weather;}
    public long getPendingRequests(){return pendingRequests;}
    public List<String> getFoodToday(){return foodToday;}
}