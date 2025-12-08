package de.wlad.kiratracker;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WalkService {

    private final WalkRepository walkRepository;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd.MM.yy");
    private static final ZoneId BERLIN_ZONE = ZoneId.of("Europe/Berlin");

    public WalkService(WalkRepository walkRepository) {
        this.walkRepository = walkRepository;
    }

    @Transactional
    public WalkEntry addEntry(String person) {
        ZonedDateTime now = ZonedDateTime.now(BERLIN_ZONE);
        WalkEntry entry = new WalkEntry(person, now);
        return walkRepository.save(entry);
    }

    @Transactional
    public void addWalk(String person, String time) {
        addEntry(person);
    }

    public boolean wasMorning() {
        ZonedDateTime now = ZonedDateTime.now(BERLIN_ZONE);
        ZonedDateTime todayStart = now.toLocalDate().atStartOfDay(BERLIN_ZONE);
        ZonedDateTime noon = todayStart.plusHours(12);

        List<WalkEntry> todayWalks = walkRepository.findEntriesSince(todayStart);
        return todayWalks.stream().anyMatch(w -> w.getTime().withZoneSameInstant(BERLIN_ZONE).toLocalTime().isBefore(noon.toLocalTime()));
    }

    public boolean wasEvening() {
        ZonedDateTime now = ZonedDateTime.now(BERLIN_ZONE);
        ZonedDateTime todayStart = now.toLocalDate().atStartOfDay(BERLIN_ZONE);
        ZonedDateTime noon = todayStart.plusHours(12);

        List<WalkEntry> todayWalks = walkRepository.findEntriesSince(todayStart);
        return todayWalks.stream().anyMatch(w -> !w.getTime().withZoneSameInstant(BERLIN_ZONE).toLocalTime().isBefore(noon.toLocalTime()));
    }

    public List<WalkEntryDto> getEntries() {
        return walkRepository.findAllByOrderByTimeDesc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getLeaderboardLast7Days() {
        ZonedDateTime sevenDaysAgo = ZonedDateTime.now(BERLIN_ZONE).minusDays(7);
        List<Object[]> results = walkRepository.getLeaderboardSince(sevenDaysAgo);

        Map<String, Long> leaderboard = new LinkedHashMap<>();
        for (Object[] result : results) {
            leaderboard.put((String) result[0], (Long) result[1]);
        }
        return leaderboard;
    }

    @Transactional
    public void deleteOlderThanDays(int days) {
        ZonedDateTime cutoff = ZonedDateTime.now(BERLIN_ZONE).minusDays(days);
        walkRepository.deleteByTimeBefore(cutoff);
    }

    @Transactional
    public void deleteAll() {
        walkRepository.deleteAll();
    }

    @Transactional
    public void deleteById(Long id) {
        walkRepository.deleteById(id);
    }

    @Transactional
    public WalkEntry updateEntry(Long id, String person, String timeString) {
        WalkEntry entry = walkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + id));

        entry.setPerson(person);

        if (timeString != null && !timeString.isEmpty()) {
            try {
                // Versuch 1: Deutsches Format (vom Admin Panel)
                // WICHTIG: withZone(BERLIN_ZONE) sorgt dafür, dass die eingegebene Zeit als deutsche Zeit interpretiert wird
                ZonedDateTime newTime = ZonedDateTime.parse(timeString, DateTimeFormatter.ofPattern("HH:mm dd.MM.yy").withZone(BERLIN_ZONE));
                entry.setTime(newTime);
            } catch (Exception e) {
                try {
                    // Versuch 2: ISO Format (Fallback)
                    ZonedDateTime newTime = ZonedDateTime.parse(timeString);
                    entry.setTime(newTime);
                } catch (Exception e2) {
                    System.out.println("Konnte Datum nicht parsen: " + timeString);
                }
            }
        }
        return walkRepository.save(entry);
    }

    @Transactional
    public void incrementApplause(Long id) {
        WalkEntry entry = walkRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Entry not found: " + id));
        entry.incrementApplause();
        walkRepository.save(entry);
    }

    @Transactional
    public void addApplause(Long id) {
        incrementApplause(id);
    }

    public Optional<WalkEntry> findById(Long id) {
        return walkRepository.findById(id);
    }

    private WalkEntryDto convertToDto(WalkEntry entry) {
        // Hier stellen wir sicher, dass bei der Ausgabe immer Berlin-Zeit verwendet wird
        return new WalkEntryDto(
                entry.getId(),
                entry.getPerson(),
                entry.getTime().withZoneSameInstant(BERLIN_ZONE).format(FORMATTER),
                entry.getApplauseCount()
        );
    }
}
