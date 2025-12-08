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
    public WalkService(WalkRepository repo) { this.repo = repo; }
    
    @Transactional public WalkEntry addEntry(String p) { return repo.save(new WalkEntry(p, ZonedDateTime.now(ZONE))); }
    @Transactional public void addWalk(String p, String t) { addEntry(p); }
    public boolean wasMorning() { return c(0,12); }
    public boolean wasEvening() { return c(12,24); }
    private boolean c(int s, int e) { ZonedDateTime n=ZonedDateTime.now(ZONE); return repo.findEntriesSince(n.toLocalDate().atStartOfDay(ZONE)).stream().anyMatch(w->{int h=w.getTime().withZoneSameInstant(ZONE).getHour();return h>=s&&h<e;}); }
    
    public List<WalkEntryDto> getEntries() { return repo.findAllByOrderByTimeDesc().stream().map(this::d).collect(Collectors.toList()); }
    public Map<String, Long> getLeaderboardLast7Days() { return getLeaderboardSinceDays(7); }
    public Map<String, Long> getLeaderboardSinceDays(int days) { List<Object[]> r=repo.getLeaderboardSince(ZonedDateTime.now(ZONE).minusDays(days)); Map<String,Long> m=new LinkedHashMap<>(); for(Object[] o:r)m.put((String)o[0],(Long)o[1]); return m; }
    
    @Transactional public void deleteAll() { repo.deleteAll(); }
    @Transactional public void deleteById(Long id) { repo.deleteById(id); }
    @Transactional public WalkEntry updateEntry(Long id, String p, String t) { WalkEntry e=repo.findById(id).orElseThrow(); e.setPerson(p); if(t!=null&&!t.isEmpty()){try{e.setTime(ZonedDateTime.parse(t,FMT.withZone(ZONE)));}catch(Exception ex){}} return repo.save(e); }
    @Transactional public void addApplause(Long id) { repo.findById(id).ifPresent(e->{e.incrementApplause();repo.save(e);}); }
    public Optional<WalkEntry> findById(Long id) { return repo.findById(id); }
    private WalkEntryDto d(WalkEntry e) { return new WalkEntryDto(e.getId(), e.getPerson(), e.getTime().withZoneSameInstant(ZONE).format(FMT), e.getApplauseCount()); }
}
