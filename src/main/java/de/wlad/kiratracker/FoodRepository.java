package de.wlad.kiratracker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface FoodRepository extends JpaRepository<FoodEntry, Long> {

    List<FoodEntry> findAllByOrderByTimestampDesc();

    List<FoodEntry> findByTimestampBetweenOrderByTimestampDesc(ZonedDateTime start, ZonedDateTime end);

    void deleteByTimestampBefore(ZonedDateTime cutoff);
}
