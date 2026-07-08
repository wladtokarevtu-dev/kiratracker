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
    private final WeatherService weatherService;

    public ReminderService(WalkService walkService, NotificationService notificationService,
                           PauseRepository pauseRepository, WeatherService weatherService) {
        this.walkService = walkService;
        this.notificationService = notificationService;
        this.pauseRepository = pauseRepository;
        this.weatherService = weatherService;
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

    /**
     * Warnt an heftigen Tagen (Ampel-Level 3) morgens um 6 Uhr mit den besten
     * Gassi-Zeitfenstern. Übersprungen im Urlaubsmodus.
     */
    @Scheduled(cron = "0 0 6 * * *", zone = "Europe/Berlin")
    public void checkHeatWarning() {
        if (getPauseIndex() != null) return;

        WeatherForecastDto forecast = weatherService.getTodayForecast();
        if (forecast.getMaxRiskLevel() < 3) return;

        StringBuilder windows = new StringBuilder();
        if (forecast.getMorningWindow() != null) {
            windows.append(forecast.getMorningWindow().getStart())
                   .append("–").append(forecast.getMorningWindow().getEnd());
        }
        if (forecast.getEveningWindow() != null) {
            if (windows.length() > 0) windows.append(" und ");
            windows.append(forecast.getEveningWindow().getStart())
                   .append("–").append(forecast.getEveningWindow().getEnd());
        }

        String message = windows.length() > 0
                ? "🌡️ Heute wird's heiß für Kira — beste Zeiten: " + windows
                : "🌡️ Heute wird's sehr heiß für Kira — heute lieber nur kurze, schattige Runden.";
        notificationService.sendCustomNotification(message);
    }

    private Integer getPauseIndex() {
        return pauseRepository.findById(1L)
                .orElse(new PauseState())
                .getPauseIndex();
    }
}
