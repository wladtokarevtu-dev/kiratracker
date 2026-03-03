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
        ZonedDateTime walkTime;
        if (time != null && !time.isEmpty()) {
            try {
                walkTime = ZonedDateTime.parse(time);
            } catch (Exception e) {
                walkTime = ZonedDateTime.now(BERLIN_ZONE);
            }
        } else {
            walkTime = ZonedDateTime.now(BERLIN_ZONE);
        }
        walkRepository.save(new WalkEntry(person, walkTime));
    }

    public List<WalkEntry> getTodayWalks() {
        ZonedDateTime todayStart = ZonedDateTime.now(BERLIN_ZONE)
                .toLocalDate().atStartOfDay(BERLIN_ZONE);
        return walkRepository.findEntriesSince(todayStart);
    }

    public boolean wasMorning() {
        ZonedDateTime noon = ZonedDateTime.now(BERLIN_ZONE)
                .toLocalDate().atStartOfDay(BERLIN_ZONE).plusHours(12);
        return getTodayWalks().stream().anyMatch(w -> w.getTime().isBefore(noon));
    }

    public boolean wasEvening() {
        ZonedDateTime noon = ZonedDateTime.now(BERLIN_ZONE)
                .toLocalDate().atStartOfDay(BERLIN_ZONE).plusHours(12);
        return getTodayWalks().stream().anyMatch(w -> !w.getTime().isBefore(noon));
    }

    public List<WalkEntryDto> getEntries() {
        return walkRepository.findAllByOrderByTimeDesc().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getLeaderboardLast7Days() {
        return getLeaderboardLastNDays(7);
    }

    public Map<String, Long> getLeaderboardLastNDays(int days) {
        ZonedDateTime since = ZonedDateTime.now(BERLIN_ZONE).minusDays(days);
        List<Object[]> results = walkRepository.getLeaderboardSince(since);
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
                entry.setTime(ZonedDateTime.parse(timeString));
            } catch (Exception e) {
                throw new IllegalArgumentException("Ungueltige Zeitformat: " + timeString);
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
        return new WalkEntryDto(
                entry.getId(),
                entry.getPerson(),
                entry.getTime().format(FORMATTER),
                entry.getApplauseCount()
        );
    }
}
