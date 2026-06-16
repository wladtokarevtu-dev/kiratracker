package de.wlad.kiratracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestTemplate;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final RestTemplate restTemplate;
    private final String ntfyUrl;
    private final String ntfyTopic;

    public NotificationService(
            @Value("${ntfy.url}") String ntfyUrl,
            @Value("${ntfy.topic}") String ntfyTopic) {
        this.ntfyUrl = ntfyUrl;
        this.ntfyTopic = ntfyTopic;

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
    public void sendWalkRequestNotification(String person) {
        send("📋 " + person + " möchte mit Kira Gassi gehen!");
    }

    @Async
    public void sendFoodNotification(String person) {
        send("🍖 " + person + " hat Kira gefüttert!");
    }

    @Async
    public void sendCustomNotification(String message) {
        send(message);
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
