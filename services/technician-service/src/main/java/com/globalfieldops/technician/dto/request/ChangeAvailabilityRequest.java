package com.globalfieldops.technician.dto.request;

import com.globalfieldops.technician.entity.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeAvailabilityRequest(

        @NotNull
        AvailabilityStatus availability
) {}
