package de.wlad.kiratracker;

public class FoodEntryDto {
    private Long id;
    private String person;
    private String food;
    private String timestamp;

    public FoodEntryDto() {}

    public FoodEntryDto(Long id, String person, String food, String timestamp) {
        this.id = id;
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
    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}
