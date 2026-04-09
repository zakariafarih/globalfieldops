package com.globalfieldops.technician.dto.response;

import java.util.UUID;

public record TechnicianSkillResponse(
        UUID id,
        String skillName,
        String proficiencyLevel,
        java.time.Instant createdAt
) {}
