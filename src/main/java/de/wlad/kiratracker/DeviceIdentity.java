package de.wlad.kiratracker;

import jakarta.persistence.*;
import java.time.ZonedDateTime;

@Entity
@Table(name = "device_identities")
public class DeviceIdentity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Full fingerprint (inkl. UserAgent) – ändert sich bei Browser-Updates
    @Column(name = "full_hash", nullable = false)
    private String fullHash;

    // Stabiler Fingerprint (nur Hardware: Canvas, Screen, Timezone, CPU-Kerne)
    // Ändert sich kaum, selbst nach Browser-Updates
    @Column(name = "stable_hash", nullable = false)
    private String stableHash;

    @Column(nullable = false)
    private String person;

    @Column(name = "last_seen", nullable = false)
    private ZonedDateTime lastSeen;

    @Column(name = "created_at", nullable = false)
    private ZonedDateTime createdAt;

    public DeviceIdentity() {}

    public DeviceIdentity(String fullHash, String stableHash, String person) {
        this.fullHash = fullHash;
        this.stableHash = stableHash;
        this.person = person;
        this.lastSeen = ZonedDateTime.now();
        this.createdAt = ZonedDateTime.now();
    }

    public Long getId() { return id; }
    public String getFullHash() { return fullHash; }
    public void setFullHash(String fullHash) { this.fullHash = fullHash; }
    public String getStableHash() { return stableHash; }
    public void setStableHash(String stableHash) { this.stableHash = stableHash; }
    public String getPerson() { return person; }
    public void setPerson(String person) { this.person = person; }
    public ZonedDateTime getLastSeen() { return lastSeen; }
    public void setLastSeen(ZonedDateTime lastSeen) { this.lastSeen = lastSeen; }
    public ZonedDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(ZonedDateTime createdAt) { this.createdAt = createdAt; }
}
