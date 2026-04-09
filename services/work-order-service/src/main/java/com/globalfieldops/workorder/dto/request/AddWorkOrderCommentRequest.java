package com.globalfieldops.workorder.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddWorkOrderCommentRequest(

        @NotBlank
        @Size(max = 100)
        String authorName,

        @NotBlank
        @Size(max = 2000)
        String body
) {}