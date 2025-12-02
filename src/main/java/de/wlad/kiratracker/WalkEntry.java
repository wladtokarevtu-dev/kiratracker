package de.wlad.kiratracker;

import java.time.LocalDateTime;

public class WalkEntry {
    private String person;
    private LocalDateTime time;

    public WalkEntry(String person, LocalDateTime time) {
        this.person = person;
        this.time = time;
    }

    public String getPerson() {
        return person;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
