package de.wlad.kiratracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FairnessServiceTest {

    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    @Autowired FairnessService fairnessService;
    @Autowired WalkRepository walkRepository;
    @Autowired WalkBlockRepository blockRepository;
    @Autowired WalkBlockService blockService;

    @BeforeEach
    void clean() {
        walkRepository.deleteAll();
        blockRepository.deleteAll();
    }

    private void walk(String person, int hoursAgo) {
        walkRepository.save(new WalkEntry(person, ZonedDateTime.now(BERLIN).minusHours(hoursAgo)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void personWithFewestWalksIsDran() {
        // Wlad 3, Mama 2, Ilja 1, Aaron 0 (im 14-Tage-Fenster)
        walk("Wlad", 1); walk("Wlad", 2); walk("Wlad", 3);
        walk("Mama", 1); walk("Mama", 2);
        walk("Ilja", 1);

        Map<String, Object> f = fairnessService.getFairness();
        assertThat(f.get("dranMorning")).isEqualTo("Aaron");
        assertThat(f.get("dranEvening")).isEqualTo("Aaron");
        assertThat(f.get("paused")).isEqualTo(false);
        assertThat(f.get("window")).isEqualTo(14);

        List<Map<String, Object>> people = (List<Map<String, Object>>) f.get("people");
        Map<String, Object> aaron = people.stream()
                .filter(p -> p.get("name").equals("Aaron")).findFirst().orElseThrow();
        assertThat(aaron.get("count")).isEqualTo(0L);
    }

    @Test
    void blockOnDranPersonShiftsToNext() {
        walk("Wlad", 1); walk("Wlad", 2); walk("Wlad", 3);
        walk("Mama", 1); walk("Mama", 2);
        walk("Ilja", 1);
        // Aaron 0 → wäre dran; blockt aber Abendrunde
        blockService.add("Aaron", List.of("EVENING"), "Spätschicht", null);

        Map<String, Object> f = fairnessService.getFairness();
        assertThat(f.get("dranMorning")).isEqualTo("Aaron");   // morgens unverändert
        assertThat(f.get("dranEvening")).isEqualTo("Ilja");     // nächst-wenigste (1)
    }

    @Test
    void dajenDoesNotCountInRotation() {
        walk("Dajen", 1); walk("Dajen", 2);
        Map<String, Object> f = fairnessService.getFairness();
        // Dajen taucht nicht in der Personenliste auf
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> people = (List<Map<String, Object>>) f.get("people");
        assertThat(people).hasSize(4);
        assertThat(people).noneMatch(p -> p.get("name").equals("Dajen"));
    }
}
