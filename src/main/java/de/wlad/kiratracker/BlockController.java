package de.wlad.kiratracker;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Selbst-Blockieren: Personen können sich für Morgen-/Abendrunde eines Tages
 * sperren (Pflicht-Notiz). Nicht admin-gated (Haushalts-App, vgl. D4).
 */
@RestController
public class BlockController {

    private final WalkBlockService blockService;

    public BlockController(WalkBlockService blockService) {
        this.blockService = blockService;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @GetMapping("/blocks")
    public ResponseEntity<List<Map<String, Object>>> getBlocks() {
        List<Map<String, Object>> result = blockService.activeToday().stream()
                .map(b -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", b.getId());
                    m.put("person", b.getPerson());
                    m.put("slot", b.getSlot());
                    m.put("note", b.getNote());
                    return m;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/block")
    public ResponseEntity<String> addBlock(@RequestBody BlockRequest request) {
        blockService.add(request.getPerson(), request.getSlots(), request.getNote(), null);
        return ResponseEntity.ok("Sperre eingetragen.");
    }

    @DeleteMapping("/block/{id}")
    public ResponseEntity<String> deleteBlock(@PathVariable Long id) {
        blockService.delete(id);
        return ResponseEntity.ok("Sperre aufgehoben.");
    }

    static class BlockRequest {
        private String person;
        private List<String> slots;
        private String note;
        public String getPerson() { return person; }
        public void setPerson(String person) { this.person = person; }
        public List<String> getSlots() { return slots; }
        public void setSlots(List<String> slots) { this.slots = slots; }
        public String getNote() { return note; }
        public void setNote(String note) { this.note = note; }
    }
}
