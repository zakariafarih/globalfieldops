package com.globalfieldops.audit.service;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import com.globalfieldops.audit.dto.request.CreateAuditEventRequest;
import com.globalfieldops.audit.dto.response.AuditEventResponse;
import com.globalfieldops.audit.entity.AuditEvent;
import com.globalfieldops.audit.entity.AuditEventType;
import com.globalfieldops.audit.mapper.AuditEventMapper;
import com.globalfieldops.audit.repository.AuditEventRepository;
import com.globalfieldops.common.exception.BadRequestException;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditEventServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    @Spy
    private AuditEventMapper auditEventMapper;

    private AuditEventService auditEventService;

    @BeforeEach
    void setUp() {
        auditEventService = new AuditEventService(
                auditEventRepository, auditEventMapper, new SimpleMeterRegistry());
    }

    // ── recordEvent tests ───────────────────────────────────────────────

    @Test
    @DisplayName("Should record audit event with valid request")
    void shouldRecordAuditEventWithValidRequest() {
        UUID entityId = UUID.randomUUID();
        CreateAuditEventRequest request = new CreateAuditEventRequest(
                "WORK_ORDER_CREATED",
                "work-order-service",
                "WorkOrder",
                entityId,
                "system",
                "{\"title\":\"Test order\"}",
                null
        );

        AuditEvent saved = AuditEvent.builder()
                .id(UUID.randomUUID())
                .eventType(AuditEventType.WORK_ORDER_CREATED)
                .serviceName("work-order-service")
                .entityType("WorkOrder")
                .entityId(entityId)
                .actor("system")
                .details("{\"title\":\"Test order\"}")
                .createdAt(Instant.now())
                .build();

        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(saved);

        AuditEventResponse response = auditEventService.recordEvent(request);

        assertThat(response.eventType()).isEqualTo("WORK_ORDER_CREATED");
        assertThat(response.serviceName()).isEqualTo("work-order-service");
        assertThat(response.entityId()).isEqualTo(entityId);
        assertThat(response.actor()).isEqualTo("system");
        verify(auditEventRepository).save(any(AuditEvent.class));
    }

    @Test
    @DisplayName("Should reject invalid event type")
    void shouldRejectInvalidEventType() {
        CreateAuditEventRequest request = new CreateAuditEventRequest(
                "INVALID_TYPE",
                "work-order-service",
                "WorkOrder",
                UUID.randomUUID(),
                "system",
                null,
                null
        );

        assertThatThrownBy(() -> auditEventService.recordEvent(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid event type: INVALID_TYPE");
    }

    // ── findAll tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should return paginated audit events with default sort")
    void shouldReturnPaginatedAuditEventsWithDefaultSort() {
        AuditEvent event = AuditEvent.builder()
                .id(UUID.randomUUID())
                .eventType(AuditEventType.TECHNICIAN_ASSIGNED)
                .serviceName("work-order-service")
                .entityType("WorkOrder")
                .entityId(UUID.randomUUID())
                .actor("system")
                .createdAt(Instant.now())
                .build();

        Page<AuditEvent> page = new PageImpl<>(List.of(event));
        when(auditEventRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<AuditEventResponse> result = auditEventService.findAll(
                PageRequest.of(0, 20));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).eventType()).isEqualTo("TECHNICIAN_ASSIGNED");
    }

    @Test
    @DisplayName("Should reject page size exceeding maximum")
    void shouldRejectPageSizeExceedingMaximum() {
        Pageable oversized = PageRequest.of(0, 200);

        assertThatThrownBy(() -> auditEventService.findAll(oversized))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Page size must not exceed 100");
    }

    @Test
    @DisplayName("Should reject invalid sort field")
    void shouldRejectInvalidSortField() {
        Pageable invalidSort = PageRequest.of(0, 20, Sort.by("actor"));

        assertThatThrownBy(() -> auditEventService.findAll(invalidSort))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid sort field: actor");
    }

    // ── filter tests ────────────────────────────────────────────────────

    @Test
    @DisplayName("Should filter by event type")
    void shouldFilterByEventType() {
        Page<AuditEvent> page = new PageImpl<>(List.of());
        when(auditEventRepository.findByEventType(any(AuditEventType.class), any(Pageable.class)))
                .thenReturn(page);

        Page<AuditEventResponse> result = auditEventService.findByEventType(
                "WORK_ORDER_CREATED", PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
        verify(auditEventRepository).findByEventType(eq(AuditEventType.WORK_ORDER_CREATED), any());
    }

    @Test
    @DisplayName("Should reject invalid event type in filter")
    void shouldRejectInvalidEventTypeInFilter() {
        assertThatThrownBy(() -> auditEventService.findByEventType(
                "NOT_A_TYPE", PageRequest.of(0, 20)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid event type: NOT_A_TYPE");
    }

    @Test
    @DisplayName("Should filter by entity ID")
    void shouldFilterByEntityId() {
        UUID entityId = UUID.randomUUID();
        Page<AuditEvent> page = new PageImpl<>(List.of());
        when(auditEventRepository.findByEntityId(any(UUID.class), any(Pageable.class)))
                .thenReturn(page);

        Page<AuditEventResponse> result = auditEventService.findByEntityId(
                entityId, PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
        verify(auditEventRepository).findByEntityId(eq(entityId), any());
    }
}
