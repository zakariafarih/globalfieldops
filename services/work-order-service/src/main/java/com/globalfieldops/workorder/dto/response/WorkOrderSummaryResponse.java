package com.globalfieldops.workorder.dto.response;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderSummaryResponse(
        UUID id,
        String title,
        String countryCode,
        String region,
        String priority,
        String status,
        UUID assignedTechnicianId,
        Instant createdAt,
        Instant updatedAt
) {}