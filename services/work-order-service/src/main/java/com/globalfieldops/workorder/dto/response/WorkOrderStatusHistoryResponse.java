package com.globalfieldops.workorder.dto.response;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderStatusHistoryResponse(
        UUID id,
        String fromStatus,
        String toStatus,
        String changedBy,
        String changeReason,
        Instant changedAt
) {}