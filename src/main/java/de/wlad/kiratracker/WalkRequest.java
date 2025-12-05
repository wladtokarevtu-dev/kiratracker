package de.wlad.kiratracker;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "walk_requests")
public class WalkRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String requester;

    @Column(nullable = false)
    private String message;

    @Column(nullable = false)
    private ZonedDateTime requestTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status = RequestStatus.PENDING;

    private ZonedDateTime respondedTime;

    // Constructors
    public WalkRequest() {
    }

    public WalkRequest(String requester, String message) {
        this.requester = requester;
        this.message = message;
        this.requestTime = ZonedDateTime.now();
        this.status = RequestStatus.PENDING;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public ZonedDateTime getRespondedTime() {
        return respondedTime;
    }

    public void setRespondedTime(ZonedDateTime respondedTime) {
        this.respondedTime = respondedTime;
    }

    public enum RequestStatus {
        PENDING, ACCEPTED, REJECTED
    }
}
