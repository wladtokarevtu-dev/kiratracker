package de.wlad.kiratracker;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "walk_requests")
public class WalkRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String person;
    private String time;
    private ZonedDateTime requestTime;

    @Enumerated(EnumType.STRING)
    private RequestStatus status;

    public enum RequestStatus {
        PENDING, APPROVED, REJECTED
    }

    // Constructors
    public WalkRequest() {}

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

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public ZonedDateTime getRequestTime() {
        return requestTime;
    }

    public void setRequestTime(ZonedDateTime requestTime) {
        this.requestTime = requestTime;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }
}
