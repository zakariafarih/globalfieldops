package com.globalfieldops.workorder.dto.response;

import java.util.UUID;

public record TechnicianValidationResponse(
        UUID id,
        String countryCode,
        boolean active,
        String availability
) {}
