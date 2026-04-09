package com.globalfieldops.audit.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.globalfieldops.audit.dto.request.CreateAuditEventRequest;
import com.globalfieldops.audit.dto.response.AuditEventResponse;
import com.globalfieldops.audit.entity.AuditEvent;
import com.globalfieldops.audit.entity.AuditEventType;
import com.globalfieldops.audit.mapper.AuditEventMapper;
import com.globalfieldops.audit.repository.AuditEventRepository;
import com.globalfieldops.common.exception.BadRequestException;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
public class AuditEventService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> SORTABLE_FIELDS = Set.of("createdAt", "eventType", "serviceName");

    private final AuditEventRepository auditEventRepository;
    private final AuditEventMapper auditEventMapper;
    private final Counter auditEventsCreatedCounter;

    public AuditEventService(AuditEventRepository auditEventRepository,
                             AuditEventMapper auditEventMapper,
                             MeterRegistry meterRegistry) {
        this.auditEventRepository = auditEventRepository;
        this.auditEventMapper = auditEventMapper;
        this.auditEventsCreatedCounter = Counter.builder("audit.events.created")
                .description("Total audit events recorded")
                .register(meterRegistry);
    }

    @Transactional
    public AuditEventResponse recordEvent(CreateAuditEventRequest request) {
        validateEventType(request.eventType());

        AuditEvent entity = auditEventMapper.toEntity(request);
        AuditEvent saved = auditEventRepository.save(entity);

        log.info("Audit event recorded: type={}, entity={}/{}, actor={}",
                saved.getEventType(), saved.getEntityType(), saved.getEntityId(), saved.getActor());

        auditEventsCreatedCounter.increment();
        return auditEventMapper.toResponse(saved);
    }

    public Page<AuditEventResponse> findAll(Pageable pageable) {
        Pageable validated = validateAndNormalize(pageable);
        return auditEventRepository.findAll(validated)
                .map(auditEventMapper::toResponse);
    }

    public Page<AuditEventResponse> findByEventType(String eventType, Pageable pageable) {
        AuditEventType type = parseEventType(eventType);
        Pageable validated = validateAndNormalize(pageable);
        return auditEventRepository.findByEventType(type, validated)
                .map(auditEventMapper::toResponse);
    }

    public Page<AuditEventResponse> findByEntityId(UUID entityId, Pageable pageable) {
        Pageable validated = validateAndNormalize(pageable);
        return auditEventRepository.findByEntityId(entityId, validated)
                .map(auditEventMapper::toResponse);
    }

    public Page<AuditEventResponse> findByServiceName(String serviceName, Pageable pageable) {
        Pageable validated = validateAndNormalize(pageable);
        return auditEventRepository.findByServiceName(serviceName, validated)
                .map(auditEventMapper::toResponse);
    }

    private void validateEventType(String eventType) {
        try {
            AuditEventType.valueOf(eventType);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("INVALID_EVENT_TYPE",
                    "Invalid event type: " + eventType + ". Allowed values: " +
                            java.util.Arrays.toString(AuditEventType.values()));
        }
    }

    private AuditEventType parseEventType(String eventType) {
        try {
            return AuditEventType.valueOf(eventType);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("INVALID_EVENT_TYPE",
                    "Invalid event type: " + eventType + ". Allowed values: " +
                            java.util.Arrays.toString(AuditEventType.values()));
        }
    }

    private Pageable validateAndNormalize(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new BadRequestException("INVALID_PAGE_SIZE",
                    "Page size must not exceed " + MAX_PAGE_SIZE);
        }

        for (Sort.Order order : pageable.getSort()) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new BadRequestException("INVALID_SORT_FIELD",
                        "Invalid sort field: " + order.getProperty() +
                                ". Allowed fields: " + SORTABLE_FIELDS);
            }
        }

        if (pageable.getSort().isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        }

        return pageable;
    }
}
