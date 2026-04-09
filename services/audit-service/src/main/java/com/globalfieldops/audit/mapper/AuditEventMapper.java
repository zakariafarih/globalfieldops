package com.globalfieldops.audit.mapper;

import com.globalfieldops.audit.dto.request.CreateAuditEventRequest;
import com.globalfieldops.audit.dto.response.AuditEventResponse;
import com.globalfieldops.audit.entity.AuditEvent;
import com.globalfieldops.audit.entity.AuditEventType;
import org.springframework.stereotype.Component;

@Component
public class AuditEventMapper {

    public AuditEvent toEntity(CreateAuditEventRequest request) {
        return AuditEvent.builder()
                .eventType(AuditEventType.valueOf(request.eventType()))
                .serviceName(request.serviceName())
                .entityType(request.entityType())
                .entityId(request.entityId())
                .actor(request.actor())
                .details(request.details())
                .correlationId(request.correlationId())
                .build();
    }

    public AuditEventResponse toResponse(AuditEvent entity) {
        return new AuditEventResponse(
                entity.getId(),
                entity.getEventType().name(),
                entity.getServiceName(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getActor(),
                entity.getDetails(),
                entity.getCorrelationId(),
                entity.getCreatedAt()
        );
    }
}
