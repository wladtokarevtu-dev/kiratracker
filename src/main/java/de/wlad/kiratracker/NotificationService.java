package de.wlad.kiratracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final RestTemplate restTemplate;
    private final String ntfyUrl;
    private final String ntfyTopic;
    private final WalkService walkService;
    private final PauseRepository pauseRepository;

    // @Lazy on WalkService breaks the circular dependency:
    // WalkService → NotificationService → WalkService
    public NotificationService(
            @Value("${ntfy.url}") String ntfyUrl,
            @Value("${ntfy.topic}") String ntfyTopic,
            @Lazy WalkService walkService,
            PauseRepository pauseRepository) {
        this.ntfyUrl = ntfyUrl;
        this.ntfyTopic = ntfyTopic;
        this.walkService = walkService;
        this.pauseRepository = pauseRepository;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);
        factory.setReadTimeout(3000);
        this.restTemplate = new RestTemplate(factory);
    }

    @Async
    public void sendWalkNotification(String person) {
        send("🐕 " + person + " ist mit Kira Gassi gegangen!");
    }

    @Async
    public void sendFoodNotification(String person) {
        send("🍖 " + person + " hat Kira gefüttert!");
    }

    @Async
    public void sendCustomNotification(String message) {
        send(message);
    }

    @Scheduled(cron = "0 0 10 * * *", zone = "Europe/Berlin")
    public void checkMorningReminder() {
        if (walkService.wasMorning()) return;
        Integer pauseIndex = getPauseIndex();
        if (pauseIndex == null) {
            send("☀️ Kira war heute noch nicht draußen!");
        } else if (pauseIndex == 0) {
            send("✈️ Kira macht Urlaub in Neuglobsow, kein Gassi nötig!");
        }
        // other pause types: silent
    }

    @Scheduled(cron = "0 0 20 * * *", zone = "Europe/Berlin")
    public void checkEveningReminder() {
        if (walkService.wasEvening()) return;
        Integer pauseIndex = getPauseIndex();
        if (pauseIndex == null) {
            send("🌙 Kira braucht noch ihre Abendrunde!");
        } else if (pauseIndex == 0) {
            send("✈️ Kira macht Urlaub in Neuglobsow, kein Gassi nötig!");
        }
        // other pause types: silent
    }

    @Scheduled(cron = "0 0 7 * * *", zone = "Europe/Berlin")
    public void checkAaronReminder() {
        if (getPauseIndex() != null) return; // skip during pause
        if (!walkService.personWalkedInLastDays("Aaron", 3)) {
            send("👀 Aaron war schon länger nicht mit Kira draußen...");
        }
    }

    private Integer getPauseIndex() {
        return pauseRepository.findById(1L)
                .orElse(new PauseState())
                .getPauseIndex();
    }

    private void send(String message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "text/plain; charset=utf-8");
            HttpEntity<String> request = new HttpEntity<>(message, headers);
            restTemplate.postForEntity(ntfyUrl + "/" + ntfyTopic, request, String.class);
        } catch (Exception e) {
            log.warn("ntfy.sh notification failed: {}", e.getMessage());
        }
    }
}
