package com.globalfieldops.audit;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.globalfieldops.audit.repository.AuditEventRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class AuditEventIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @BeforeEach
    void setUp() {
        auditEventRepository.deleteAll();
    }

    // ── POST /api/audit-events ──────────────────────────────────────────

    @Nested
    @DisplayName("Record Audit Event")
    class RecordAuditEvent {

        @Test
        @DisplayName("Should persist audit event and return 201")
        void shouldPersistAuditEventAndReturn201() throws Exception {
            UUID entityId = UUID.randomUUID();

            mockMvc.perform(post("/api/audit-events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "eventType": "WORK_ORDER_CREATED",
                                        "serviceName": "work-order-service",
                                        "entityType": "WorkOrder",
                                        "entityId": "%s",
                                        "actor": "system",
                                        "details": "{\\"title\\":\\"Router replacement\\"}"
                                    }
                                    """.formatted(entityId)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").exists())
                    .andExpect(jsonPath("$.eventType").value("WORK_ORDER_CREATED"))
                    .andExpect(jsonPath("$.serviceName").value("work-order-service"))
                    .andExpect(jsonPath("$.entityType").value("WorkOrder"))
                    .andExpect(jsonPath("$.entityId").value(entityId.toString()))
                    .andExpect(jsonPath("$.actor").value("system"))
                    .andExpect(jsonPath("$.createdAt").exists());

            assertThat(auditEventRepository.count()).isEqualTo(1);
        }

        @Test
        @DisplayName("Should reject invalid event type with 400")
        void shouldRejectInvalidEventType() throws Exception {
            mockMvc.perform(post("/api/audit-events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "eventType": "NOT_A_REAL_TYPE",
                                        "serviceName": "work-order-service",
                                        "entityType": "WorkOrder",
                                        "entityId": "%s",
                                        "actor": "system"
                                    }
                                    """.formatted(UUID.randomUUID())))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_EVENT_TYPE"));

            assertThat(auditEventRepository.count()).isEqualTo(0);
        }

        @Test
        @DisplayName("Should reject missing required fields with 400")
        void shouldRejectMissingRequiredFields() throws Exception {
            mockMvc.perform(post("/api/audit-events")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                        "eventType": "WORK_ORDER_CREATED"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
        }
    }

    // ── GET /api/audit-events ───────────────────────────────────────────

    @Nested
    @DisplayName("Query Audit Events")
    class QueryAuditEvents {

        @Test
        @DisplayName("Should return all events paginated")
        void shouldReturnAllEventsPaginated() throws Exception {
            // Seed two events
            createEvent("WORK_ORDER_CREATED", "WorkOrder");
            createEvent("TECHNICIAN_ASSIGNED", "WorkOrder");

            mockMvc.perform(get("/api/audit-events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content").isArray())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("Should filter events by event type")
        void shouldFilterEventsByEventType() throws Exception {
            createEvent("WORK_ORDER_CREATED", "WorkOrder");
            createEvent("TECHNICIAN_ASSIGNED", "WorkOrder");
            createEvent("WORK_ORDER_CREATED", "WorkOrder");

            mockMvc.perform(get("/api/audit-events")
                            .param("eventType", "WORK_ORDER_CREATED"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        @DisplayName("Should filter events by entity ID")
        void shouldFilterEventsByEntityId() throws Exception {
            UUID targetEntity = UUID.randomUUID();
            createEventForEntity("WORK_ORDER_CREATED", targetEntity);
            createEvent("TECHNICIAN_ASSIGNED", "WorkOrder");

            mockMvc.perform(get("/api/audit-events")
                            .param("entityId", targetEntity.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].entityId").value(targetEntity.toString()));
        }

        @Test
        @DisplayName("Should return empty page when no events exist")
        void shouldReturnEmptyPageWhenNoEvents() throws Exception {
            mockMvc.perform(get("/api/audit-events"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.content").isEmpty());
        }
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private void createEvent(String eventType, String entityType) throws Exception {
        createEventForEntity(eventType, UUID.randomUUID());
    }

    private void createEventForEntity(String eventType, UUID entityId) throws Exception {
        mockMvc.perform(post("/api/audit-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "eventType": "%s",
                                    "serviceName": "work-order-service",
                                    "entityType": "WorkOrder",
                                    "entityId": "%s",
                                    "actor": "system"
                                }
                                """.formatted(eventType, entityId)))
                .andExpect(status().isCreated());
    }
}
