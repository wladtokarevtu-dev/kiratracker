package de.wlad.kiratracker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WalkBlockRepository extends JpaRepository<WalkBlock, Long> {

    List<WalkBlock> findByDay(LocalDate day);

    void deleteByDayBefore(LocalDate day);
}
