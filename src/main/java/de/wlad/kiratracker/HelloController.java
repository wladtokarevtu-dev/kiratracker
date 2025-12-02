package de.wlad.kiratracker;

import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@RestController
public class HelloController {

    private final WalkService walkService;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm dd.MM.yy");

    public HelloController(WalkService walkService) {
        this.walkService = walkService;
    }

    // Eintrag speichern
    @PostMapping("/walk")
    public void addWalk(@RequestParam String person) {
        walkService.addEntry(person);
    }

    // Status für die HTML-Seite
    @GetMapping("/status")
    public StatusDto status() {
        return new StatusDto(
                walkService.wasMorning(),
                walkService.wasEvening(),
                walkService.getEntries(),
                walkService.getLeaderboardLast7Days()
        );
    }

    // ==== Admin-Endpoints ====

    // löscht Einträge, die älter als ?days sind (Standard 30)
    @GetMapping("/admin/cleanup")
    public String cleanup(@RequestParam(defaultValue = "30") int days) {
        walkService.deleteOlderThanDays(days);
        return "Einträge, die älter als " + days + " Tage sind, wurden gelöscht.";
    }

    // löscht ALLE Einträge
    @PostMapping("/admin/reset")
    public String reset() {
        walkService.deleteAll();
        return "redirect:/";
    }
}

