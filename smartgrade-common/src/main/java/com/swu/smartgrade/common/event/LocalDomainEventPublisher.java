package com.swu.smartgrade.common.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;

@Component
@Profile("!infra")
public class LocalDomainEventPublisher extends AfterCommitEventPublisher {
    private final RestClient client;
    public LocalDomainEventPublisher(
            @Value("${NOTIFICATION_URL:http://127.0.0.1:8087}") String url,
            @Value("${SERVICE_TOKEN:local-service-token}") String token) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(3000);
        client = RestClient.builder().baseUrl(url).requestFactory(factory)
                .defaultHeader("X-Service-Token", token).build();
    }
    protected void send(DomainEvent event) {
        client.post().uri("/internal/notifications/events").body(event).retrieve().toBodilessEntity();
    }
}
