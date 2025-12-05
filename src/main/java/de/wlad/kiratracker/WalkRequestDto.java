package de.wlad.kiratracker;

public class WalkRequestDto {
    private Long id;
    private String requester;
    private String message;
    private String requestTimeFormatted;
    private String status;
    private String respondedTimeFormatted;

    public WalkRequestDto() {
    }

    public WalkRequestDto(Long id, String requester, String message, String requestTimeFormatted,
                          String status, String respondedTimeFormatted) {
        this.id = id;
        this.requester = requester;
        this.message = message;
        this.requestTimeFormatted = requestTimeFormatted;
        this.status = status;
        this.respondedTimeFormatted = respondedTimeFormatted;
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

    public String getRequestTimeFormatted() {
        return requestTimeFormatted;
    }

    public void setRequestTimeFormatted(String requestTimeFormatted) {
        this.requestTimeFormatted = requestTimeFormatted;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRespondedTimeFormatted() {
        return respondedTimeFormatted;
    }

    public void setRespondedTimeFormatted(String respondedTimeFormatted) {
        this.respondedTimeFormatted = respondedTimeFormatted;
    }
}
