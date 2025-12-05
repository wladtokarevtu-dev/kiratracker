package de.wlad.kiratracker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.ZonedDateTime;
import java.util.List;

@Repository
public interface WalkRepository extends JpaRepository<WalkEntry, Long> {

    List<WalkEntry> findAllByOrderByTimeDesc();

    void deleteByTimeBefore(ZonedDateTime cutoffTime);

    @Query("SELECT w FROM WalkEntry w WHERE w.time >= :startDate ORDER BY w.time DESC")
    List<WalkEntry> findEntriesSince(@Param("startDate") ZonedDateTime startDate);

    @Query("SELECT w.person, COUNT(w) as walkCount FROM WalkEntry w WHERE w.time >= :startDate GROUP BY w.person ORDER BY walkCount DESC")
    List<Object[]> getLeaderboardSince(@Param("startDate") ZonedDateTime startDate);
}
