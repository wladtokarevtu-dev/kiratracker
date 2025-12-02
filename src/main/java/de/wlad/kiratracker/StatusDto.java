package de.wlad.kiratracker;

import java.util.List;
import java.util.Map;

public class StatusDto {

    private boolean morning;
    private boolean evening;
    private List<WalkEntry> entries;
    private Map<String, Long> leaderboard;

    public StatusDto(boolean morning,
                     boolean evening,
                     List<WalkEntry> entries,
                     Map<String, Long> leaderboard) {
        this.morning = morning;
        this.evening = evening;
        this.entries = entries;
        this.leaderboard = leaderboard;
    }

    public boolean isMorning() {
        return morning;
    }

    public boolean isEvening() {
        return evening;
    }

    public List<WalkEntry> getEntries() {
        return entries;
    }

    public Map<String, Long> getLeaderboard() {
        return leaderboard;
    }
}
