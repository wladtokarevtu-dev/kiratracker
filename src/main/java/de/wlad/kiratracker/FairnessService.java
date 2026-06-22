package de.wlad.kiratracker;

import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Fairness-Rotation: zeigt, wer als nächstes dran sein sollte.
 * Rotation nur unter Wlad·Mama·Ilja·Aaron (Dajen zählt nicht, D2).
 * „dran" = wenigste Runden in den letzten 14 Tagen, der für den Slot nicht
 * geblockt ist; Tie-Break = längste Zeit seit letzter Runde.
 */
@Service
public class FairnessService {

    static final List<String> PEOPLE = List.of("Wlad", "Mama", "Ilja", "Aaron");
    static final int WINDOW_DAYS = 14;
    private static final ZoneId BERLIN_ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd.MM.yy");

    private final WalkRepository walkRepository;
    private final WalkBlockService blockService;
    private final PauseRepository pauseRepository;

    public FairnessService(WalkRepository walkRepository, WalkBlockService blockService,
                           PauseRepository pauseRepository) {
        this.walkRepository = walkRepository;
        this.blockService = blockService;
        this.pauseRepository = pauseRepository;
    }

    public Map<String, Object> getFairness() {
        ZonedDateTime now = ZonedDateTime.now(BERLIN_ZONE);
        ZonedDateTime since = now.minusDays(WINDOW_DAYS);
        LocalDate today = now.toLocalDate();

        List<WalkEntry> all = walkRepository.findAllByOrderByTimeDesc();

        Map<String, Long> counts = new HashMap<>();
        Map<String, ZonedDateTime> lastWalk = new HashMap<>();
        for (String p : PEOPLE) {
            counts.put(p, 0L);
        }
        for (WalkEntry e : all) {
            String canonical = canonical(e.getPerson());
            if (canonical == null) continue; // nicht im Rotationskreis (z. B. Dajen)
            // letzte Runde insgesamt (Liste ist absteigend sortiert → erste gewinnt)
            lastWalk.putIfAbsent(canonical, e.getTime());
            if (!e.getTime().isBefore(since)) {
                counts.merge(canonical, 1L, Long::sum);
            }
        }

        // geblockte Slots je Person für heute
        Map<String, Set<String>> blocked = new HashMap<>();
        for (WalkBlock b : blockService.activeToday()) {
            String canonical = canonical(b.getPerson());
            if (canonical == null) continue;
            blocked.computeIfAbsent(canonical, k -> new HashSet<>()).add(b.getSlot().toUpperCase());
        }

        List<Map<String, Object>> people = new ArrayList<>();
        for (String p : PEOPLE) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", p);
            m.put("count", counts.get(p));
            ZonedDateTime lw = lastWalk.get(p);
            m.put("lastWalk", lw == null ? null : lw.withZoneSameInstant(BERLIN_ZONE).format(FORMATTER));
            m.put("blocked", new ArrayList<>(blocked.getOrDefault(p, Set.of())));
            people.add(m);
        }

        PauseState pause = pauseRepository.findById(1L).orElse(new PauseState());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("window", WINDOW_DAYS);
        result.put("people", people);
        result.put("dranMorning", dranFor("MORNING", counts, lastWalk, blocked));
        result.put("dranEvening", dranFor("EVENING", counts, lastWalk, blocked));
        result.put("paused", pause.getPauseIndex() != null);
        return result;
    }

    /** Person mit wenigsten Runden, die für den Slot heute nicht geblockt ist;
     *  Tie-Break = längste Zeit seit letzter Runde (nie gegangen zählt am meisten). */
    private String dranFor(String slot, Map<String, Long> counts,
                           Map<String, ZonedDateTime> lastWalk, Map<String, Set<String>> blocked) {
        return PEOPLE.stream()
                .filter(p -> !blocked.getOrDefault(p, Set.of()).contains(slot))
                .min(Comparator
                        .comparingLong((String p) -> counts.get(p))
                        .thenComparing(p -> lastWalk.get(p),
                                Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(null);
    }

    /** Mappt einen Eingabenamen case-insensitiv auf den Rotations-Namen, sonst null. */
    private String canonical(String person) {
        if (person == null) return null;
        for (String p : PEOPLE) {
            if (p.equalsIgnoreCase(person.trim())) return p;
        }
        return null;
    }
}
