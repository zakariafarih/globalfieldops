package com.globalfieldops.audit.dto.request;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAuditEventRequest(
        @NotBlank @Size(max = 100) String eventType,
        @NotBlank @Size(max = 100) String serviceName,
        @NotBlank @Size(max = 100) String entityType,
        @NotNull UUID entityId,
        @NotBlank @Size(max = 255) String actor,
        String details,
        @Size(max = 255) String correlationId
) {}
