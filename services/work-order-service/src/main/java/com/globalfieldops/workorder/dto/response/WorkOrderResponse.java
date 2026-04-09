package com.globalfieldops.workorder.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkOrderResponse(
        UUID id,
        String title,
        String summary,
        String countryCode,
        String region,
        String priority,
        String status,
        UUID assignedTechnicianId,
        List<WorkOrderCommentResponse> comments,
        List<WorkOrderStatusHistoryResponse> statusHistory,
        Instant createdAt,
        Instant updatedAt
) {}