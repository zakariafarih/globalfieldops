package com.globalfieldops.audit.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.globalfieldops.audit.entity.AuditEvent;
import com.globalfieldops.audit.entity.AuditEventType;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    Page<AuditEvent> findByEventType(AuditEventType eventType, Pageable pageable);

    Page<AuditEvent> findByEntityId(UUID entityId, Pageable pageable);

    Page<AuditEvent> findByServiceName(String serviceName, Pageable pageable);
}
