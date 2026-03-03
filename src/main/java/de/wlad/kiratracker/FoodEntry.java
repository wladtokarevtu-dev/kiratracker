package de.wlad.kiratracker;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.ZonedDateTime;

@Entity
@Table(name = "food_entries")
public class FoodEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Person darf nicht leer sein")
    @Size(max = 100)
    @Column(nullable = false)
    private String person;

    @Size(max = 255)
    @Column
    private String food;

    @NotNull
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
