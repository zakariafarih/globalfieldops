package com.globalfieldops.audit.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.globalfieldops.audit.dto.request.CreateAuditEventRequest;
import com.globalfieldops.audit.dto.response.AuditEventResponse;
import com.globalfieldops.audit.security.SecurityConfig;
import com.globalfieldops.audit.service.AuditEventService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuditEventController.class)
@Import(SecurityConfig.class)
class AuditEventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuditEventService auditEventService;

    private static final UUID ENTITY_ID = UUID.fromString("3fa85f64-5717-4562-b3fc-2c963f66afa6");
    private static final UUID EVENT_ID = UUID.fromString("7c9e6679-7425-40de-944b-e07fc1f90ae7");

    // ── POST /api/audit-events ──────────────────────────────────────────

    @Test
    @DisplayName("Should return 201 with valid audit event request")
    void shouldReturn201WithValidRequest() throws Exception {
        AuditEventResponse response = new AuditEventResponse(
                EVENT_ID,
                "WORK_ORDER_CREATED",
                "work-order-service",
                "WorkOrder",
                ENTITY_ID,
                "system",
                "{\"title\":\"Test\"}",
                null,
                Instant.parse("2026-04-01T10:00:00Z")
        );

        when(auditEventService.recordEvent(any(CreateAuditEventRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/audit-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "eventType": "WORK_ORDER_CREATED",
                                    "serviceName": "work-order-service",
                                    "entityType": "WorkOrder",
                                    "entityId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                    "actor": "system",
                                    "details": "{\\"title\\":\\"Test\\"}"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(EVENT_ID.toString()))
                .andExpect(jsonPath("$.eventType").value("WORK_ORDER_CREATED"))
                .andExpect(jsonPath("$.serviceName").value("work-order-service"))
                .andExpect(jsonPath("$.entityId").value(ENTITY_ID.toString()))
                .andExpect(jsonPath("$.actor").value("system"));
    }

    @Test
    @DisplayName("Should return 400 when eventType is missing")
    void shouldReturn400WhenEventTypeIsMissing() throws Exception {
        mockMvc.perform(post("/api/audit-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "serviceName": "work-order-service",
                                    "entityType": "WorkOrder",
                                    "entityId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
                                    "actor": "system"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.eventType").exists());
    }

    @Test
    @DisplayName("Should return 400 when entityId is missing")
    void shouldReturn400WhenEntityIdIsMissing() throws Exception {
        mockMvc.perform(post("/api/audit-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "eventType": "WORK_ORDER_CREATED",
                                    "serviceName": "work-order-service",
                                    "entityType": "WorkOrder",
                                    "actor": "system"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.details.entityId").exists());
    }

    @Test
    @DisplayName("Should return 400 when request body is empty")
    void shouldReturn400WhenBodyIsEmpty() throws Exception {
        mockMvc.perform(post("/api/audit-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    @DisplayName("Should return 400 with malformed JSON")
    void shouldReturn400WithMalformedJson() throws Exception {
        mockMvc.perform(post("/api/audit-events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"));
    }

    // ── GET /api/audit-events ───────────────────────────────────────────

    @Test
    @DisplayName("Should return 200 with paginated audit events")
    void shouldReturn200WithPaginatedEvents() throws Exception {
        AuditEventResponse response = new AuditEventResponse(
                EVENT_ID,
                "TECHNICIAN_ASSIGNED",
                "work-order-service",
                "WorkOrder",
                ENTITY_ID,
                "system",
                null,
                null,
                Instant.parse("2026-04-01T10:00:00Z")
        );

        when(auditEventService.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(response)));

        mockMvc.perform(get("/api/audit-events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].eventType").value("TECHNICIAN_ASSIGNED"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    @DisplayName("Should return 200 when filtering by event type")
    void shouldReturn200WhenFilteringByEventType() throws Exception {
        when(auditEventService.findByEventType(any(String.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/audit-events")
                        .param("eventType", "WORK_ORDER_CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("Should return 200 when filtering by entity ID")
    void shouldReturn200WhenFilteringByEntityId() throws Exception {
        when(auditEventService.findByEntityId(any(UUID.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/audit-events")
                        .param("entityId", ENTITY_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }
}
