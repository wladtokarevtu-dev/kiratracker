package de.wlad.kiratracker;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

@Entity
public class PauseState {

    @Id
    private Long id = 1L; // always one row

    private Integer pauseIndex; // null = no pause

    public PauseState() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getPauseIndex() { return pauseIndex; }
    public void setPauseIndex(Integer pauseIndex) { this.pauseIndex = pauseIndex; }
}
