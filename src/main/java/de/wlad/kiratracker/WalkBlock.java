package de.wlad.kiratracker;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Eine Selbst-Sperre: Person X kann für einen Slot (MORNING/EVENING) eines Tages
 * nicht — mit Pflicht-Notiz. Geblockte Person wird in der Fairness-Rotation für
 * diesen Slot übersprungen.
 */
@Entity
@Table(name = "walk_blocks")
public class WalkBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String person;

    /** "MORNING" oder "EVENING". */
    @Column(nullable = false)
    private String slot;

    @Column(nullable = false)
    private String note;

    @Column(name = "block_day", nullable = false)
    private LocalDate day;

    @Column(nullable = false)
    private ZonedDateTime createdAt;

    public WalkBlock() {}

    public WalkBlock(String person, String slot, String note, LocalDate day, ZonedDateTime createdAt) {
        this.person = person;
        this.slot = slot;
        this.note = note;
        this.day = day;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPerson() { return person; }
    public void setPerson(String person) { this.person = person; }

    public String getSlot() { return slot; }
    public void setSlot(String slot) { this.slot = slot; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public LocalDate getDay() { return day; }
    public void setDay(LocalDate day) { this.day = day; }

    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
