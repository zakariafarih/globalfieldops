package com.globalfieldops.technician;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalfieldops.technician.repository.TechnicianRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class TechnicianIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void cleanDatabase() {
        technicianRepository.deleteAllInBatch();
    }

    // ── Full create flow ────────────────────────────────────────────────

    @Nested
    @DisplayName("Full create flow")
    class FullCreateFlow {

        @Test
        @DisplayName("Should persist technician and return 201 with normalized fields")
        void shouldPersistAndReturn201() throws Exception {
            String json = """
                    {
                      "employeeCode": " emp-int-001 ",
                      "email": "INT.TEST@Example.COM",
                      "firstName": " Alice ",
                      "lastName": " Martinez ",
                      "countryCode": "us"
                    }
                    """;

            mockMvc.perform(post("/api/technicians")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.employeeCode").value("EMP-INT-001"))
                    .andExpect(jsonPath("$.email").value("int.test@example.com"))
                    .andExpect(jsonPath("$.firstName").value("Alice"))
                    .andExpect(jsonPath("$.countryCode").value("US"))
                    .andExpect(jsonPath("$.availability").value("AVAILABLE"))
                    .andExpect(jsonPath("$.active").value(true));

            assertThat(technicianRepository.existsByEmployeeCode("EMP-INT-001")).isTrue();
        }
    }

    // ── Create with skills ──────────────────────────────────────────────

    @Nested
    @DisplayName("Create with skills")
    class CreateWithSkills {

        @Test
        @DisplayName("Should persist technician with skills and return them in response")
        void shouldPersistWithSkills() throws Exception {
            String json = """
                    {
                      "employeeCode": "EMP-SKILL-001",
                      "email": "skilled@example.com",
                      "firstName": "Bob",
                      "lastName": "Welder",
                      "countryCode": "DE",
                      "skills": [
                        {"skillName": "Welding", "proficiencyLevel": "SENIOR"},
                        {"skillName": "Electrical", "proficiencyLevel": "MID"}
                      ]
                    }
                    """;

            mockMvc.perform(post("/api/technicians")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.skills.length()").value(2))
                    .andExpect(jsonPath("$.skills[0].skillName").value("WELDING"));
        }
    }

    // ── Duplicate rejection ─────────────────────────────────────────────

    @Nested
    @DisplayName("Duplicate rejection")
    class DuplicateRejection {

        @Test
        @DisplayName("Should return 409 when employee code already exists")
        void shouldRejectDuplicateEmployeeCode() throws Exception {
            String json = """
                    {
                      "employeeCode": "EMP-DUP-001",
                      "email": "first@example.com",
                      "firstName": "First",
                      "lastName": "Tech",
                      "countryCode": "FR"
                    }
                    """;

            mockMvc.perform(post("/api/technicians")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isCreated());

            String duplicateJson = """
                    {
                      "employeeCode": "emp-dup-001",
                      "email": "second@example.com",
                      "firstName": "Second",
                      "lastName": "Tech",
                      "countryCode": "FR"
                    }
                    """;

            mockMvc.perform(post("/api/technicians")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(duplicateJson))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("DUPLICATE_RESOURCE"));
        }
    }

    // ── Availability change ─────────────────────────────────────────────

    @Nested
    @DisplayName("Availability change flow")
    class AvailabilityChangeFlow {

        @Test
        @DisplayName("Should change availability from AVAILABLE to ON_JOB")
        void shouldChangeAvailability() throws Exception {
            // Create a technician first
            String createJson = """
                    {
                      "employeeCode": "EMP-AVAIL-001",
                      "email": "avail@example.com",
                      "firstName": "Charlie",
                      "lastName": "Delta",
                      "countryCode": "GB"
                    }
                    """;

            String createResult = mockMvc.perform(post("/api/technicians")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            String id = objectMapper.readTree(createResult).get("id").asText();

            // Change availability
            mockMvc.perform(patch("/api/technicians/{id}/availability", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"availability": "ON_JOB"}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.availability").value("ON_JOB"));
        }
    }

    // ── Deactivation flow ───────────────────────────────────────────────

    @Nested
    @DisplayName("Deactivation flow")
    class DeactivationFlow {

        @Test
        @DisplayName("Should deactivate and force OFF_DUTY, then reject availability change")
        void shouldDeactivateAndRejectAvailabilityChange() throws Exception {
            // Create
            String createJson = """
                    {
                      "employeeCode": "EMP-DEACT-001",
                      "email": "deact@example.com",
                      "firstName": "Eve",
                      "lastName": "Fox",
                      "countryCode": "JP"
                    }
                    """;

            String createResult = mockMvc.perform(post("/api/technicians")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(createJson))
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();

            String id = objectMapper.readTree(createResult).get("id").asText();

            // Deactivate
            mockMvc.perform(patch("/api/technicians/{id}/activation", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"active": false}
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.active").value(false))
                    .andExpect(jsonPath("$.availability").value("OFF_DUTY"));

            // Attempt availability change on inactive technician
            mockMvc.perform(patch("/api/technicians/{id}/availability", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"availability": "AVAILABLE"}
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("TECHNICIAN_NOT_ACTIVE"));
        }
    }
}
