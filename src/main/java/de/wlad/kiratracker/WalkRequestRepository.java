package de.wlad.kiratracker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WalkRequestRepository extends JpaRepository<WalkRequest, Long> {

    List<WalkRequest> findAllByOrderByRequestTimeDesc();

    List<WalkRequest> findByStatusOrderByRequestTimeDesc(WalkRequest.RequestStatus status);

    long countByStatus(WalkRequest.RequestStatus status);
}
