package de.wlad.kiratracker;
import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
public class FoodEntry {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String person;
    private String description; // Was gab es? (Optional)
    private ZonedDateTime time;

    public FoodEntry() {}
    public FoodEntry(String person, String description, ZonedDateTime time) {
        this.person = person;
        this.description = description;
        this.time = time;
    }
    // Getters
    public Long getId() { return id; }
    public String getPerson() { return person; }
    public String getDescription() { return description; }
    public ZonedDateTime getTime() { return time; }
}