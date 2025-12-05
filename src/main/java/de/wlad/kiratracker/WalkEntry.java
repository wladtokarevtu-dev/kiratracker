package de.wlad.kiratracker;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "walk_entries")
public class WalkEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String person;

    @Column(nullable = false)
    private ZonedDateTime time;

    @Column(nullable = false)
    private Integer applauseCount = 0;

    // JPA braucht einen Default-Constructor
    public WalkEntry() {
    }

    public WalkEntry(String person, ZonedDateTime time) {
        this.person = person;
        this.time = time;
        this.applauseCount = 0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPerson() {
        return person;
    }

    public void setPerson(String person) {
        this.person = person;
    }

    public ZonedDateTime getTime() {
        return time;
    }

    public void setTime(ZonedDateTime time) {
        this.time = time;
    }

    public Integer getApplauseCount() {
        return applauseCount;
    }

    public void setApplauseCount(Integer applauseCount) {
        this.applauseCount = applauseCount;
    }

    public void incrementApplause() {
        this.applauseCount++;
    }
}
