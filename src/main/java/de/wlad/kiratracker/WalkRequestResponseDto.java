package de.wlad.kiratracker;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class WalkRequestResponseDto {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("HH:mm dd.MM.yy");
    private static final ZoneId BERLIN = ZoneId.of("Europe/Berlin");

    private Long id;
    private String person;
    private String time;
    private String requestTimeFormatted;
    private String status;

    public WalkRequestResponseDto(WalkRequest r) {
        this.id = r.getId();
        this.person = r.getPerson();
        this.time = r.getTime();
        this.requestTimeFormatted = r.getRequestTime() != null
                ? r.getRequestTime().withZoneSameInstant(BERLIN).format(FMT)
                : "—";
        this.status = r.getStatus() != null ? r.getStatus().name() : "UNKNOWN";
    }

    public Long getId() { return id; }
    public String getPerson() { return person; }
    public String getTime() { return time; }
    public String getRequestTimeFormatted() { return requestTimeFormatted; }
    public String getStatus() { return status; }
}
