package com.globalfieldops.technician.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import com.globalfieldops.technician.dto.request.ChangeActivationRequest;
import com.globalfieldops.technician.dto.request.ChangeAvailabilityRequest;
import com.globalfieldops.technician.dto.request.CreateTechnicianRequest;
import com.globalfieldops.technician.dto.request.CreateTechnicianSkillRequest;
import com.globalfieldops.technician.dto.response.TechnicianResponse;
import com.globalfieldops.technician.entity.AvailabilityStatus;
import com.globalfieldops.technician.entity.Technician;
import com.globalfieldops.common.exception.BadRequestException;
import com.globalfieldops.common.exception.BusinessRuleException;
import com.globalfieldops.common.exception.DuplicateResourceException;
import com.globalfieldops.common.exception.ResourceNotFoundException;
import com.globalfieldops.technician.mapper.TechnicianMapper;
import com.globalfieldops.technician.repository.TechnicianRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TechnicianServiceTest {

    @Mock
    private TechnicianRepository technicianRepository;

    @Mock
    private TechnicianMapper technicianMapper;

    private TechnicianService technicianService;

    @BeforeEach
    void setUp() {
        technicianService = new TechnicianService(
                technicianRepository, technicianMapper, new SimpleMeterRegistry());
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private static Technician buildTechnician() {
        Technician tech = Technician.builder()
                .id(UUID.randomUUID())
                .employeeCode("EMP001")
                .email("john@example.com")
                .firstName("John")
                .lastName("Doe")
                .countryCode("US")
                .availability(AvailabilityStatus.AVAILABLE)
                .active(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        return tech;
    }

    private static TechnicianResponse buildResponse(Technician tech) {
        return new TechnicianResponse(
                tech.getId(),
                tech.getEmployeeCode(),
                tech.getEmail(),
                tech.getFirstName(),
                tech.getLastName(),
                tech.getCountryCode(),
                tech.getRegion(),
                tech.getAvailability().name(),
                tech.isActive(),
                List.of(),
                tech.getCreatedAt(),
                tech.getUpdatedAt()
        );
    }

    // ── Create ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Should create technician with normalized fields")
        void shouldCreateWithNormalizedFields() {
            CreateTechnicianRequest request = new CreateTechnicianRequest(
                    " emp001 ", "JOHN@Example.COM", "John", "Doe", "us", null, null);

            Technician saved = buildTechnician();
            TechnicianResponse expected = buildResponse(saved);

            when(technicianRepository.existsByEmployeeCode("EMP001")).thenReturn(false);
            when(technicianRepository.existsByEmail("john@example.com")).thenReturn(false);
            when(technicianRepository.save(any(Technician.class))).thenReturn(saved);
            when(technicianMapper.toResponse(saved)).thenReturn(expected);

            TechnicianResponse result = technicianService.create(request);

            assertThat(result).isEqualTo(expected);
            verify(technicianRepository).save(any(Technician.class));
        }

        @Test
        @DisplayName("Should reject duplicate employeeCode")
        void shouldRejectDuplicateEmployeeCode() {
            CreateTechnicianRequest request = new CreateTechnicianRequest(
                    "EMP001", "new@example.com", "Jane", "Doe", "US", null, null);

            when(technicianRepository.existsByEmployeeCode("EMP001")).thenReturn(true);

            assertThatThrownBy(() -> technicianService.create(request))
                    .isInstanceOf(DuplicateResourceException.class)
                    .hasMessageContaining("employeeCode");

            verify(technicianRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject duplicate skill names in request")
        void shouldRejectDuplicateSkills() {
            List<CreateTechnicianSkillRequest> skills = List.of(
                    new CreateTechnicianSkillRequest("Java", null),
                    new CreateTechnicianSkillRequest("java", null)
            );
            CreateTechnicianRequest request = new CreateTechnicianRequest(
                    "EMP002", "jane@example.com", "Jane", "Doe", "US", null, skills);

            when(technicianRepository.existsByEmployeeCode("EMP002")).thenReturn(false);
            when(technicianRepository.existsByEmail("jane@example.com")).thenReturn(false);

            assertThatThrownBy(() -> technicianService.create(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("JAVA");

            verify(technicianRepository, never()).save(any());
        }
    }

    // ── Find by ID ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Should throw ResourceNotFoundException for unknown ID")
        void shouldThrowWhenNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(technicianRepository.findByIdWithSkills(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> technicianService.findById(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Technician");
        }
    }

    // ── Change availability ─────────────────────────────────────────────

    @Nested
    @DisplayName("changeAvailability()")
    class ChangeAvailability {

        @Test
        @DisplayName("Should reject availability change for inactive technician")
        void shouldRejectWhenInactive() {
            Technician inactive = buildTechnician();
            inactive.setActive(false);

            when(technicianRepository.findByIdWithSkills(inactive.getId()))
                    .thenReturn(Optional.of(inactive));

            ChangeAvailabilityRequest request = new ChangeAvailabilityRequest(AvailabilityStatus.ON_JOB);

            assertThatThrownBy(() -> technicianService.changeAvailability(inactive.getId(), request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("inactive");
        }
    }

    // ── Change activation ───────────────────────────────────────────────

    @Nested
    @DisplayName("changeActivation()")
    class ChangeActivation {

        @Test
        @DisplayName("Should force OFF_DUTY when deactivating")
        void shouldForceOffDutyOnDeactivation() {
            Technician tech = buildTechnician();
            TechnicianResponse expected = buildResponse(tech);

            when(technicianRepository.findByIdWithSkills(tech.getId()))
                    .thenReturn(Optional.of(tech));
            when(technicianRepository.save(any(Technician.class))).thenReturn(tech);
            when(technicianMapper.toResponse(any(Technician.class))).thenReturn(expected);

            technicianService.changeActivation(tech.getId(), new ChangeActivationRequest(false));

            assertThat(tech.isActive()).isFalse();
            assertThat(tech.getAvailability()).isEqualTo(AvailabilityStatus.OFF_DUTY);
        }

        @Test
        @DisplayName("Should keep OFF_DUTY when reactivating")
        void shouldKeepOffDutyOnReactivation() {
            Technician tech = buildTechnician();
            tech.setActive(false);
            tech.setAvailability(AvailabilityStatus.OFF_DUTY);
            TechnicianResponse expected = buildResponse(tech);

            when(technicianRepository.findByIdWithSkills(tech.getId()))
                    .thenReturn(Optional.of(tech));
            when(technicianRepository.save(any(Technician.class))).thenReturn(tech);
            when(technicianMapper.toResponse(any(Technician.class))).thenReturn(expected);

            technicianService.changeActivation(tech.getId(), new ChangeActivationRequest(true));

            assertThat(tech.isActive()).isTrue();
            assertThat(tech.getAvailability()).isEqualTo(AvailabilityStatus.OFF_DUTY);
        }
    }

    // ── Pagination validation ───────────────────────────────────────────

    @Nested
    @DisplayName("findAll() validation")
    class FindAllValidation {

        @Test
        @DisplayName("Should reject page size exceeding maximum")
        void shouldRejectOversizePageSize() {
            PageRequest oversized = PageRequest.of(0, 200, Sort.by("createdAt"));

            assertThatThrownBy(() -> technicianService.findAll(null, null, null, null, oversized))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Maximum page size");
        }

        @Test
        @DisplayName("Should reject unsupported sort property")
        void shouldRejectInvalidSortProperty() {
            PageRequest badSort = PageRequest.of(0, 20, Sort.by("password"));

            assertThatThrownBy(() -> technicianService.findAll(null, null, null, null, badSort))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("password");
        }
    }
}
