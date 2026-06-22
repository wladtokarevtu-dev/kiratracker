package de.wlad.kiratracker;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
public class WalkBlockService {

    private static final ZoneId BERLIN_ZONE = ZoneId.of("Europe/Berlin");
    static final Set<String> VALID_SLOTS = Set.of("MORNING", "EVENING");

    private final WalkBlockRepository repository;

    public WalkBlockService(WalkBlockRepository repository) {
        this.repository = repository;
    }

    /**
     * Legt für jeden Slot eine Sperre an. Notiz ist Pflicht.
     * @param day null = heute.
     */
    @Transactional
    public List<WalkBlock> add(String person, List<String> slots, String note, LocalDate day) {
        if (person == null || person.isBlank())
            throw new IllegalArgumentException("Person darf nicht leer sein.");
        if (note == null || note.isBlank())
            throw new IllegalArgumentException("Eine Begründung ist Pflicht.");
        if (slots == null || slots.isEmpty())
            throw new IllegalArgumentException("Mindestens ein Slot muss gewählt werden.");

        LocalDate targetDay = (day != null) ? day : LocalDate.now(BERLIN_ZONE);
        ZonedDateTime now = ZonedDateTime.now(BERLIN_ZONE);

        List<WalkBlock> created = new ArrayList<>();
        for (String rawSlot : slots) {
            String slot = rawSlot == null ? "" : rawSlot.trim().toUpperCase();
            if (!VALID_SLOTS.contains(slot))
                throw new IllegalArgumentException("Ungültiger Slot: " + rawSlot);
            created.add(repository.save(new WalkBlock(person.trim(), slot, note.trim(), targetDay, now)));
        }
        return created;
    }

    public List<WalkBlock> activeToday() {
        return repository.findByDay(LocalDate.now(BERLIN_ZONE));
    }

    public boolean isBlocked(String person, String slot, LocalDate day) {
        if (person == null || slot == null) return false;
        return repository.findByDay(day).stream()
                .anyMatch(b -> b.getPerson().equalsIgnoreCase(person)
                        && b.getSlot().equalsIgnoreCase(slot));
    }

    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }
}
