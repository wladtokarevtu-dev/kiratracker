package de.wlad.kiratracker;

public class WalkEntryDto {
    private Long id;
    private String person;
    private String timeFormatted;
    private Integer applauseCount;

    // Konstruktor
    public WalkEntryDto(Long id, String person, String timeFormatted, Integer applauseCount) {
        this.id = id;
        this.person = person;
        this.timeFormatted = timeFormatted;
        this.applauseCount = applauseCount != null ? applauseCount : 0;
    }

    // Default Konstruktor (für JSON Deserialisierung manchmal nötig)
    public WalkEntryDto() {}

    // Getters
    public Long getId() { return id; }
    public String getPerson() { return person; }
    public String getTimeFormatted() { return timeFormatted; }
    public Integer getApplauseCount() { return applauseCount; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setPerson(String person) { this.person = person; }
    public void setTimeFormatted(String timeFormatted) { this.timeFormatted = timeFormatted; }
    public void setApplauseCount(Integer applauseCount) { this.applauseCount = applauseCount; }
}
