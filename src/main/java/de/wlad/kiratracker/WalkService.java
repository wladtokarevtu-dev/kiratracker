package de.wlad.kiratracker;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WalkService {

    private final List<WalkEntry> entries = new ArrayList<>();

    public void addEntry(String person) {
        entries.add(new WalkEntry(person, LocalDateTime.now()));
        cleanup();
    }

    public List<WalkEntry> getEntries() {
        return new ArrayList<>(entries);
    }

    // war Kira heute morgens (0–11 Uhr) draußen?
    public boolean wasMorning() {
        LocalDate today = LocalDate.now();
        return entries.stream()
                .filter(e -> e.getTime().toLocalDate().isEqual(today))
                .anyMatch(e -> e.getTime().getHour() < 12);
    }

    // war Kira heute abends (12–23 Uhr) draußen?
    public boolean wasEvening() {
        LocalDate today = LocalDate.now();
        return entries.stream()
                .filter(e -> e.getTime().toLocalDate().isEqual(today))
                .anyMatch(e -> e.getTime().getHour() >= 12);
    }

    // Leaderboard letzte 7 Tage
    public Map<String, Long> getLeaderboardLast7Days() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        return entries.stream()
                .filter(e -> e.getTime().isAfter(weekAgo))
                .collect(Collectors.groupingBy(
                        WalkEntry::getPerson,
                        Collectors.counting()
                ));
    }

    // alles löschen, was älter als 7 Tage ist (Standard: 7 Tage)
    private void cleanup() {
        deleteOlderThanDays(7);
    }

    // ==== Admin-Funktionen ====

    // ALLE Einträge löschen
    public void deleteAll() {
        entries.clear();
    }

    // Nur Einträge löschen, die älter als X Tage sind
    public void deleteOlderThanDays(int days) {
        LocalDateTime limit = LocalDateTime.now().minusDays(days);
        entries.removeIf(e -> e.getTime().isBefore(limit));
    }
}
