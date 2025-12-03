package de.wlad.kiratracker;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class WalkService {

    private final WalkRepository repository;

    public WalkService(WalkRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void addEntry(String person) {
        if (person == null || person.trim().isEmpty()) {
            throw new IllegalArgumentException("Person darf nicht leer sein");
        }
        repository.save(new WalkEntry(person.trim(), LocalDateTime.now()));
        cleanup();
    }

    public List<WalkEntry> getEntries() {
        return repository.findAll();
    }

    // war Kira heute morgens (0–11 Uhr) draußen?
    public boolean wasMorning() {
        LocalDate today = LocalDate.now();
        return repository.findAll().stream()
                .filter(e -> e.getTime().toLocalDate().isEqual(today))
                .anyMatch(e -> e.getTime().getHour() < 12);
    }

    // war Kira heute abends (12–23 Uhr) draußen?
    public boolean wasEvening() {
        LocalDate today = LocalDate.now();
        return repository.findAll().stream()
                .filter(e -> e.getTime().toLocalDate().isEqual(today))
                .anyMatch(e -> e.getTime().getHour() >= 12);
    }

    // Leaderboard letzte 7 Tage
    public Map<String, Long> getLeaderboardLast7Days() {
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);
        return repository.findByTimeAfter(weekAgo).stream()
                .collect(Collectors.groupingBy(
                        WalkEntry::getPerson,
                        Collectors.counting()
                ));
    }

    // alles löschen, was älter als 7 Tage ist
    @Transactional
    private void cleanup() {
        deleteOlderThanDays(7);
    }

    // ==== Admin-Funktionen ====
    @Transactional
    public void deleteAll() {
        repository.deleteAll();
    }

    @Transactional
    public void deleteOlderThanDays(int days) {
        LocalDateTime limit = LocalDateTime.now().minusDays(days);
        repository.deleteByTimeBefore(limit);
    }
}
