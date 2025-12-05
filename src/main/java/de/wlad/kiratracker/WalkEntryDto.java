package de.wlad.kiratracker;

public class WalkEntryDto {
    private Long id;
    private String person;
    private String timeFormatted;
    private Integer applauseCount;

    public WalkEntryDto() {
    }

    public WalkEntryDto(Long id, String person, String timeFormatted, Integer applauseCount) {
        this.id = id;
        this.person = person;
        this.timeFormatted = timeFormatted;
        this.applauseCount = applauseCount;
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

    public String getTimeFormatted() {
        return timeFormatted;
    }

    public void setTimeFormatted(String timeFormatted) {
        this.timeFormatted = timeFormatted;
    }

    public Integer getApplauseCount() {
        return applauseCount;
    }

    public void setApplauseCount(Integer applauseCount) {
        this.applauseCount = applauseCount;
    }
}
