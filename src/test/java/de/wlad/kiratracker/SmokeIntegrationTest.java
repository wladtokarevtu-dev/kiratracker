package de.wlad.kiratracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke-Verifikation des Redesigns: bildet die Plan-curls als echte HTTP-Calls
 * gegen einen laufenden Server (Zufallsport, H2) ab.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SmokeIntegrationTest {

    @Autowired TestRestTemplate rest;
    @Autowired WalkRepository walkRepository;
    @Autowired WalkBlockRepository blockRepository;
    @Autowired PauseRepository pauseRepository;

    @BeforeEach
    void clean() {
        walkRepository.deleteAll();
        blockRepository.deleteAll();
        pauseRepository.deleteById(1L);
    }

    private HttpEntity<String> json(String body) {
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(body, h);
    }

    @Test
    void indexPageIsServed() {
        ResponseEntity<String> res = rest.getForEntity("/", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).contains("kiratracker");
        assertThat(res.getBody()).doesNotContain("FAIRCNT"); // kein Mock-Datensatz
    }

    @Test
    void statusAndLeaderboardRespond() {
        assertThat(rest.getForEntity("/status", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(rest.getForEntity("/leaderboard?days=14", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @SuppressWarnings("unchecked")
    void blockShiftsFairnessAwayFromBlockedPerson() {
        // Wlad/Mama/Ilja haben Runden, Aaron 0 → Aaron wäre dran
        rest.postForEntity("/walk", json("{\"person\":\"Wlad\"}"), String.class);
        rest.postForEntity("/walk", json("{\"person\":\"Mama\"}"), String.class);
        rest.postForEntity("/walk", json("{\"person\":\"Ilja\"}"), String.class);

        Map<String,Object> before = rest.getForObject("/fairness", Map.class);
        assertThat(before.get("dranEvening")).isEqualTo("Aaron");

        // Aaron blockt Abendrunde
        ResponseEntity<String> block = rest.postForEntity("/block",
                json("{\"person\":\"Aaron\",\"slots\":[\"EVENING\"],\"note\":\"Spätschicht\"}"), String.class);
        assertThat(block.getStatusCode()).isEqualTo(HttpStatus.OK);

        List<Map<String,Object>> blocks = rest.getForObject("/blocks", List.class);
        assertThat(blocks).anyMatch(b -> "Aaron".equals(b.get("person")));

        Map<String,Object> after = rest.getForObject("/fairness", Map.class);
        assertThat(after.get("dranEvening")).isNotEqualTo("Aaron");
    }

    @Test
    void blockWithoutNoteIsRejected() {
        ResponseEntity<String> res = rest.postForEntity("/block",
                json("{\"person\":\"Aaron\",\"slots\":[\"EVENING\"]}"), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void backdatedWalkIsAccepted() {
        ResponseEntity<String> res = rest.postForEntity("/walk",
                json("{\"person\":\"Mama\",\"time\":\"20.06.26 08:15\"}"), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @SuppressWarnings("unchecked")
    void ungatedPauseToggles() {
        rest.postForEntity("/pause", json("{\"index\":0}"), String.class);
        Map<String,Object> p = rest.getForObject("/pause", Map.class);
        assertThat(p.get("active")).isEqualTo(true);

        rest.exchange("/pause", HttpMethod.DELETE, null, String.class);
        Map<String,Object> p2 = rest.getForObject("/pause", Map.class);
        assertThat(p2.get("active")).isEqualTo(false);
    }

    @Test
    void adminEndpointsRequireAuth() {
        ResponseEntity<String> res = rest.getForEntity("/admin/walk/request", String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
