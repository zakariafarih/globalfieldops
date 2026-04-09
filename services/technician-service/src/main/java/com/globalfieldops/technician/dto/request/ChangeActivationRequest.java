package com.globalfieldops.technician.dto.request;

import jakarta.validation.constraints.NotNull;

public record ChangeActivationRequest(

        @NotNull
        Boolean active
) {}
