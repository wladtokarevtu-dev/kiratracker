package de.wlad.kiratracker;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FairnessController {

    private final FairnessService fairnessService;

    public FairnessController(FairnessService fairnessService) {
        this.fairnessService = fairnessService;
    }

    @GetMapping("/fairness")
    public ResponseEntity<Map<String, Object>> fairness() {
        return ResponseEntity.ok(fairnessService.getFairness());
    }
}
