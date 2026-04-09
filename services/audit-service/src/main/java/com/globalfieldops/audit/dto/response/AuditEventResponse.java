package com.globalfieldops.audit.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
        UUID id,
        String eventType,
        String serviceName,
        String entityType,
        UUID entityId,
        String actor,
        String details,
        String correlationId,
        Instant createdAt
) {}
