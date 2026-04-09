package com.globalfieldops.workorder.dto.response;

import java.time.Instant;
import java.util.UUID;

public record WorkOrderCommentResponse(
        UUID id,
        String authorName,
        String body,
        Instant createdAt
) {}