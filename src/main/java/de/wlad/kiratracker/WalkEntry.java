package de.wlad.kiratracker;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "walk_entries")
public class WalkEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String person;

    @Column(nullable = false)
    private LocalDateTime time;

    // JPA braucht einen Default-Constructor
    public WalkEntry() {
    }

    public WalkEntry(String person, LocalDateTime time) {
        this.person = person;
        this.time = time;
    }

    public Long getId() {
        return id;
    }

    public String getPerson() {
        return person;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
