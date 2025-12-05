package de.wlad.kiratracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    // Walk Request annehmen
    @PostMapping("/walk/request/{id}/approve")
    public ResponseEntity<String> approveRequest(@PathVariable Long id) {
        requestService.approveRequest(id);
        return ResponseEntity.ok("Anfrage genehmigt!");
    }

    // Walk Request ablehnen
    @PostMapping("/walk/request/{id}/reject")
    public ResponseEntity<String> rejectRequest(@PathVariable Long id) {
        requestService.rejectRequest(id);
        return ResponseEntity.ok("Anfrage abgelehnt!");
    }

    // Applaus geben
    @PostMapping("/walk/{id}/applause")
    public ResponseEntity<String> addApplause(@PathVariable Long id) {
        walkService.addApplause(id);
        return ResponseEntity.ok("Applaus gegeben!");
    }

    static class WalkRequest {
        private String person;
        private String time;

        public String getPerson() {
            return person;
        }

        public void setPerson(String person) {
            this.person = person;
        }

        public String getTime() {
            return time;
        }

        public void setTime(String time) {
            this.time = time;
        }
    }
}
