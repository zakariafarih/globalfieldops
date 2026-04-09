package com.globalfieldops.technician.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TechnicianSummaryResponse(
        UUID id,
        String employeeCode,
        String email,
        String firstName,
        String lastName,
        String countryCode,
        String region,
        String availability,
        boolean active,
        Instant createdAt
) {}
