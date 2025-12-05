package de.wlad.kiratracker;

public class WalkRequestDto {
    private String person;
    private String time;

    public WalkRequestDto() {
    }

    public WalkRequestDto(String person, String time) {
        this.person = person;
        this.time = time;
    }

    public String getPerson() {
        return person;
    }

    public void setPerson(String person) {
        this.person = person;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
