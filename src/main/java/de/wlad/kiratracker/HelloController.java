package de.wlad.kiratracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import java.util.Map;

@RestController
public class HelloController {
    private final WalkService s;
    private final WeatherService w;
    private final WalkRequestService r;
    @Autowired
    public HelloController(WalkService s, WeatherService w, WalkRequestService r) { this.s=s; this.w=w; this.r=r; }
    
    @GetMapping("/admin") public ModelAndView adminPage() { return new ModelAndView("admin"); }
    
    @GetMapping("/status") public ResponseEntity<StatusDto> status() {
        return ResponseEntity.ok(new StatusDto(s.wasMorning(), s.wasEvening(), s.getEntries(), s.getLeaderboardLast7Days(), w.getCurrentWeather(), r.getPendingRequestsCount()));
    }
    
    @GetMapping("/leaderboard") public ResponseEntity<Map<String, Long>> lb(@RequestParam(defaultValue="7") int days) { return ResponseEntity.ok(s.getLeaderboardSinceDays(days)); }
    
    @PostMapping("/walk") public ResponseEntity<String> addWalk(@RequestBody WalkRequest req) { s.addWalk(req.getPerson(), req.getTime()); return ResponseEntity.ok("Ok"); }
    
    @PostMapping("/walk/request") public ResponseEntity<String> createReq(@RequestBody WalkRequestDto d) { r.createRequest(d.getPerson(), d.getTime()); return ResponseEntity.ok("Ok"); }
    
    @PostMapping("/walk/{id}/applause") public ResponseEntity<String> app(@PathVariable Long id) { s.addApplause(id); return ResponseEntity.ok("Ok"); }
    
    @DeleteMapping("/admin/walk/{id}") public ResponseEntity<String> del(@PathVariable Long id) { s.deleteById(id); return ResponseEntity.ok("Ok"); }
    
    @PutMapping("/admin/walk/{id}") public ResponseEntity<String> upd(@PathVariable Long id, @RequestBody Map<String,String> p) { s.updateEntry(id, p.get("person"), p.get("time")); return ResponseEntity.ok("Ok"); }
    
    @PostMapping("/admin/reset") public ResponseEntity<String> res() { s.deleteAll(); return ResponseEntity.ok("Ok"); }
    
    @RequestMapping(value="/admin/reset", method=RequestMethod.OPTIONS) public ResponseEntity<Void> pre() { return ResponseEntity.ok().build(); }
    
    static class WalkRequest { private String person; private String time; public String getPerson(){return person;} public void setPerson(String p){this.person=p;} public String getTime(){return time;} public void setTime(String t){this.time=t;} }
}
