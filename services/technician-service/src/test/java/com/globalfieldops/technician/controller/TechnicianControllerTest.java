package com.globalfieldops.technician.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.globalfieldops.technician.dto.response.TechnicianResponse;
import com.globalfieldops.technician.dto.response.TechnicianSummaryResponse;
import com.globalfieldops.technician.entity.AvailabilityStatus;
import com.globalfieldops.common.exception.BadRequestException;
import com.globalfieldops.common.exception.ResourceNotFoundException;
import com.globalfieldops.technician.security.SecurityConfig;
import com.globalfieldops.technician.service.TechnicianService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TechnicianController.class)
@Import(SecurityConfig.class)
class TechnicianControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TechnicianService technicianService;

    // ── Helpers ─────────────────────────────────────────────────────────

    private static final UUID TECH_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.now();

    private static TechnicianResponse sampleResponse() {
        return new TechnicianResponse(
                TECH_ID, "EMP001", "john@example.com", "John", "Doe",
                "US", null, "AVAILABLE", true, List.of(), NOW, NOW);
    }

    private static TechnicianSummaryResponse sampleSummary() {
        return new TechnicianSummaryResponse(
                TECH_ID, "EMP001", "john@example.com", "John", "Doe",
                "US", null, "AVAILABLE", true, NOW);
    }

    // ── POST /api/technicians ───────────────────────────────────────────

    @Nested
    @DisplayName("POST /api/technicians")
    class CreateEndpoint {

        @Test
        @DisplayName("Should return 201 with valid request")
        void shouldReturn201() throws Exception {
            when(technicianService.create(any())).thenReturn(sampleResponse());

            mockMvc.perform(post("/api/technicians")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "employeeCode": "EMP001",
                                      "email": "john@example.com",
                                      "firstName": "John",
                                      "lastName": "Doe",
                                      "countryCode": "US"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.employeeCode").value("EMP001"))
                    .andExpect(jsonPath("$.email").value("john@example.com"));
        }

        @Test
        @DisplayName("Should return 400 with missing required fields")
        void shouldReturn400ForMissingFields() throws Exception {
            mockMvc.perform(post("/api/technicians")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.details.employeeCode").exists())
                    .andExpect(jsonPath("$.details.email").exists());
        }

        @Test
        @DisplayName("Should return 400 with invalid country code format")
        void shouldReturn400ForBadCountryCode() throws Exception {
            mockMvc.perform(post("/api/technicians")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "employeeCode": "EMP001",
                                      "email": "john@example.com",
                                      "firstName": "John",
                                      "lastName": "Doe",
                                      "countryCode": "USA"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.countryCode").exists());
        }

        @Test
        @DisplayName("Should return 400 for malformed JSON body")
        void shouldReturn400ForMalformedJson() throws Exception {
            mockMvc.perform(post("/api/technicians")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                    .andExpect(jsonPath("$.details.body").exists());
        }
    }

    // ── GET /api/technicians/{id} ───────────────────────────────────────

    @Nested
    @DisplayName("GET /api/technicians/{id}")
    class FindByIdEndpoint {

        @Test
        @DisplayName("Should return 200 with valid ID")
        void shouldReturn200() throws Exception {
            when(technicianService.findById(TECH_ID)).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/technicians/{id}", TECH_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TECH_ID.toString()))
                    .andExpect(jsonPath("$.employeeCode").value("EMP001"));
        }

        @Test
        @DisplayName("Should return 404 for unknown ID")
        void shouldReturn404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(technicianService.findById(unknownId))
                    .thenThrow(new ResourceNotFoundException("Technician", unknownId));

            mockMvc.perform(get("/api/technicians/{id}", unknownId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 400 for invalid UUID format")
        void shouldReturn400ForInvalidUuid() throws Exception {
            mockMvc.perform(get("/api/technicians/{id}", "not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"))
                    .andExpect(jsonPath("$.details.id").exists());
        }
    }

    // ── GET /api/technicians ────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/technicians")
    class FindAllEndpoint {

        @Test
        @DisplayName("Should return 200 with paginated results")
        void shouldReturn200WithPage() throws Exception {
            PageImpl<TechnicianSummaryResponse> page = new PageImpl<>(
                    List.of(sampleSummary()),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                    1);

            when(technicianService.findAll(any(), any(), any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/technicians"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].employeeCode").value("EMP001"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should return 409 for invalid sort property")
        void shouldReturn409ForBadSort() throws Exception {
            when(technicianService.findAll(any(), any(), any(), any(), any()))
                .thenThrow(new BadRequestException("INVALID_SORT_PROPERTY",
                            "Unsupported sort property: password"));

            mockMvc.perform(get("/api/technicians").param("sort", "password"))
                .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_SORT_PROPERTY"));
        }

        @Test
        @DisplayName("Should return 400 for page size exceeding maximum")
        void shouldReturn400ForOversizePageSize() throws Exception {
            when(technicianService.findAll(any(), any(), any(), any(), any()))
                .thenThrow(new BadRequestException("PAGE_SIZE_EXCEEDED",
                            "Maximum page size is 100"));

            mockMvc.perform(get("/api/technicians").param("size", "200"))
                .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("PAGE_SIZE_EXCEEDED"));
        }

        @Test
        @DisplayName("Should return 400 for invalid availability query parameter")
        void shouldReturn400ForInvalidAvailabilityQueryParameter() throws Exception {
            mockMvc.perform(get("/api/technicians").param("availability", "NOT_REAL"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"))
                .andExpect(jsonPath("$.details.availability").exists());
        }
    }

    // ── PATCH /api/technicians/{id}/availability ────────────────────────

    @Nested
    @DisplayName("PATCH /api/technicians/{id}/availability")
    class ChangeAvailabilityEndpoint {

        @Test
        @DisplayName("Should return 200 with valid availability change")
        void shouldReturn200() throws Exception {
            when(technicianService.changeAvailability(eq(TECH_ID), any()))
                    .thenReturn(sampleResponse());

            mockMvc.perform(patch("/api/technicians/{id}/availability", TECH_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"availability": "ON_JOB"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(TECH_ID.toString()));
        }

        @Test
        @DisplayName("Should return 400 with null availability")
        void shouldReturn400ForNullAvailability() throws Exception {
            mockMvc.perform(patch("/api/technicians/{id}/availability", TECH_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.details.availability").exists());
            }

            @Test
            @DisplayName("Should return 400 for invalid availability enum in body")
            void shouldReturn400ForInvalidAvailabilityEnum() throws Exception {
                mockMvc.perform(patch("/api/technicians/{id}/availability", TECH_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"availability": "NOT_REAL"}
                            """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                    .andExpect(jsonPath("$.details.body").exists());
        }
    }

    // ── PATCH /api/technicians/{id}/activation ──────────────────────────

    @Nested
    @DisplayName("PATCH /api/technicians/{id}/activation")
    class ChangeActivationEndpoint {

        @Test
        @DisplayName("Should return 200 with valid activation change")
        void shouldReturn200() throws Exception {
            when(technicianService.changeActivation(eq(TECH_ID), any()))
                    .thenReturn(sampleResponse());

            mockMvc.perform(patch("/api/technicians/{id}/activation", TECH_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"active": false}
                                    """))
                    .andExpect(status().isOk());
        }
    }
}
