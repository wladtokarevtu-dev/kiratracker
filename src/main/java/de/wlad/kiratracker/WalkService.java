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
    private final WalkRepository repo;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd.MM.yy");
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    // Cache für heute's Walks (wird häufig abgefragt)
    private List<WalkEntry> todayWalksCache;
    private long todayWalksCacheTime = 0;
    private static final long WALKS_CACHE_DURATION = 60000; // 1 Minute

    public WalkService(WalkRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public WalkEntry addEntry(String p) {
        invalidateTodayCache(); // Cache invalidieren wenn neue Entry hinzugefügt wird
        return repo.save(new WalkEntry(p, ZonedDateTime.now(ZONE)));
    }

    @Transactional
    public void addWalk(String p, String t) {
        addEntry(p);
    }

    public boolean wasMorning() {
        return checkTimeRange(0, 12);
    }

    public boolean wasEvening() {
        return checkTimeRange(12, 24);
    }

    // Optimierte Version - benutzt Cache
    private boolean checkTimeRange(int startHour, int endHour) {
        ZonedDateTime now = ZonedDateTime.now(ZONE);
        List<WalkEntry> todayWalks = getTodayWalksCached();

        return todayWalks.stream().anyMatch(w -> {
            int h = w.getTime().withZoneSameInstant(ZONE).getHour();
            return h >= startHour && h < endHour;
        });
    }

    // Cache für heute's Walks
    private List<WalkEntry> getTodayWalksCached() {
        long now = System.currentTimeMillis();
        if (todayWalksCache != null && (now - todayWalksCacheTime) < WALKS_CACHE_DURATION) {
            return todayWalksCache;
        }

        ZonedDateTime startOfDay = ZonedDateTime.now(ZONE).toLocalDate().atStartOfDay(ZONE);
        todayWalksCache = repo.findEntriesSince(startOfDay);
        todayWalksCacheTime = now;
        return todayWalksCache;
    }

    private void invalidateTodayCache() {
        todayWalksCache = null;
        todayWalksCacheTime = 0;
    }

    public List<WalkEntryDto> getEntries() {
        return repo.findAllByOrderByTimeDesc().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Map<String, Long> getLeaderboardLast7Days() {
        return getLeaderboardSinceDays(7);
    }

    public Map<String, Long> getLeaderboardSinceDays(int days) {
        List<Object[]> results = repo.getLeaderboardSince(ZonedDateTime.now(ZONE).minusDays(days));
        Map<String, Long> leaderboard = new LinkedHashMap<>();
        for (Object[] row : results) {
            leaderboard.put((String) row[0], (Long) row[1]);
        }
        return leaderboard;
    }

    @Transactional
    public void deleteAll() {
        repo.deleteAll();
        invalidateTodayCache();
    }

    @Transactional
    public void deleteById(Long id) {
        repo.deleteById(id);
        invalidateTodayCache();
    }

    @Transactional
    public WalkEntry updateEntry(Long id, String person, String timeStr) {
        WalkEntry entry = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Walk entry not found"));

        entry.setPerson(person);

        if (timeStr != null && !timeStr.isEmpty()) {
            try {
                ZonedDateTime parsedTime = ZonedDateTime.parse(timeStr, FMT.withZone(ZONE));
                entry.setTime(parsedTime);
            } catch (Exception ex) {
                System.err.println("Fehler beim Parsen der Zeit: " + ex.getMessage());
            }
        }

        invalidateTodayCache();
        return repo.save(entry);
    }

    @Transactional
    public void addApplause(Long id) {
        repo.findById(id).ifPresent(entry -> {
            entry.incrementApplause();
            repo.save(entry);
        });
    }

    public Optional<WalkEntry> findById(Long id) {
        return repo.findById(id);
    }

    private WalkEntryDto toDto(WalkEntry entry) {
        return new WalkEntryDto(
                entry.getId(),
                entry.getPerson(),
                entry.getTime().withZoneSameInstant(ZONE).format(FMT),
                entry.getApplauseCount()
        );
    }
}