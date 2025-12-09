package de.wlad.kiratracker;
public class FoodEntryDto {
    private Long id;
    private String person;
    private String description;
    private String timeFormatted;
    public FoodEntryDto(Long id, String person, String description, String timeFormatted) {
        this.id = id;
        this.person = person;
        this.description = description;
        this.timeFormatted = timeFormatted;
    }
    public Long getId() { return id; }
    public String getPerson() { return person; }
    public String getDescription() { return description; }
    public String getTimeFormatted() { return timeFormatted; }
}