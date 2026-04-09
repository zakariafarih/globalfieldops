package com.globalfieldops.technician.dto.response;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record TechnicianResponse(
        UUID id,
        String employeeCode,
        String email,
        String firstName,
        String lastName,
        String countryCode,
        String region,
        String availability,
        boolean active,
        List<TechnicianSkillResponse> skills,
        Instant createdAt,
        Instant updatedAt
) {}
