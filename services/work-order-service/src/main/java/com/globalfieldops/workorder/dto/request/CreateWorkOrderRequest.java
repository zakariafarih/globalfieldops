package com.globalfieldops.workorder.dto.request;

import com.globalfieldops.workorder.entity.WorkOrderPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateWorkOrderRequest(

        @NotBlank
        @Size(max = 150)
        String title,

        @NotBlank
        @Size(max = 1000)
        String summary,

        @NotBlank
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "must be a 2-letter ISO country code")
        String countryCode,

        @Size(max = 100)
        String region,

        @NotNull
        WorkOrderPriority priority
) {}