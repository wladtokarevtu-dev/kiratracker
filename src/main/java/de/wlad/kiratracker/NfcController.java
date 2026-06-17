package de.wlad.kiratracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.ZoneId;
import java.time.ZonedDateTime;

import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/nfc")
public class NfcController {

    private static final int COOLDOWN_MINUTES = 30;
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private final DeviceIdentityRepository deviceRepo;
    private final WalkService walkService;
    private final WalkRepository walkRepository;

    @Autowired
    public NfcController(DeviceIdentityRepository deviceRepo, WalkService walkService, WalkRepository walkRepository) {
        this.deviceRepo = deviceRepo;
        this.walkService = walkService;
        this.walkRepository = walkRepository;
    }

    @GetMapping({"", "/"})
    public ResponseEntity<Void> nfcPage() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", "/nfc.html")
                .build();
    }

    /**
     * Registriert ein Gerät mit einer Person.
     * Wird aufgerufen wenn der Nutzer seinen Namen eingegeben hat.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest req) {
        if (req.getFullHash() == null || req.getStableHash() == null || req.getPerson() == null || req.getPerson().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Fehlende Daten"));
        }

        // Prüfen ob dieses Gerät schon registriert ist (fullHash oder stableHash)
        Optional<DeviceIdentity> existing = deviceRepo.findByFullHash(req.getFullHash());
        if (existing.isEmpty()) {
            existing = deviceRepo.findByStableHash(req.getStableHash());
        }
        if (existing.isPresent()) {
            existing.get().setPerson(req.getPerson());
            existing.get().setFullHash(req.getFullHash());
            existing.get().setStableHash(req.getStableHash());
            existing.get().setLastSeen(ZonedDateTime.now());
            deviceRepo.save(existing.get());
        } else {
            deviceRepo.save(new DeviceIdentity(req.getFullHash(), req.getStableHash(), req.getPerson()));
        }

        return ResponseEntity.ok(Map.of("status", "registered", "person", req.getPerson()));
    }

    /**
     * Trägt Gassi ein. Fingerprint identifiziert die Person automatisch.
     * Gibt zurück ob Eintrag erfolgt oder Person erst noch gewählt werden muss.
     */
    @PostMapping("/walk")
    public ResponseEntity<Map<String, Object>> nfcWalk(@RequestBody FingerprintRequest req) {
        if (req.getFullHash() == null || req.getStableHash() == null) {
            return ResponseEntity.badRequest().body(Map.of("status", "error", "message", "Fingerprint fehlt"));
        }

        // Gerät identifizieren
        Optional<DeviceIdentity> identity = deviceRepo.findByFullHash(req.getFullHash());
        if (identity.isEmpty()) {
            identity = deviceRepo.findByStableHash(req.getStableHash());
            if (identity.isPresent()) {
                // fullHash aktualisieren
                identity.get().setFullHash(req.getFullHash());
                deviceRepo.save(identity.get());
            }
        }

        if (identity.isEmpty()) {
            return ResponseEntity.ok(Map.of("status", "unknown"));
        }

        String person = identity.get().getPerson();
        identity.get().setLastSeen(ZonedDateTime.now(BERLIN));
        deviceRepo.save(identity.get());

        // Harte Sperre: gleiche Person in den letzten 10 Sekunden → stiller Doppeleintrag-Schutz
        ZonedDateTime tenSecAgo = ZonedDateTime.now(BERLIN).minusSeconds(10);
        boolean duplicate = walkRepository.findEntriesSince(tenSecAgo).stream()
                .anyMatch(w -> w.getPerson().equalsIgnoreCase(person));
        if (duplicate) {
            // Letzten Eintrag zurückgeben ohne neu anzulegen
            var last = walkRepository.findEntriesSince(tenSecAgo).stream()
                    .filter(w -> w.getPerson().equalsIgnoreCase(person))
                    .findFirst().orElseThrow();
            return ResponseEntity.ok(Map.of("status", "logged", "person", person, "walkId", last.getId()));
        }

        // Weicher Hinweis: gleiche Person in den letzten 30 Minuten
        ZonedDateTime cooldownSince = ZonedDateTime.now(BERLIN).minusMinutes(COOLDOWN_MINUTES);
        boolean recentWalk = walkRepository.findEntriesSince(cooldownSince).stream()
                .anyMatch(w -> w.getPerson().equalsIgnoreCase(person));
        if (recentWalk) {
            return ResponseEntity.ok(Map.of("status", "recent", "person", person));
        }

        WalkEntry entry = walkService.addWalk(person, null);
        return ResponseEntity.ok(Map.of("status", "logged", "person", person, "walkId", entry.getId()));
    }

    /**
     * Macht den letzten NFC-Eintrag rückgängig — nur wenn er jünger als 2 Minuten ist.
     * Kein Auth nötig, da zeitlich stark begrenzt.
     */
    @DeleteMapping("/walk/{walkId}")
    public ResponseEntity<String> undoWalk(@PathVariable Long walkId) {
        var entry = walkRepository.findById(walkId);
        if (entry.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Eintrag nicht gefunden.");
        }
        ZonedDateTime cutoff = ZonedDateTime.now(BERLIN).minusMinutes(2);
        if (entry.get().getTime().isBefore(cutoff)) {
            return ResponseEntity.status(HttpStatus.GONE).body("Eintrag zu alt zum Rückgängigmachen.");
        }
        walkRepository.deleteById(walkId);
        return ResponseEntity.ok("Eintrag gelöscht.");
    }

    static class FingerprintRequest {
        private String fullHash;
        private String stableHash;
        public String getFullHash() { return fullHash; }
        public void setFullHash(String fullHash) { this.fullHash = fullHash; }
        public String getStableHash() { return stableHash; }
        public void setStableHash(String stableHash) { this.stableHash = stableHash; }
    }

    static class RegisterRequest {
        private String fullHash;
        private String stableHash;
        private String person;
        public String getFullHash() { return fullHash; }
        public void setFullHash(String fullHash) { this.fullHash = fullHash; }
        public String getStableHash() { return stableHash; }
        public void setStableHash(String stableHash) { this.stableHash = stableHash; }
        public String getPerson() { return person; }
        public void setPerson(String person) { this.person = person; }
    }
}
