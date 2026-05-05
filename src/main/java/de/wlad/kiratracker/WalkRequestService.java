package de.wlad.kiratracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class WalkRequestService {

    private static final ZoneId BERLIN_ZONE = ZoneId.of("Europe/Berlin");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm");

    private final WalkRequestRepository requestRepository;
    private final WalkService walkService;
    private final NotificationService notificationService;

    @Autowired
    public WalkRequestService(WalkRequestRepository requestRepository, WalkService walkService,
                              NotificationService notificationService) {
        this.requestRepository = requestRepository;
        this.walkService = walkService;
        this.notificationService = notificationService;
    }

    @Transactional
    public void createRequest(String person, String time) {
        WalkRequest request = new WalkRequest();
        request.setPerson(person);
        request.setTime(time);
        request.setRequestTime(ZonedDateTime.now(BERLIN_ZONE));
        request.setStatus(WalkRequest.RequestStatus.PENDING);
        requestRepository.save(request);
        notificationService.sendWalkNotification(person);
    }

    @Transactional
    public void approveRequest(Long id) {
        WalkRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request nicht gefunden"));

        request.setStatus(WalkRequest.RequestStatus.APPROVED);
        requestRepository.save(request);

        // Always pass an explicit time so addWalk treats it as non-fresh (no double notification).
        // If the request had no time, fall back to when the request was submitted.
        String walkTime = (request.getTime() != null && !request.getTime().isEmpty())
                ? request.getTime()
                : request.getRequestTime().withZoneSameInstant(BERLIN_ZONE).format(TIME_FORMAT);
        walkService.addWalk(request.getPerson(), walkTime);
    }

    @Transactional
    public void rejectRequest(Long id) {
        WalkRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request nicht gefunden"));

        request.setStatus(WalkRequest.RequestStatus.REJECTED);
        requestRepository.save(request);
    }

    public int getPendingRequestsCount() {
        return (int) requestRepository.countByStatus(WalkRequest.RequestStatus.PENDING);
    }
}
