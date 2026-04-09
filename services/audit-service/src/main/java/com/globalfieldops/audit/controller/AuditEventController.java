package com.globalfieldops.audit.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.globalfieldops.audit.dto.request.CreateAuditEventRequest;
import com.globalfieldops.audit.dto.response.AuditEventResponse;
import com.globalfieldops.audit.service.AuditEventService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/audit-events")
@RequiredArgsConstructor
public class AuditEventController {

    private final AuditEventService auditEventService;

    @PostMapping
    public ResponseEntity<AuditEventResponse> recordEvent(
            @Valid @RequestBody CreateAuditEventRequest request) {
        AuditEventResponse response = auditEventService.recordEvent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<AuditEventResponse>> findAll(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) UUID entityId,
            @RequestParam(required = false) String serviceName,
            Pageable pageable) {

        Page<AuditEventResponse> result;

        if (eventType != null) {
            result = auditEventService.findByEventType(eventType, pageable);
        } else if (entityId != null) {
            result = auditEventService.findByEntityId(entityId, pageable);
        } else if (serviceName != null) {
            result = auditEventService.findByServiceName(serviceName, pageable);
        } else {
            result = auditEventService.findAll(pageable);
        }

        return ResponseEntity.ok(result);
    }
}
