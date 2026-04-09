package com.globalfieldops.workorder.client;

import java.util.UUID;

public record AuditEventRequest(
        String eventType,
        String serviceName,
        String entityType,
        UUID entityId,
        String actor,
        String details,
        String correlationId
) {}
