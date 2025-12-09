package de.wlad.kiratracker;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.ZonedDateTime;

public interface FoodRepository extends JpaRepository<FoodEntry, Long> {
    List<FoodEntry> findByTimeAfterOrderByTimeDesc(ZonedDateTime time);
}