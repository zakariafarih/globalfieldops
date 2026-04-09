package com.globalfieldops.workorder.client;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.globalfieldops.workorder.exception.ServiceCommunicationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditClient {

    private final RestClient auditRestClient;

    public void recordEvent(AuditEventRequest request) {
        try {
            auditRestClient.post()
                    .uri("/api/audit-events")
                    .body(request)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Failed to record audit event {} for entity {}: {}",
                    request.eventType(), request.entityId(), ex.getMessage());
            throw new ServiceCommunicationException("audit-service", ex);
        }
    }
}
