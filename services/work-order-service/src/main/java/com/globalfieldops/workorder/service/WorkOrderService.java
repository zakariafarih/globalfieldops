package com.globalfieldops.workorder.service;

import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.globalfieldops.workorder.dto.request.AddWorkOrderCommentRequest;
import com.globalfieldops.workorder.dto.request.ChangeWorkOrderStatusRequest;
import com.globalfieldops.workorder.dto.request.CreateWorkOrderRequest;
import com.globalfieldops.workorder.dto.response.WorkOrderResponse;
import com.globalfieldops.workorder.dto.response.WorkOrderSummaryResponse;
import com.globalfieldops.workorder.entity.WorkOrder;
import com.globalfieldops.workorder.entity.WorkOrderComment;
import com.globalfieldops.workorder.entity.WorkOrderPriority;
import com.globalfieldops.workorder.entity.WorkOrderStatus;
import com.globalfieldops.workorder.entity.WorkOrderStatusHistory;
import com.globalfieldops.common.exception.BadRequestException;
import com.globalfieldops.common.exception.BusinessRuleException;
import com.globalfieldops.common.exception.ResourceNotFoundException;
import com.globalfieldops.workorder.client.AuditClient;
import com.globalfieldops.workorder.client.AuditEventRequest;
import com.globalfieldops.workorder.client.TechnicianClient;
import com.globalfieldops.workorder.dto.response.TechnicianValidationResponse;
import com.globalfieldops.workorder.exception.ServiceCommunicationException;
import com.globalfieldops.workorder.mapper.WorkOrderMapper;
import com.globalfieldops.workorder.repository.WorkOrderRepository;
import com.globalfieldops.workorder.repository.WorkOrderSpecification;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@Transactional(readOnly = true)
public class WorkOrderService {

    private static final Set<String> ALLOWED_SORT_PROPERTIES = Set.of(
            "createdAt", "updatedAt", "title", "priority", "status", "countryCode"
    );
    private static final int MAX_PAGE_SIZE = 100;
    private static final String INITIAL_STATUS_ACTOR = "system";
    private static final String SOURCE_SERVICE = "work-order-service";
    private static final String ENTITY_TYPE = "WorkOrder";

    private final WorkOrderRepository workOrderRepository;
    private final WorkOrderMapper workOrderMapper;
    private final TechnicianClient technicianClient;
    private final AuditClient auditClient;
    private final ObjectMapper objectMapper;
    private final Counter workOrdersCreatedCounter;
    private final Counter assignmentAttemptsCounter;
    private final Counter assignmentFailuresCounter;

    public WorkOrderService(WorkOrderRepository workOrderRepository,
                            WorkOrderMapper workOrderMapper,
                            TechnicianClient technicianClient,
                            AuditClient auditClient,
                            ObjectMapper objectMapper,
                            MeterRegistry meterRegistry) {
        this.workOrderRepository = workOrderRepository;
        this.workOrderMapper = workOrderMapper;
        this.technicianClient = technicianClient;
        this.auditClient = auditClient;
        this.objectMapper = objectMapper;
        this.workOrdersCreatedCounter = Counter.builder("workorders.created")
                .description("Total work orders created")
                .register(meterRegistry);
        this.assignmentAttemptsCounter = Counter.builder("assignments.attempted")
                .description("Total technician assignment attempts")
                .register(meterRegistry);
        this.assignmentFailuresCounter = Counter.builder("assignments.failed")
                .description("Total failed technician assignment attempts")
                .register(meterRegistry);
    }

    @Transactional
    public WorkOrderResponse create(CreateWorkOrderRequest request) {
        WorkOrder workOrder = WorkOrder.builder()
                .title(request.title().trim())
                .summary(request.summary().trim())
                .countryCode(request.countryCode().trim().toUpperCase())
                .region(normalizeOptionalText(request.region()))
                .priority(request.priority())
                .status(WorkOrderStatus.OPEN)
                .build();

        workOrder.addStatusHistory(WorkOrderStatusHistory.builder()
                .fromStatus(null)
                .toStatus(WorkOrderStatus.OPEN)
                .changedBy(INITIAL_STATUS_ACTOR)
                .changeReason("Work order created")
                .build());

        WorkOrder saved = workOrderRepository.save(workOrder);
        recordAuditEventSafely(
            "WORK_ORDER_CREATED",
            saved.getId(),
            INITIAL_STATUS_ACTOR,
            Map.of(
                "title", saved.getTitle(),
                "priority", saved.getPriority().name(),
                "countryCode", saved.getCountryCode()
            )
        );
        log.info("Work order created: id={}, country={}, priority={}",
                saved.getId(), saved.getCountryCode(), saved.getPriority());
        workOrdersCreatedCounter.increment();
        return workOrderMapper.toResponse(saved);
    }

    public WorkOrderResponse findById(UUID id) {
        WorkOrder workOrder = workOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", id));
        return workOrderMapper.toResponse(workOrder);
    }

    public Page<WorkOrderSummaryResponse> findAll(
            String countryCode,
            WorkOrderPriority priority,
            WorkOrderStatus status,
            Pageable pageable) {

        validatePageSize(pageable);
        validateSortProperties(pageable);

        Specification<WorkOrder> spec = buildFilterSpecification(countryCode, priority, status);

        Page<WorkOrder> page = spec != null
                ? workOrderRepository.findAll(spec, pageable)
                : workOrderRepository.findAll(pageable);

        return page.map(workOrderMapper::toSummaryResponse);
    }

    @Transactional
    public WorkOrderResponse assignTechnician(UUID workOrderId, UUID technicianId) {
        assignmentAttemptsCounter.increment();

        WorkOrder workOrder = workOrderRepository.findByIdWithDetails(workOrderId)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", workOrderId));

        if (workOrder.getStatus() != WorkOrderStatus.OPEN) {
            assignmentFailuresCounter.increment();
            throw new BusinessRuleException("WORK_ORDER_NOT_ASSIGNABLE",
                    "Only work orders in OPEN status can be assigned. Current status: " + workOrder.getStatus());
        }

        TechnicianValidationResponse technician = technicianClient.getTechnician(technicianId);

        if (!technician.active()) {
            assignmentFailuresCounter.increment();
            throw new BusinessRuleException("TECHNICIAN_NOT_ACTIVE",
                    "Technician " + technicianId + " is not active");
        }

        if (!"AVAILABLE".equals(technician.availability())) {
            assignmentFailuresCounter.increment();
            throw new BusinessRuleException("TECHNICIAN_NOT_AVAILABLE",
                    "Technician " + technicianId + " is not available. Current status: " + technician.availability());
        }

        if (!workOrder.getCountryCode().equalsIgnoreCase(technician.countryCode())) {
            assignmentFailuresCounter.increment();
            throw new BusinessRuleException("COUNTRY_MISMATCH",
                    "Technician country " + technician.countryCode()
                            + " does not match work order country " + workOrder.getCountryCode());
        }

        workOrder.setAssignedTechnicianId(technicianId);
        workOrder.setStatus(WorkOrderStatus.ASSIGNED);
        // changedBy is "system" until Phase 5 introduces JWT auth, which will provide the real principal
        workOrder.addStatusHistory(WorkOrderStatusHistory.builder()
                .fromStatus(WorkOrderStatus.OPEN)
                .toStatus(WorkOrderStatus.ASSIGNED)
                .changedBy(INITIAL_STATUS_ACTOR)
                .changeReason("Technician " + technicianId + " assigned")
                .build());

        WorkOrder saved = workOrderRepository.save(workOrder);
    recordAuditEventSafely(
        "TECHNICIAN_ASSIGNED",
        saved.getId(),
        INITIAL_STATUS_ACTOR,
        Map.of(
            "technicianId", technicianId,
            "fromStatus", WorkOrderStatus.OPEN.name(),
            "toStatus", WorkOrderStatus.ASSIGNED.name()
        )
    );
        log.info("Work order assigned: id={}, technicianId={}, country={}",
                saved.getId(), technicianId, saved.getCountryCode());
        return workOrderMapper.toResponse(saved);
    }

    @Transactional
    public WorkOrderResponse changeStatus(UUID id, ChangeWorkOrderStatusRequest request) {
        WorkOrder workOrder = workOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", id));

        WorkOrderStatus currentStatus = workOrder.getStatus();
        WorkOrderStatus targetStatus = request.targetStatus();

        validateStatusTransition(currentStatus, targetStatus);

        workOrder.setStatus(targetStatus);
        workOrder.addStatusHistory(WorkOrderStatusHistory.builder()
                .fromStatus(currentStatus)
                .toStatus(targetStatus)
                .changedBy(request.changedBy().trim())
                .changeReason(normalizeOptionalText(request.reason()))
                .build());

        WorkOrder saved = workOrderRepository.save(workOrder);
    recordAuditEventSafely(
        "WORK_ORDER_STATUS_CHANGED",
        saved.getId(),
        request.changedBy().trim(),
        buildStatusChangeDetails(currentStatus, targetStatus, request.reason())
    );
        log.info("Work order status changed: id={}, from={}, to={}",
                saved.getId(), currentStatus, targetStatus);
        return workOrderMapper.toResponse(saved);
    }

    @Transactional
    public WorkOrderResponse addComment(UUID id, AddWorkOrderCommentRequest request) {
        WorkOrder workOrder = workOrderRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("WorkOrder", id));

        workOrder.addComment(WorkOrderComment.builder()
                .authorName(request.authorName().trim())
                .body(request.body().trim())
                .build());

        WorkOrder saved = workOrderRepository.save(workOrder);
    recordAuditEventSafely(
        "WORK_ORDER_COMMENT_ADDED",
        saved.getId(),
        request.authorName().trim(),
        Map.of(
            "authorName", request.authorName().trim(),
            "body", request.body().trim()
        )
    );
        log.info("Work order comment added: id={}, comments={}",
                saved.getId(), saved.getComments().size());
        return workOrderMapper.toResponse(saved);
    }

    private void validatePageSize(Pageable pageable) {
        if (pageable.getPageSize() > MAX_PAGE_SIZE) {
            throw new BadRequestException("PAGE_SIZE_EXCEEDED",
                    "Maximum page size is " + MAX_PAGE_SIZE);
        }
    }

    private void validateSortProperties(Pageable pageable) {
        pageable.getSort().forEach(order -> {
            if (!ALLOWED_SORT_PROPERTIES.contains(order.getProperty())) {
                throw new BadRequestException("INVALID_SORT_PROPERTY",
                        "Unsupported sort property: " + order.getProperty()
                                + ". Allowed: " + ALLOWED_SORT_PROPERTIES);
            }
        });
    }

    private Specification<WorkOrder> buildFilterSpecification(
            String countryCode, WorkOrderPriority priority, WorkOrderStatus status) {

        List<Specification<WorkOrder>> filters = new ArrayList<>();

        if (countryCode != null && !countryCode.isBlank()) {
            filters.add(WorkOrderSpecification.hasCountryCode(countryCode.trim().toUpperCase()));
        }
        if (priority != null) {
            filters.add(WorkOrderSpecification.hasPriority(priority));
        }
        if (status != null) {
            filters.add(WorkOrderSpecification.hasStatus(status));
        }

        return filters.stream().reduce(Specification::and).orElse(null);
    }

    private void validateStatusTransition(WorkOrderStatus currentStatus, WorkOrderStatus targetStatus) {
        if (currentStatus == targetStatus) {
            throw new BusinessRuleException("WORK_ORDER_STATUS_UNCHANGED",
                    "Work order is already in status " + targetStatus);
        }

        // ASSIGNED is only reachable via the assignment endpoint — never via changeStatus()
        if (targetStatus == WorkOrderStatus.ASSIGNED) {
            throw new BusinessRuleException("WORK_ORDER_STATUS_TRANSITION_INVALID",
                    "Status ASSIGNED can only be set through the assignment endpoint");
        }

        boolean allowed = switch (currentStatus) {
            case OPEN -> targetStatus == WorkOrderStatus.COMPLETED
                      || targetStatus == WorkOrderStatus.CANCELLED;
            case ASSIGNED -> targetStatus == WorkOrderStatus.IN_PROGRESS
                          || targetStatus == WorkOrderStatus.CANCELLED;
            case IN_PROGRESS -> targetStatus == WorkOrderStatus.COMPLETED
                             || targetStatus == WorkOrderStatus.CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };

        if (!allowed) {
            throw new BusinessRuleException("WORK_ORDER_STATUS_TRANSITION_INVALID",
                    "Cannot transition work order from " + currentStatus + " to " + targetStatus);
        }
    }

    private String normalizeOptionalText(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private Map<String, Object> buildStatusChangeDetails(
            WorkOrderStatus currentStatus,
            WorkOrderStatus targetStatus,
            String reason) {

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("fromStatus", currentStatus.name());
        details.put("toStatus", targetStatus.name());

        String normalizedReason = normalizeOptionalText(reason);
        if (normalizedReason != null) {
            details.put("reason", normalizedReason);
        }

        return details;
    }

    private void recordAuditEventSafely(
            String eventType,
            UUID entityId,
            String actor,
            Object details) {

        try {
            auditClient.recordEvent(new AuditEventRequest(
                    eventType,
                    SOURCE_SERVICE,
                    ENTITY_TYPE,
                    entityId,
                    actor,
                    serializeDetails(details),
                    null
            ));
        } catch (ServiceCommunicationException ex) {
            log.warn("Audit event not recorded: type={}, entityId={}, reason={}",
                    eventType, entityId, ex.getMessage());
        }
    }

    private String serializeDetails(Object details) {
        if (details == null) {
            return null;
        }

        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize audit details: {}", ex.getMessage());
            return null;
        }
    }
}