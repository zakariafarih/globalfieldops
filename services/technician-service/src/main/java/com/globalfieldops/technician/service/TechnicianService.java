package com.globalfieldops.technician.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.globalfieldops.technician.dto.request.ChangeActivationRequest;
import com.globalfieldops.technician.dto.request.ChangeAvailabilityRequest;
import com.globalfieldops.technician.dto.request.CreateTechnicianRequest;
import com.globalfieldops.technician.dto.response.TechnicianResponse;
import com.globalfieldops.technician.dto.response.TechnicianSummaryResponse;
import com.globalfieldops.technician.entity.AvailabilityStatus;
import com.globalfieldops.technician.entity.Technician;
import com.globalfieldops.technician.entity.TechnicianSkill;
import com.globalfieldops.common.exception.BadRequestException;
import com.globalfieldops.common.exception.BusinessRuleException;
import com.globalfieldops.common.exception.DuplicateResourceException;
import com.globalfieldops.common.exception.ResourceNotFoundException;
import com.globalfieldops.technician.mapper.TechnicianMapper;
import com.globalfieldops.technician.repository.TechnicianRepository;
import com.globalfieldops.technician.repository.TechnicianSpecification;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
public class TechnicianService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "createdAt", "lastName", "employeeCode", "countryCode"
    );
    private static final int MAX_PAGE_SIZE = 100;

    private final TechnicianRepository technicianRepository;
    private final TechnicianMapper technicianMapper;
    private final Counter techniciansCreatedCounter;

    public TechnicianService(TechnicianRepository technicianRepository,
                             TechnicianMapper technicianMapper,
                             MeterRegistry meterRegistry) {
        this.technicianRepository = technicianRepository;
        this.technicianMapper = technicianMapper;
        this.techniciansCreatedCounter = Counter.builder("technicians.created")
                .description("Total technicians created")
                .register(meterRegistry);
    }

    // ── Create ──────────────────────────────────────────────────────────

    @Transactional
    public TechnicianResponse create(CreateTechnicianRequest request) {
        String normalizedCode = request.employeeCode().trim().toUpperCase();
        String normalizedEmail = request.email().trim().toLowerCase();
        String normalizedCountry = request.countryCode().toUpperCase();

        // Pre-check uniqueness for friendly messages
        if (technicianRepository.existsByEmployeeCode(normalizedCode)) {
            throw new DuplicateResourceException("Technician", "employeeCode", normalizedCode);
        }
        if (technicianRepository.existsByEmail(normalizedEmail)) {
            throw new DuplicateResourceException("Technician", "email", normalizedEmail);
        }

        Technician technician = Technician.builder()
                .employeeCode(normalizedCode)
                .email(normalizedEmail)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .countryCode(normalizedCountry)
                .region(request.region() != null ? request.region().trim() : null)
                .availability(AvailabilityStatus.AVAILABLE)
                .active(true)
                .build();

        // Add skills if provided
        if (request.skills() != null && !request.skills().isEmpty()) {
            validateNoDuplicateSkills(request.skills().stream()
                    .map(s -> s.skillName().trim().toUpperCase())
                    .toList());

            request.skills().forEach(skillReq -> {
                TechnicianSkill skill = TechnicianSkill.builder()
                        .skillName(skillReq.skillName().trim().toUpperCase())
                        .proficiencyLevel(skillReq.proficiencyLevel())
                        .build();
                technician.addSkill(skill);
            });
        }

        try {
            Technician saved = technicianRepository.save(technician);
            log.info("Technician created: employeeCode={}, country={}",
                    saved.getEmployeeCode(), saved.getCountryCode());
            techniciansCreatedCounter.increment();
            return technicianMapper.toResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            throw mapDataIntegrityViolation(ex, normalizedCode, normalizedEmail);
        }
    }

    // ── Read (detail) ───────────────────────────────────────────────────

    public TechnicianResponse findById(UUID id) {
        Technician technician = technicianRepository.findByIdWithSkills(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician", id));
        return technicianMapper.toResponse(technician);
    }

    // ── Read (list) ─────────────────────────────────────────────────────

    public Page<TechnicianSummaryResponse> findAll(
            String countryCode,
            Boolean active,
            AvailabilityStatus availability,
            String skillName,
            Pageable pageable) {

        validatePageSize(pageable);
        validateSortProperties(pageable);

        Specification<Technician> spec = buildFilterSpecification(countryCode, active, availability, skillName);

        Page<Technician> page = spec != null
                ? technicianRepository.findAll(spec, pageable)
                : technicianRepository.findAll(pageable);

        return page.map(technicianMapper::toSummaryResponse);
    }

    // ── Update availability ─────────────────────────────────────────────

    @Transactional
    public TechnicianResponse changeAvailability(UUID id, ChangeAvailabilityRequest request) {
        Technician technician = technicianRepository.findByIdWithSkills(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician", id));

        if (!technician.isActive()) {
            throw new BusinessRuleException("TECHNICIAN_NOT_ACTIVE",
                    "Cannot change availability of an inactive technician");
        }

        technician.setAvailability(request.availability());
        Technician saved = technicianRepository.save(technician);

        log.info("Technician availability changed: id={}, availability={}",
                saved.getId(), saved.getAvailability());
        return technicianMapper.toResponse(saved);
    }

    // ── Update activation ───────────────────────────────────────────────

    @Transactional
    public TechnicianResponse changeActivation(UUID id, ChangeActivationRequest request) {
        Technician technician = technicianRepository.findByIdWithSkills(id)
                .orElseThrow(() -> new ResourceNotFoundException("Technician", id));

        if (request.active()) {
            // Reactivation: active = true, availability stays OFF_DUTY
            technician.setActive(true);
        } else {
            // Deactivation: force active = false AND availability = OFF_DUTY
            technician.setActive(false);
            technician.setAvailability(AvailabilityStatus.OFF_DUTY);
        }

        Technician saved = technicianRepository.save(technician);

        log.info("Technician activation changed: id={}, active={}, availability={}",
                saved.getId(), saved.isActive(), saved.getAvailability());
        return technicianMapper.toResponse(saved);
    }

    // ── Private helpers ─────────────────────────────────────────────────

    private void validateNoDuplicateSkills(List<String> normalizedNames) {
        Set<String> seen = new HashSet<>();
        for (String name : normalizedNames) {
            if (!seen.add(name)) {
                throw new BusinessRuleException("DUPLICATE_SKILL",
                        "Duplicate skill in request: " + name);
            }
        }
    }

    private Specification<Technician> buildFilterSpecification(
            String countryCode, Boolean active, AvailabilityStatus availability, String skillName) {

        List<Specification<Technician>> filters = new ArrayList<>();

        if (countryCode != null) {
            filters.add(TechnicianSpecification.hasCountryCode(countryCode));
        }
        if (active != null) {
            filters.add(TechnicianSpecification.hasActive(active));
        }
        if (availability != null) {
            filters.add(TechnicianSpecification.hasAvailability(availability));
        }
        if (skillName != null) {
            filters.add(TechnicianSpecification.hasSkill(skillName));
        }

        return filters.stream().reduce(Specification::and).orElse(null);
    }

    private void validatePageSize(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new BadRequestException("PAGE_SIZE_EXCEEDED",
                    "Maximum page size is " + MAX_PAGE_SIZE);
        }
    }

    private void validateSortProperties(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                throw new BadRequestException("INVALID_SORT_PROPERTY",
                        "Unsupported sort property: " + order.getProperty()
                                + ". Allowed: " + ALLOWED_SORT_PROPERTIES);
            }
        });
    }

    private RuntimeException mapDataIntegrityViolation(
            DataIntegrityViolationException ex,
            String employeeCode,
            String email) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message != null) {
            if (message.contains("uk_technicians_employee_code")) {
                return new DuplicateResourceException("Technician", "employeeCode", employeeCode);
            }
            if (message.contains("uk_technicians_email")) {
                return new DuplicateResourceException("Technician", "email", email);
            }
        }
        return ex;
    }
}
