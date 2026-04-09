package com.globalfieldops.technician.mapper;

import com.globalfieldops.technician.dto.response.TechnicianResponse;
import com.globalfieldops.technician.dto.response.TechnicianSkillResponse;
import com.globalfieldops.technician.dto.response.TechnicianSummaryResponse;
import com.globalfieldops.technician.entity.Technician;
import com.globalfieldops.technician.entity.TechnicianSkill;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TechnicianMapper {

    public TechnicianResponse toResponse(Technician entity) {
        return new TechnicianResponse(
                entity.getId(),
                entity.getEmployeeCode(),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getCountryCode(),
                entity.getRegion(),
                entity.getAvailability().name(),
                entity.isActive(),
                toSkillResponseList(entity.getSkills()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public TechnicianSummaryResponse toSummaryResponse(Technician entity) {
        return new TechnicianSummaryResponse(
                entity.getId(),
                entity.getEmployeeCode(),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getCountryCode(),
                entity.getRegion(),
                entity.getAvailability().name(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }

    public TechnicianSkillResponse toSkillResponse(TechnicianSkill entity) {
        return new TechnicianSkillResponse(
                entity.getId(),
                entity.getSkillName(),
                entity.getProficiencyLevel() != null
                        ? entity.getProficiencyLevel().name()
                        : null,
                entity.getCreatedAt()
        );
    }

    private List<TechnicianSkillResponse> toSkillResponseList(List<TechnicianSkill> skills) {
        return skills.stream()
                .map(this::toSkillResponse)
                .toList();
    }
}
