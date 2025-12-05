package de.wlad.kiratracker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
public class WalkRequestService {

    private final WalkRequestRepository requestRepository;
    private final WalkService walkService;

    @Autowired
    public WalkRequestService(WalkRequestRepository requestRepository, WalkService walkService) {
        this.requestRepository = requestRepository;
        this.walkService = walkService;
    }

    @Transactional
    public void createRequest(String person, String time) {
        WalkRequest request = new WalkRequest();
        request.setPerson(person);
        request.setTime(time);
        request.setRequestTime(ZonedDateTime.now());
        request.setStatus(WalkRequest.RequestStatus.PENDING);
        requestRepository.save(request);
    }

    @Transactional
    public void approveRequest(Long id) {
        WalkRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Request nicht gefunden"));

        request.setStatus(WalkRequest.RequestStatus.APPROVED);
        requestRepository.save(request);

        walkService.addWalk(request.getPerson(), request.getTime());
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
