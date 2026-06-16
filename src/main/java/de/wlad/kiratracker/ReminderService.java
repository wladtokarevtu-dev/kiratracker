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

    @Scheduled(cron = "0 0 10 * * *", zone = "Europe/Berlin")
    public void checkMorningReminder() {
        if (walkService.wasMorning()) return;
        Integer pauseIndex = getPauseIndex();
        if (pauseIndex == null) {
            notificationService.sendCustomNotification("☀️ Kira war heute noch nicht draußen!");
        } else if (pauseIndex == 0) {
            notificationService.sendCustomNotification("✈️ Kira macht Urlaub in Neuglobsow, kein Gassi nötig!");
        }
    }

    @Scheduled(cron = "0 0 20 * * *", zone = "Europe/Berlin")
    public void checkEveningReminder() {
        if (walkService.wasEvening()) return;
        Integer pauseIndex = getPauseIndex();
        if (pauseIndex == null) {
            notificationService.sendCustomNotification("🌙 Kira braucht noch ihre Abendrunde!");
        } else if (pauseIndex == 0) {
            notificationService.sendCustomNotification("✈️ Kira macht Urlaub in Neuglobsow, kein Gassi nötig!");
        }
    }

    @Scheduled(cron = "0 0 7 * * *", zone = "Europe/Berlin")
    public void checkAaronReminder() {
        if (getPauseIndex() != null) return;
        if (!walkService.personWalkedInLastDays("Aaron", 3)) {
            notificationService.sendCustomNotification("👀 Aaron war schon länger nicht mit Kira draußen...");
        }
    }

    private Integer getPauseIndex() {
        return pauseRepository.findById(1L)
                .orElse(new PauseState())
                .getPauseIndex();
    }
}
