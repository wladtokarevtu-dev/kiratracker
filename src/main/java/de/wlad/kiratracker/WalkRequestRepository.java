package de.wlad.kiratracker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Repository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Repository
public interface WalkRequestRepository extends JpaRepository<WalkRequest, Long> {

    List<WalkRequest> findAllByOrderByRequestTimeDesc();

    List<WalkRequest> findByStatusOrderByRequestTimeDesc(WalkRequest.RequestStatus status);

    long countByStatus(WalkRequest.RequestStatus status);

    @RestController
    class HelloController {

        private final WalkService walkService;
        private final WeatherService weatherService;
        private final WalkRequestService requestService;

        public HelloController(WalkService walkService, WeatherService weatherService,
                               WalkRequestService requestService) {
            this.walkService = walkService;
            this.weatherService = weatherService;
            this.requestService = requestService;
        }

        // ===== USER ENDPOINTS =====

        @PostMapping("/walk")
        public ResponseEntity<WalkEntryDto> addWalk(@RequestParam String person) {
            WalkEntry entry = walkService.addEntry(person);
            WalkEntryDto dto = new WalkEntryDto(
                    entry.getId(),
                    entry.getPerson(),
                    entry.getTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd.MM.yy")),
                    entry.getApplauseCount()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        }

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

        @PostMapping("/applause/{id}")
        public ResponseEntity<String> applause(@PathVariable Long id) {
            try {
                walkService.incrementApplause(id);
                return ResponseEntity.ok("Applaus hinzugefügt!");
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }
        }

        @PostMapping("/request")
        public ResponseEntity<WalkRequestDto> createWalkRequest(
                @RequestParam String requester,
                @RequestParam String message) {
            WalkRequest request = requestService.createRequest(requester, message);
            WalkRequestDto dto = new WalkRequestDto(
                    request.getId(),
                    request.getRequester(),
                    request.getMessage(),
                    request.getRequestTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd.MM.yy")),
                    request.getStatus().toString(),
                    null
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        }

        @GetMapping("/requests")
        public ResponseEntity<List<WalkRequestDto>> getAllRequests() {
            return ResponseEntity.ok(requestService.getAllRequests());
        }

        // ===== ADMIN ENDPOINTS =====

        @GetMapping("/admin/cleanup")
        public ResponseEntity<String> cleanup(@RequestParam(defaultValue = "30") int days) {
            walkService.deleteOlderThanDays(days);
            return ResponseEntity.ok("Einträge älter als " + days + " Tage wurden gelöscht.");
        }

        @PostMapping("/admin/reset")
        public ResponseEntity<String> reset() {
            walkService.deleteAll();
            return ResponseEntity.ok("Alle Einträge wurden gelöscht.");
        }

        @DeleteMapping("/admin/entry/{id}")
        public ResponseEntity<String> deleteEntry(@PathVariable Long id) {
            try {
                walkService.deleteById(id);
                return ResponseEntity.ok("Eintrag gelöscht.");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Eintrag nicht gefunden.");
            }
        }

        @PutMapping("/admin/entry/{id}")
        public ResponseEntity<WalkEntryDto> updateEntry(
                @PathVariable Long id,
                @RequestParam String person,
                @RequestParam(required = false) String time) {
            try {
                WalkEntry updated = walkService.updateEntry(id, person, time);
                WalkEntryDto dto = new WalkEntryDto(
                        updated.getId(),
                        updated.getPerson(),
                        updated.getTime().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm dd.MM.yy")),
                        updated.getApplauseCount()
                );
                return ResponseEntity.ok(dto);
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        }

        @PostMapping("/admin/request/{id}/accept")
        public ResponseEntity<String> acceptRequest(@PathVariable Long id) {
            try {
                requestService.acceptRequest(id);
                return ResponseEntity.ok("Anfrage akzeptiert.");
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }
        }

        @PostMapping("/admin/request/{id}/reject")
        public ResponseEntity<String> rejectRequest(@PathVariable Long id) {
            try {
                requestService.rejectRequest(id);
                return ResponseEntity.ok("Anfrage abgelehnt.");
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
            }
        }

        @DeleteMapping("/admin/request/{id}")
        public ResponseEntity<String> deleteRequest(@PathVariable Long id) {
            try {
                requestService.deleteRequest(id);
                return ResponseEntity.ok("Anfrage gelöscht.");
            } catch (Exception e) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Anfrage nicht gefunden.");
            }
        }
    }
}
