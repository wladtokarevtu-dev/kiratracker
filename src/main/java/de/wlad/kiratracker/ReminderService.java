package de.wlad.kiratracker;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled reminders. Separated from NotificationService to break the
 * WalkService → NotificationService → WalkService circular dependency.
 */
@Service
public class ReminderService {

    private final WalkService walkService;
    private final NotificationService notificationService;
    private final PauseRepository pauseRepository;

    public ReminderService(WalkService walkService, NotificationService notificationService,
                           PauseRepository pauseRepository) {
        this.walkService = walkService;
        this.notificationService = notificationService;
        this.pauseRepository = pauseRepository;
    }

    @Scheduled(cron = "0 0 11 * * *", zone = "Europe/Berlin")
    public void checkMorningReminder() {
        if (walkService.wasMorning()) return;
        Integer pauseIndex = getPauseIndex();
        if (pauseIndex == null) {
            notificationService.sendCustomNotification(
                    "☀️ War jemand schon mit Kira raus oder hat es jemand vergessen einzutragen?");
        }
        // Bei aktivem Urlaub (pauseIndex != null): keine Erinnerung.
    }

    @Scheduled(cron = "0 0 21 * * *", zone = "Europe/Berlin")
    public void aaronBedtime() {
        notificationService.sendCustomNotification("Schlafenszeit für Aaron, ab ins Bett!");
    }

    @Scheduled(cron = "0 0 22 * * *", zone = "Europe/Berlin")
    public void checkEveningReminder() {
        if (walkService.wasEvening()) return;
        Integer pauseIndex = getPauseIndex();
        if (pauseIndex == null) {
            notificationService.sendCustomNotification(
                    "🌙 War jemand schon mit Kira raus oder hat es jemand vergessen einzutragen?");
        }
        // Bei aktivem Urlaub (pauseIndex != null): keine Erinnerung.
    }

    private Integer getPauseIndex() {
        return pauseRepository.findById(1L)
                .orElse(new PauseState())
                .getPauseIndex();
    }
}
