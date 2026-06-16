package de.wlad.kiratracker;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface DeviceIdentityRepository extends JpaRepository<DeviceIdentity, Long> {
    Optional<DeviceIdentity> findByFullHash(String fullHash);
    Optional<DeviceIdentity> findByStableHash(String stableHash);
}
