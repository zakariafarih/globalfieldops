package com.globalfieldops.technician.dto.request;

import com.globalfieldops.technician.entity.ProficiencyLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTechnicianSkillRequest(

        @NotBlank
        @Size(max = 100)
        String skillName,

        ProficiencyLevel proficiencyLevel
) {}
