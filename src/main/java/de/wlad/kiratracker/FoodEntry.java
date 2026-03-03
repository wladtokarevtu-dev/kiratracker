package de.wlad.kiratracker;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "food_entries")
public class FoodEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String person;

    @Column(length = 255)
    private String food;

    @Column(nullable = false)
    private ZonedDateTime timestamp;

    public FoodEntry() {}

    public FoodEntry(String person, String food, ZonedDateTime timestamp) {
        this.person = person;
        this.food = food;
        this.timestamp = timestamp;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPerson() { return person; }
    public void setPerson(String person) { this.person = person; }
    public String getFood() { return food; }
    public void setFood(String food) { this.food = food; }
    public ZonedDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(ZonedDateTime timestamp) { this.timestamp = timestamp; }
}