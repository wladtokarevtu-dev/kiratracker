package de.wlad.kiratracker;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class WalkBlockServiceTest {

    @Autowired WalkBlockService service;
    @Autowired WalkBlockRepository repository;

    @BeforeEach
    void clean() {
        repository.deleteAll();
    }

    @Test
    void add_withoutNote_throws() {
        assertThatThrownBy(() -> service.add("Aaron", List.of("EVENING"), "  ", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(service.activeToday()).isEmpty();
    }

    @Test
    void add_withNote_appearsInActiveToday() {
        service.add("Aaron", List.of("EVENING"), "Spätschicht", null);

        List<WalkBlock> blocks = service.activeToday();
        assertThat(blocks).hasSize(1);
        assertThat(blocks.get(0).getPerson()).isEqualTo("Aaron");
        assertThat(blocks.get(0).getSlot()).isEqualTo("EVENING");
        assertThat(blocks.get(0).getNote()).isEqualTo("Spätschicht");
    }

    @Test
    void add_multipleSlots_createsOneEach() {
        service.add("Mama", List.of("MORNING", "EVENING"), "Verreist", null);
        assertThat(service.activeToday()).hasSize(2);
    }

    @Test
    void isBlocked_reflectsActiveBlock() {
        service.add("Ilja", List.of("MORNING"), "Krank", null);
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Europe/Berlin"));
        assertThat(service.isBlocked("Ilja", "MORNING", today)).isTrue();
        assertThat(service.isBlocked("Ilja", "EVENING", today)).isFalse();
        assertThat(service.isBlocked("Wlad", "MORNING", today)).isFalse();
    }

    @Test
    void delete_removesBlock() {
        WalkBlock b = service.add("Aaron", List.of("EVENING"), "Spätschicht", null).get(0);
        service.delete(b.getId());
        assertThat(service.activeToday()).isEmpty();
    }
}
