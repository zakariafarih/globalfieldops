package com.globalfieldops.workorder.dto.request;

import com.globalfieldops.workorder.entity.WorkOrderStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChangeWorkOrderStatusRequest(

        @NotNull
        WorkOrderStatus targetStatus,

        @NotBlank
        @Size(max = 100)
        String changedBy,

        @Size(max = 500)
        String reason
) {}