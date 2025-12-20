package de.wlad.kiratracker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.ZonedDateTime;

public interface FoodRepository extends JpaRepository<FoodEntry, Long> {

    List<FoodEntry> findByTimeAfterOrderByTimeDesc(ZonedDateTime time);

    // Neue optimierte Methode: nur prüfen ob Einträge existieren
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM FoodEntry f WHERE f.time >= :time")
    boolean existsByTimeAfter(@Param("time") ZonedDateTime time);

    // Alternative: Nur Count zurückgeben
    long countByTimeAfter(ZonedDateTime time);
}