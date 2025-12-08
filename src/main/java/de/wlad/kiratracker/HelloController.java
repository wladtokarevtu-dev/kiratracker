package de.wlad.kiratracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@RestController
public class HelloController {

    private final WalkService walkService;
    private final WeatherService weatherService;
    private final WalkRequestService requestService;

    @Autowired
    public HelloController(WalkService walkService, WeatherService weatherService, WalkRequestService requestService) {
        this.walkService = walkService;
        this.weatherService = weatherService;
        this.requestService = requestService;
    }

    // --- HTML SEITEN ---

    // Liefert die Admin-Seite aus (erwartet src/main/resources/templates/admin.html)
    @GetMapping("/admin")
    public ModelAndView adminPage() {
        return new ModelAndView("admin");
    }

    // --- API ENDPOINTS ---

    // Status für die HTML-Seite
    @GetMapping("/status")
    public ResponseEntity<StatusDto> status() {
        StatusDto statusDto = new StatusDto(
                walkService.wasMorning(),
                walkService.wasEvening(),
                walkService.getEntries(),
                walkService.getLeaderboardLast7Days(),
                weatherService.getCurrentWeather(),
                requestService.getPendingRequestsCount()
        );
        return ResponseEntity.ok(statusDto);
    }

    // Walk eintragen
    @PostMapping("/walk")
    public ResponseEntity<String> addWalk(@RequestBody WalkRequest request) {
        walkService.addWalk(request.getPerson(), request.getTime());
        return ResponseEntity.ok("Spaziergang eingetragen!");
    }

    // Walk Request erstellen
    @PostMapping("/walk/request")
    public ResponseEntity<String> createWalkRequest(@RequestBody WalkRequestDto dto) {
        requestService.createRequest(dto.getPerson(), dto.getTime());
        return ResponseEntity.ok("Anfrage erstellt!");
    }

    // Applaus geben
    @PostMapping("/walk/{id}/applause")
    public ResponseEntity<String> addApplause(@PathVariable Long id) {
        walkService.addApplause(id);
        return ResponseEntity.ok("Applaus gegeben!");
    }

    // --- ADMIN API ENDPOINTS (Geschützt durch SecurityConfig) ---

    // Eintrag löschen
    @DeleteMapping("/admin/walk/{id}")
    public ResponseEntity<String> deleteWalk(@PathVariable Long id) {
        walkService.deleteById(id);
        return ResponseEntity.ok("Gelöscht");
    }

    // Eintrag bearbeiten (PUT)
    @PutMapping("/admin/walk/{id}")
    public ResponseEntity<String> updateWalk(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        String newPerson = payload.get("person");
        String newTime = payload.get("time"); // Erwartet Format: HH:mm dd.MM.yy

        walkService.updateEntry(id, newPerson, newTime);
        return ResponseEntity.ok("Aktualisiert");
    }

    // Alles löschen / Reset
    @PostMapping("/admin/reset")
    public ResponseEntity<String> resetAll() {
        walkService.deleteAll();
        return ResponseEntity.ok("Alles gelöscht");
    }

    // DTO Helper Class
    static class WalkRequest {
        private String person;
        private String time;

        // Getter & Setter
        public String getPerson() { return person; }
        public void setPerson(String person) { this.person = person; }
        public String getTime() { return time; }
        public void setTime(String time) { this.time = time; }
    }
}
