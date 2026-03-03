package de.wlad.kiratracker;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PauseRepository extends JpaRepository<PauseState, Long> {}
