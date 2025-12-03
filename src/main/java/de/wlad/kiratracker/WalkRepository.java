package de.wlad.kiratracker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WalkRepository extends JpaRepository<WalkEntry, Long> {

    List<WalkEntry> findByTimeAfter(LocalDateTime time);

    @Modifying
    @Query("DELETE FROM WalkEntry w WHERE w.time < ?1")
    void deleteByTimeBefore(LocalDateTime time);
}
