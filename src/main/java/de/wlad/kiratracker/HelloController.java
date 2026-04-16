package de.wlad.kiratracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
public class HelloController {

    private final WalkService walkService;
    private final WeatherService weatherService;
    private final WalkRequestService requestService;
    private final FoodService foodService;
    private final PauseRepository pauseRepository;
    private final JdbcTemplate jdbcTemplate;
    private final NotificationService notificationService;

    @Autowired
    public HelloController(WalkService w, WeatherService we, WalkRequestService r, FoodService f, PauseRepository pr, JdbcTemplate jt, NotificationService ns) {
        this.walkService = w; this.weatherService = we;
        this.requestService = r; this.foodService = f;
        this.pauseRepository = pr; this.jdbcTemplate = jt;
        this.notificationService = ns;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @GetMapping("/status")
    public ResponseEntity<StatusDto> status() {
        return ResponseEntity.ok(new StatusDto(
                walkService.wasMorning(), walkService.wasEvening(),
                walkService.getEntries(), walkService.getLeaderboardLast7Days(),
                weatherService.getCurrentWeather(), requestService.getPendingRequestsCount()));
    }

    @PostMapping("/walk")
    public ResponseEntity<String> addWalk(@RequestBody WalkLogRequest request) {
        if (request.getPerson() == null || request.getPerson().trim().isEmpty())
            return ResponseEntity.badRequest().body("Person darf nicht leer sein.");
        walkService.addWalk(request.getPerson(), request.getTime());
        return ResponseEntity.ok("Spaziergang eingetragen!");
    }

    @GetMapping("/walk")
    public ResponseEntity<List<WalkEntryDto>> getWalks() {
        return ResponseEntity.ok(walkService.getEntries());
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<Map<String,Long>> getLeaderboard(
            @RequestParam(defaultValue = "7") int days) {
        return ResponseEntity.ok(walkService.getLeaderboardLastNDays(days));
    }

    @PostMapping("/walk/request")
    public ResponseEntity<String> createWalkRequest(@RequestBody WalkRequestDto dto) {
        requestService.createRequest(dto.getPerson(), dto.getTime());
        return ResponseEntity.ok("Anfrage erstellt!");
    }

    @PostMapping("/walk/{id}/applause")
    public ResponseEntity<String> addApplause(@PathVariable Long id) {
        walkService.addApplause(id);
        return ResponseEntity.ok("Applaus gegeben!");
    }

    @GetMapping("/food")
    public ResponseEntity<List<FoodEntryDto>> getFood() {
        return ResponseEntity.ok(foodService.getTodayFood());
    }

    @PostMapping("/food")
    public ResponseEntity<FoodEntryDto> addFood(@RequestBody FoodRequest request) {
        if (request.getPerson() == null || request.getPerson().trim().isEmpty())
            return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(foodService.addFood(request.getPerson(), request.getFood()));
    }

    @PostMapping("/admin/walk/request/{id}/approve")
    public ResponseEntity<String> approveRequest(@PathVariable Long id) {
        requestService.approveRequest(id);
        return ResponseEntity.ok("Anfrage genehmigt!");
    }

    @PostMapping("/admin/walk/request/{id}/reject")
    public ResponseEntity<String> rejectRequest(@PathVariable Long id) {
        requestService.rejectRequest(id);
        return ResponseEntity.ok("Anfrage abgelehnt!");
    }

    @DeleteMapping("/admin/walk/{id}")
    public ResponseEntity<String> deleteWalk(@PathVariable Long id) {
        walkService.deleteById(id);
        return ResponseEntity.ok("Eintrag geloescht!");
    }

    @PutMapping("/admin/walk/{id}")
    public ResponseEntity<WalkEntry> updateWalk(@PathVariable Long id,
                                                @RequestBody WalkLogRequest r) {
        return ResponseEntity.ok(walkService.updateEntry(id, r.getPerson(), r.getTime()));
    }

    @DeleteMapping("/admin/food/{id}")
    public ResponseEntity<String> deleteFood(@PathVariable Long id) {
        foodService.deleteFood(id);
        return ResponseEntity.ok("Geloescht!");
    }

    @PutMapping("/admin/food/{id}")
    public ResponseEntity<FoodEntryDto> updateFood(@PathVariable Long id,
                                                   @RequestBody FoodRequest r) {
        return ResponseEntity.ok(foodService.updateFood(id, r.getPerson(), r.getFood()));
    }

    @GetMapping("/pause")
    public ResponseEntity<Map<String,Object>> getPause() {
        PauseState state = pauseRepository.findById(1L).orElse(new PauseState());
        Map<String,Object> result = new java.util.HashMap<>();
        result.put("active", state.getPauseIndex() != null);
        result.put("index", state.getPauseIndex());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/admin/pause")
    public ResponseEntity<String> setPause(@RequestBody Map<String,Integer> body) {
        PauseState state = pauseRepository.findById(1L).orElse(new PauseState());
        state.setPauseIndex(body.get("index"));
        pauseRepository.save(state);
        return ResponseEntity.ok("Pause gesetzt.");
    }

    @DeleteMapping("/admin/pause")
    public ResponseEntity<String> clearPause() {
        PauseState state = pauseRepository.findById(1L).orElse(new PauseState());
        state.setPauseIndex(null);
        pauseRepository.save(state);
        return ResponseEntity.ok("Pause beendet.");
    }

    @PostMapping("/notify")
    public ResponseEntity<String> sendNotification(@RequestBody NotifyRequest request) {
        if (request.getPerson() == null || request.getMessage() == null)
            return ResponseEntity.badRequest().body("Person und Message erforderlich.");
        String msg = switch (request.getMessage()) {
            case "walk" -> "🐕 Hey " + request.getPerson() + ", könntest du heute mit Kira Gassi gehen?";
            case "urgent" -> "🐾 " + request.getPerson() + ", Kira will raus! Wer hat Zeit für eine Runde?";
            case "food" -> "🥣 " + request.getPerson() + ", Kira wurde noch nicht gefüttert!";
            case "evening" -> "🌙 " + request.getPerson() + ", denk an Kiras Abendrunde!";
            default -> request.getMessage();
        };
        notificationService.sendCustomNotification(msg);
        return ResponseEntity.ok("Benachrichtigung gesendet!");
    }

    static class NotifyRequest {
        private String person; private String message;
        public String getPerson(){return person;} public void setPerson(String p){this.person=p;}
        public String getMessage(){return message;} public void setMessage(String m){this.message=m;}
    }

    @GetMapping("/admin/cleanup")
    public ResponseEntity<String> adminCleanup(@RequestParam(defaultValue = "30") int days) {
        walkService.deleteOlderThanDays(days);
        return ResponseEntity.ok("Alte Eintraege geloescht.");
    }

    @PostMapping("/admin/reset")
    public ResponseEntity<String> adminReset() {
        walkService.deleteAll();
        return ResponseEntity.ok("Alle Daten geloescht.");
    }

    @PostMapping("/admin/fix-timezone")
    public ResponseEntity<String> fixTimezone(@RequestParam(defaultValue = "1") int hours) {
        int walks = jdbcTemplate.update("UPDATE walk_entries SET time = time + make_interval(hours => ?)::interval", hours);
        int food = jdbcTemplate.update("UPDATE food_entries SET timestamp = timestamp + make_interval(hours => ?)::interval", hours);
        return ResponseEntity.ok("Korrigiert: " + walks + " Spaziergaenge, " + food + " Futterungen (" + (hours > 0 ? "+" : "") + hours + "h)");
    }

    static class WalkLogRequest {
        private String person; private String time;
        public String getPerson(){return person;} public void setPerson(String p){this.person=p;}
        public String getTime(){return time;} public void setTime(String t){this.time=t;}
    }

    static class FoodRequest {
        private String person; private String food;
        public String getPerson(){return person;} public void setPerson(String p){this.person=p;}
        public String getFood(){return food;} public void setFood(String f){this.food=f;}
    }
}