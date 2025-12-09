package de.wlad.kiratracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@RestController
public class HelloController {
    private final WalkService s;
    private final WeatherService w;
    private final WalkRequestService r;
    private final FoodRepository fRepo;
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    public HelloController(WalkService s, WeatherService w, WalkRequestService r, FoodRepository fRepo) { 
        this.s=s; this.w=w; this.r=r; this.fRepo=fRepo; 
    }
    
    @GetMapping("/admin") public ModelAndView adminPage() { return new ModelAndView("admin"); }
    
    @GetMapping("/status") public ResponseEntity<StatusDto> status() {
        ZonedDateTime startOfDay = ZonedDateTime.now(ZONE).toLocalDate().atStartOfDay(ZONE);
        List<FoodEntryDto> foodList = fRepo.findByTimeAfterOrderByTimeDesc(startOfDay).stream()
            .map(f -> new FoodEntryDto(f.getId(), f.getPerson(), f.getDescription(), f.getTime().withZoneSameInstant(ZONE).format(TIME_FMT)))
            .collect(Collectors.toList());

        return ResponseEntity.ok(new StatusDto(
            s.wasMorning(), s.wasEvening(), s.getEntries(), s.getLeaderboardLast7Days(), 
            w.getCurrentWeather(), r.getPendingRequestsCount(), foodList
        ));
    }
    
    @GetMapping("/leaderboard") public ResponseEntity<Map<String, Long>> lb(@RequestParam(defaultValue="7") int days) { return ResponseEntity.ok(s.getLeaderboardSinceDays(days)); }
    @PostMapping("/walk") public ResponseEntity<String> addWalk(@RequestBody WalkRequest req) { s.addWalk(req.getPerson(), req.getTime()); return ResponseEntity.ok("Ok"); }
    
    @PostMapping("/food") public ResponseEntity<String> addFood(@RequestBody FoodRequest req) {
        fRepo.save(new FoodEntry(req.getPerson(), req.getDescription(), ZonedDateTime.now(ZONE)));
        return ResponseEntity.ok("Ok");
    }
    
    // NEU: Essen löschen
    @DeleteMapping("/admin/food/{id}") public ResponseEntity<String> delFood(@PathVariable Long id) {
        fRepo.deleteById(id);
        return ResponseEntity.ok("Ok");
    }

    @PostMapping("/walk/request") public ResponseEntity<String> createReq(@RequestBody WalkRequestDto d) { r.createRequest(d.getPerson(), d.getTime()); return ResponseEntity.ok("Ok"); }
    @PostMapping("/walk/{id}/applause") public ResponseEntity<String> app(@PathVariable Long id) { s.addApplause(id); return ResponseEntity.ok("Ok"); }
    @DeleteMapping("/admin/walk/{id}") public ResponseEntity<String> del(@PathVariable Long id) { s.deleteById(id); return ResponseEntity.ok("Ok"); }
    @PutMapping("/admin/walk/{id}") public ResponseEntity<String> upd(@PathVariable Long id, @RequestBody Map<String,String> p) { s.updateEntry(id, p.get("person"), p.get("time")); return ResponseEntity.ok("Ok"); }
    @PostMapping("/admin/reset") public ResponseEntity<String> res() { s.deleteAll(); return ResponseEntity.ok("Ok"); }
    @RequestMapping(value="/admin/reset", method=RequestMethod.OPTIONS) public ResponseEntity<Void> pre() { return ResponseEntity.ok().build(); }
    
    static class WalkRequest { private String person; private String time; public String getPerson(){return person;} public void setPerson(String p){this.person=p;} public String getTime(){return time;} public void setTime(String t){this.time=t;} }
    static class FoodRequest { 
        private String person; private String description; 
        public String getPerson(){return person;} public void setPerson(String p){this.person=p;}
        public String getDescription(){return description;} public void setDescription(String d){this.description=d;}
    }
}