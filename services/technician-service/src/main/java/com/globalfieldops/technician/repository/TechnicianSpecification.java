package com.globalfieldops.technician.repository;

import org.springframework.data.jpa.domain.Specification;

import com.globalfieldops.technician.entity.AvailabilityStatus;
import com.globalfieldops.technician.entity.Technician;

import jakarta.persistence.criteria.JoinType;

public final class TechnicianSpecification {

    private TechnicianSpecification() {
        // utility class — no instantiation
    }

    public static Specification<Technician> hasCountryCode(String countryCode) {
        return (root, query, cb) ->
                cb.equal(root.get("countryCode"), countryCode.toUpperCase());
    }

    public static Specification<Technician> hasActive(Boolean active) {
        return (root, query, cb) ->
                cb.equal(root.get("active"), active);
    }

    public static Specification<Technician> hasAvailability(AvailabilityStatus availability) {
        return (root, query, cb) ->
                cb.equal(root.get("availability"), availability);
    }

    public static Specification<Technician> hasSkill(String skillName) {
        return (root, query, cb) -> {
            query.distinct(true);
            var skills = root.join("skills", JoinType.INNER);
            return cb.equal(skills.get("skillName"), skillName.toUpperCase());
        };
    }
}
