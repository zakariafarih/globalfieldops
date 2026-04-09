package com.globalfieldops.workorder.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import com.globalfieldops.workorder.client.AuditClient;
import com.globalfieldops.workorder.client.TechnicianClient;
import com.globalfieldops.workorder.dto.request.AddWorkOrderCommentRequest;
import com.globalfieldops.workorder.dto.request.ChangeWorkOrderStatusRequest;
import com.globalfieldops.workorder.dto.request.CreateWorkOrderRequest;
import com.globalfieldops.workorder.dto.response.TechnicianValidationResponse;
import com.globalfieldops.workorder.dto.response.WorkOrderResponse;
import com.globalfieldops.workorder.entity.WorkOrder;
import com.globalfieldops.workorder.entity.WorkOrderComment;
import com.globalfieldops.workorder.entity.WorkOrderPriority;
import com.globalfieldops.workorder.entity.WorkOrderStatus;
import com.globalfieldops.workorder.entity.WorkOrderStatusHistory;
import com.globalfieldops.common.exception.BadRequestException;
import com.globalfieldops.common.exception.BusinessRuleException;
import com.globalfieldops.common.exception.ResourceNotFoundException;
import com.globalfieldops.workorder.exception.ServiceCommunicationException;
import com.globalfieldops.workorder.mapper.WorkOrderMapper;
import com.globalfieldops.workorder.repository.WorkOrderRepository;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkOrderServiceTest {

    @Mock
    private WorkOrderRepository workOrderRepository;

    @Mock
    private TechnicianClient technicianClient;

        @Mock
        private AuditClient auditClient;

    @Spy
    private WorkOrderMapper workOrderMapper;

        @Spy
        private ObjectMapper objectMapper = new ObjectMapper();

    private WorkOrderService workOrderService;

    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        workOrderService = new WorkOrderService(
                workOrderRepository, workOrderMapper, technicianClient,
                auditClient, objectMapper, meterRegistry);
    }

    private static WorkOrder buildWorkOrder() {
        Instant now = Instant.parse("2026-04-01T10:15:30Z");
        WorkOrder workOrder = WorkOrder.builder()
                .id(UUID.randomUUID())
                .title("Replace failed router")
                .summary("Core router in branch office is offline")
                .countryCode("US")
                .region("North East")
                .priority(WorkOrderPriority.HIGH)
                .status(WorkOrderStatus.OPEN)
                .createdAt(now)
                .updatedAt(now)
                .build();
        workOrder.addStatusHistory(WorkOrderStatusHistory.builder()
                .id(UUID.randomUUID())
                .fromStatus(null)
                .toStatus(WorkOrderStatus.OPEN)
                .changedBy("system")
                .changeReason("Work order created")
                .changedAt(now)
                .build());
        return workOrder;
    }

    private static TechnicianValidationResponse buildValidTechnician(String countryCode) {
        return new TechnicianValidationResponse(
                UUID.randomUUID(),
                countryCode,
                true,
                "AVAILABLE"
        );
    }

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("Should create work order with normalized fields and initial history")
        void shouldCreateWorkOrderWithNormalizedFieldsAndInitialHistory() {
            CreateWorkOrderRequest request = new CreateWorkOrderRequest(
                    " Replace failed router ",
                    " Core router in branch office is offline ",
                    "us",
                    "  North East  ",
                    WorkOrderPriority.HIGH
            );

            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> {
                WorkOrder entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                Instant now = Instant.parse("2026-04-01T10:15:30Z");
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                return entity;
            });

            WorkOrderResponse response = workOrderService.create(request);

            ArgumentCaptor<WorkOrder> captor = ArgumentCaptor.forClass(WorkOrder.class);
            verify(workOrderRepository).save(captor.capture());

            WorkOrder saved = captor.getValue();
            assertThat(saved.getTitle()).isEqualTo("Replace failed router");
            assertThat(saved.getSummary()).isEqualTo("Core router in branch office is offline");
            assertThat(saved.getCountryCode()).isEqualTo("US");
            assertThat(saved.getRegion()).isEqualTo("North East");
            assertThat(saved.getStatus()).isEqualTo(WorkOrderStatus.OPEN);
            assertThat(saved.getAssignedTechnicianId()).isNull();
            assertThat(saved.getStatusHistory()).hasSize(1);
            assertThat(saved.getStatusHistory().getFirst().getFromStatus()).isNull();
            assertThat(saved.getStatusHistory().getFirst().getToStatus()).isEqualTo(WorkOrderStatus.OPEN);
            assertThat(saved.getStatusHistory().getFirst().getChangedBy()).isEqualTo("system");

            assertThat(response.status()).isEqualTo("OPEN");
            assertThat(response.statusHistory()).hasSize(1);
                        verify(auditClient).recordEvent(any());
                }

                @Test
                @DisplayName("Should create work order even when audit-service is unavailable")
                void shouldCreateWorkOrderWhenAuditServiceUnavailable() {
                        CreateWorkOrderRequest request = new CreateWorkOrderRequest(
                                        "Replace failed router",
                                        "Core router in branch office is offline",
                                        "US",
                                        "North East",
                                        WorkOrderPriority.HIGH
                        );

                        when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> {
                                WorkOrder entity = invocation.getArgument(0);
                                entity.setId(UUID.randomUUID());
                                Instant now = Instant.parse("2026-04-01T10:15:30Z");
                                entity.setCreatedAt(now);
                                entity.setUpdatedAt(now);
                                return entity;
                        });
                        org.mockito.Mockito.doThrow(new ServiceCommunicationException(
                                        "audit-service", new RuntimeException("Connection refused")))
                                        .when(auditClient).recordEvent(any());

                        WorkOrderResponse response = workOrderService.create(request);

                        assertThat(response.status()).isEqualTo("OPEN");
                        verify(workOrderRepository).save(any(WorkOrder.class));
                        verify(auditClient).recordEvent(any());
        }
    }

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("Should throw ResourceNotFoundException for unknown work order")
        void shouldThrowWhenWorkOrderNotFound() {
            UUID unknownId = UUID.randomUUID();
            when(workOrderRepository.findByIdWithDetails(unknownId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workOrderService.findById(unknownId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("WorkOrder");
        }
    }

    @Nested
    @DisplayName("findAll() validation")
    class FindAllValidation {

        @Test
        @DisplayName("Should reject page size exceeding maximum")
        void shouldRejectOversizePageSize() {
            PageRequest oversized = PageRequest.of(0, 200, Sort.by("createdAt"));

            assertThatThrownBy(() -> workOrderService.findAll(null, null, null, oversized))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("Maximum page size");
        }

        @Test
        @DisplayName("Should reject unsupported sort property")
        void shouldRejectUnsupportedSortProperty() {
            PageRequest badSort = PageRequest.of(0, 20, Sort.by("comments"));

            assertThatThrownBy(() -> workOrderService.findAll(null, null, null, badSort))
                    .isInstanceOf(BadRequestException.class)
                    .hasMessageContaining("comments");
        }

        @Test
        @DisplayName("Should return mapped page for valid filters")
        void shouldReturnMappedPageForValidFilters() {
            WorkOrder workOrder = buildWorkOrder();
            PageRequest pageable = PageRequest.of(0, 20, Sort.by("createdAt").descending());

                when(workOrderRepository.findAll(org.mockito.ArgumentMatchers.<Specification<WorkOrder>>any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(workOrder), pageable, 1));

            var result = workOrderService.findAll("us", WorkOrderPriority.HIGH, WorkOrderStatus.OPEN, pageable);

            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().getFirst().countryCode()).isEqualTo("US");
            assertThat(result.getContent().getFirst().status()).isEqualTo("OPEN");
        }
    }

    @Nested
    @DisplayName("assignTechnician()")
    class AssignTechnician {

        @Test
        @DisplayName("Should assign technician and transition status to ASSIGNED")
        void shouldAssignTechnicianAndTransitionToAssigned() {
            WorkOrder workOrder = buildWorkOrder();
            UUID technicianId = UUID.randomUUID();

            when(workOrderRepository.findByIdWithDetails(workOrder.getId()))
                    .thenReturn(Optional.of(workOrder));
            when(technicianClient.getTechnician(technicianId))
                    .thenReturn(buildValidTechnician("US"));
            when(workOrderRepository.save(any(WorkOrder.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            WorkOrderResponse response = workOrderService.assignTechnician(workOrder.getId(), technicianId);

            assertThat(workOrder.getAssignedTechnicianId()).isEqualTo(technicianId);
            assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.ASSIGNED);
            assertThat(workOrder.getStatusHistory()).hasSize(2);
            assertThat(workOrder.getStatusHistory().getLast().getFromStatus()).isEqualTo(WorkOrderStatus.OPEN);
            assertThat(workOrder.getStatusHistory().getLast().getToStatus()).isEqualTo(WorkOrderStatus.ASSIGNED);
            assertThat(response.status()).isEqualTo("ASSIGNED");
            assertThat(response.assignedTechnicianId()).isEqualTo(technicianId);
            verify(workOrderRepository).save(workOrder);
            verify(auditClient).recordEvent(any());
        }

        @Test
        @DisplayName("Should assign technician even when audit-service is unavailable")
        void shouldAssignTechnicianWhenAuditServiceUnavailable() {
            WorkOrder workOrder = buildWorkOrder();
            UUID technicianId = UUID.randomUUID();

            when(workOrderRepository.findByIdWithDetails(workOrder.getId()))
                    .thenReturn(Optional.of(workOrder));
            when(technicianClient.getTechnician(technicianId))
                    .thenReturn(buildValidTechnician("US"));
            when(workOrderRepository.save(any(WorkOrder.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));
            org.mockito.Mockito.doThrow(new ServiceCommunicationException(
                    "audit-service", new RuntimeException("Connection refused")))
                    .when(auditClient).recordEvent(any());

            WorkOrderResponse response = workOrderService.assignTechnician(workOrder.getId(), technicianId);

            assertThat(response.status()).isEqualTo("ASSIGNED");
            assertThat(workOrder.getAssignedTechnicianId()).isEqualTo(technicianId);
            verify(workOrderRepository).save(workOrder);
            verify(auditClient).recordEvent(any());
        }

        @Test
        @DisplayName("Should reject assignment when work order not found")
        void shouldRejectAssignmentWhenWorkOrderNotFound() {
            UUID workOrderId = UUID.randomUUID();
            UUID technicianId = UUID.randomUUID();

            when(workOrderRepository.findByIdWithDetails(workOrderId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> workOrderService.assignTechnician(workOrderId, technicianId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("WorkOrder");

            verify(technicianClient, never()).getTechnician(any());
            verify(workOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject assignment when work order is not OPEN")
        void shouldRejectAssignmentWhenWorkOrderNotOpen() {
            WorkOrder workOrder = buildWorkOrder();
            workOrder.setStatus(WorkOrderStatus.COMPLETED);
            UUID technicianId = UUID.randomUUID();

            when(workOrderRepository.findByIdWithDetails(workOrder.getId()))
                    .thenReturn(Optional.of(workOrder));

            assertThatThrownBy(() -> workOrderService.assignTechnician(workOrder.getId(), technicianId))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting("code")
                    .isEqualTo("WORK_ORDER_NOT_ASSIGNABLE");

            verify(technicianClient, never()).getTechnician(any());
            verify(workOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should propagate ResourceNotFoundException when technician not found")
        void shouldRejectAssignmentWhenTechnicianNotFound() {
            WorkOrder workOrder = buildWorkOrder();
            UUID technicianId = UUID.randomUUID();

            when(workOrderRepository.findByIdWithDetails(workOrder.getId()))
                    .thenReturn(Optional.of(workOrder));
            when(technicianClient.getTechnician(technicianId))
                    .thenThrow(new ResourceNotFoundException("Technician", technicianId));

            assertThatThrownBy(() -> workOrderService.assignTechnician(workOrder.getId(), technicianId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Technician");

            verify(workOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject assignment when technician is inactive")
        void shouldRejectAssignmentWhenTechnicianInactive() {
            WorkOrder workOrder = buildWorkOrder();
            UUID technicianId = UUID.randomUUID();
            TechnicianValidationResponse inactiveTech = new TechnicianValidationResponse(
                    technicianId, "US", false, "AVAILABLE");

            when(workOrderRepository.findByIdWithDetails(workOrder.getId()))
                    .thenReturn(Optional.of(workOrder));
            when(technicianClient.getTechnician(technicianId))
                    .thenReturn(inactiveTech);

            assertThatThrownBy(() -> workOrderService.assignTechnician(workOrder.getId(), technicianId))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting("code")
                    .isEqualTo("TECHNICIAN_NOT_ACTIVE");

            verify(workOrderRepository, never()).save(any());
            verify(auditClient, never()).recordEvent(any());
        }

        @Test
        @DisplayName("Should reject assignment when technician is unavailable")
        void shouldRejectAssignmentWhenTechnicianUnavailable() {
            WorkOrder workOrder = buildWorkOrder();
            UUID technicianId = UUID.randomUUID();
            TechnicianValidationResponse unavailableTech = new TechnicianValidationResponse(
                    technicianId, "US", true, "ON_SITE");

            when(workOrderRepository.findByIdWithDetails(workOrder.getId()))
                    .thenReturn(Optional.of(workOrder));
            when(technicianClient.getTechnician(technicianId))
                    .thenReturn(unavailableTech);

            assertThatThrownBy(() -> workOrderService.assignTechnician(workOrder.getId(), technicianId))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting("code")
                    .isEqualTo("TECHNICIAN_NOT_AVAILABLE");

            verify(workOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject assignment when country codes do not match")
        void shouldRejectAssignmentWhenCountryMismatch() {
            WorkOrder workOrder = buildWorkOrder();
            UUID technicianId = UUID.randomUUID();

            when(workOrderRepository.findByIdWithDetails(workOrder.getId()))
                    .thenReturn(Optional.of(workOrder));
            when(technicianClient.getTechnician(technicianId))
                    .thenReturn(buildValidTechnician("DE"));

            assertThatThrownBy(() -> workOrderService.assignTechnician(workOrder.getId(), technicianId))
                    .isInstanceOf(BusinessRuleException.class)
                    .extracting("code")
                    .isEqualTo("COUNTRY_MISMATCH");

            verify(workOrderRepository, never()).save(any());
            verify(auditClient, never()).recordEvent(any());
        }

        @Test
        @DisplayName("Should propagate ServiceCommunicationException when technician-service is down")
        void shouldPropagateServiceCommunicationException() {
            WorkOrder workOrder = buildWorkOrder();
            UUID technicianId = UUID.randomUUID();

            when(workOrderRepository.findByIdWithDetails(workOrder.getId()))
                    .thenReturn(Optional.of(workOrder));
            when(technicianClient.getTechnician(technicianId))
                    .thenThrow(new ServiceCommunicationException("technician-service",
                            new RuntimeException("Connection refused")));

            assertThatThrownBy(() -> workOrderService.assignTechnician(workOrder.getId(), technicianId))
                    .isInstanceOf(ServiceCommunicationException.class);

            verify(workOrderRepository, never()).save(any());
                        verify(auditClient, never()).recordEvent(any());
        }
    }

    @Nested
    @DisplayName("changeStatus()")
    class ChangeStatus {

        @Test
        @DisplayName("Should append history when transitioning from OPEN to COMPLETED")
        void shouldAppendHistoryForValidTransition() {
            WorkOrder workOrder = buildWorkOrder();

            when(workOrderRepository.findByIdWithDetails(workOrder.getId())).thenReturn(Optional.of(workOrder));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

            WorkOrderResponse response = workOrderService.changeStatus(
                    workOrder.getId(),
                    new ChangeWorkOrderStatusRequest(WorkOrderStatus.COMPLETED, " dispatcher ", "  job finished  ")
            );

            assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.COMPLETED);
            assertThat(workOrder.getStatusHistory()).hasSize(2);
            assertThat(workOrder.getStatusHistory().getLast().getFromStatus()).isEqualTo(WorkOrderStatus.OPEN);
            assertThat(workOrder.getStatusHistory().getLast().getToStatus()).isEqualTo(WorkOrderStatus.COMPLETED);
            assertThat(workOrder.getStatusHistory().getLast().getChangedBy()).isEqualTo("dispatcher");
            assertThat(workOrder.getStatusHistory().getLast().getChangeReason()).isEqualTo("job finished");
            assertThat(response.status()).isEqualTo("COMPLETED");
                        verify(auditClient).recordEvent(any());
        }

        @Test
        @DisplayName("Should reject ASSIGNED via changeStatus — only reachable through assignment endpoint")
        void shouldRejectAssignedViaStatusChange() {
            WorkOrder workOrder = buildWorkOrder();
            when(workOrderRepository.findByIdWithDetails(workOrder.getId())).thenReturn(Optional.of(workOrder));

            assertThatThrownBy(() -> workOrderService.changeStatus(
                    workOrder.getId(),
                    new ChangeWorkOrderStatusRequest(WorkOrderStatus.ASSIGNED, "dispatcher", null)
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("assignment endpoint");

            verify(workOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject transition from terminal status")
        void shouldRejectTransitionFromTerminalStatus() {
            WorkOrder workOrder = buildWorkOrder();
            workOrder.setStatus(WorkOrderStatus.CANCELLED);

            when(workOrderRepository.findByIdWithDetails(workOrder.getId())).thenReturn(Optional.of(workOrder));

            assertThatThrownBy(() -> workOrderService.changeStatus(
                    workOrder.getId(),
                    new ChangeWorkOrderStatusRequest(WorkOrderStatus.COMPLETED, "dispatcher", null)
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot transition");

            verify(workOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should reject unchanged status")
        void shouldRejectUnchangedStatus() {
            WorkOrder workOrder = buildWorkOrder();
            when(workOrderRepository.findByIdWithDetails(workOrder.getId())).thenReturn(Optional.of(workOrder));

            assertThatThrownBy(() -> workOrderService.changeStatus(
                    workOrder.getId(),
                    new ChangeWorkOrderStatusRequest(WorkOrderStatus.OPEN, "dispatcher", null)
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already in status");

            verify(workOrderRepository, never()).save(any());
        }

        @Test
        @DisplayName("Should allow ASSIGNED to IN_PROGRESS")
        void shouldAllowAssignedToInProgress() {
            WorkOrder workOrder = buildWorkOrder();
            workOrder.setStatus(WorkOrderStatus.ASSIGNED);

            when(workOrderRepository.findByIdWithDetails(workOrder.getId())).thenReturn(Optional.of(workOrder));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

            WorkOrderResponse response = workOrderService.changeStatus(
                    workOrder.getId(),
                    new ChangeWorkOrderStatusRequest(WorkOrderStatus.IN_PROGRESS, "dispatcher", "work started")
            );

            assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
            assertThat(workOrder.getStatusHistory()).hasSize(2);
            assertThat(workOrder.getStatusHistory().getLast().getFromStatus()).isEqualTo(WorkOrderStatus.ASSIGNED);
            assertThat(workOrder.getStatusHistory().getLast().getToStatus()).isEqualTo(WorkOrderStatus.IN_PROGRESS);
            assertThat(response.status()).isEqualTo("IN_PROGRESS");
                        verify(auditClient).recordEvent(any());
        }

        @Test
        @DisplayName("Should allow ASSIGNED to CANCELLED")
        void shouldAllowAssignedToCancelled() {
            WorkOrder workOrder = buildWorkOrder();
            workOrder.setStatus(WorkOrderStatus.ASSIGNED);

            when(workOrderRepository.findByIdWithDetails(workOrder.getId())).thenReturn(Optional.of(workOrder));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

            WorkOrderResponse response = workOrderService.changeStatus(
                    workOrder.getId(),
                    new ChangeWorkOrderStatusRequest(WorkOrderStatus.CANCELLED, "dispatcher", "no longer needed")
            );

            assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.CANCELLED);
            assertThat(response.status()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("Should allow IN_PROGRESS to COMPLETED")
        void shouldAllowInProgressToCompleted() {
            WorkOrder workOrder = buildWorkOrder();
            workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);

            when(workOrderRepository.findByIdWithDetails(workOrder.getId())).thenReturn(Optional.of(workOrder));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

            WorkOrderResponse response = workOrderService.changeStatus(
                    workOrder.getId(),
                    new ChangeWorkOrderStatusRequest(WorkOrderStatus.COMPLETED, "dispatcher", "job finished")
            );

            assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.COMPLETED);
            assertThat(response.status()).isEqualTo("COMPLETED");
        }

        @Test
        @DisplayName("Should allow IN_PROGRESS to CANCELLED")
        void shouldAllowInProgressToCancelled() {
            WorkOrder workOrder = buildWorkOrder();
            workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);

            when(workOrderRepository.findByIdWithDetails(workOrder.getId())).thenReturn(Optional.of(workOrder));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

            WorkOrderResponse response = workOrderService.changeStatus(
                    workOrder.getId(),
                    new ChangeWorkOrderStatusRequest(WorkOrderStatus.CANCELLED, "dispatcher", "aborted")
            );

            assertThat(workOrder.getStatus()).isEqualTo(WorkOrderStatus.CANCELLED);
            assertThat(response.status()).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("Should reject IN_PROGRESS to OPEN — backward transition not allowed")
        void shouldRejectInProgressToOpen() {
            WorkOrder workOrder = buildWorkOrder();
            workOrder.setStatus(WorkOrderStatus.IN_PROGRESS);

            when(workOrderRepository.findByIdWithDetails(workOrder.getId())).thenReturn(Optional.of(workOrder));

            assertThatThrownBy(() -> workOrderService.changeStatus(
                    workOrder.getId(),
                    new ChangeWorkOrderStatusRequest(WorkOrderStatus.OPEN, "dispatcher", null)
            ))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot transition");

            verify(workOrderRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("addComment()")
    class AddComment {

        @Test
        @DisplayName("Should append trimmed comment to work order")
        void shouldAppendTrimmedCommentToWorkOrder() {
            WorkOrder workOrder = buildWorkOrder();

            when(workOrderRepository.findByIdWithDetails(workOrder.getId())).thenReturn(Optional.of(workOrder));
            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

            WorkOrderResponse response = workOrderService.addComment(
                    workOrder.getId(),
                    new AddWorkOrderCommentRequest(" Dispatcher ", "  Technician dispatched to site  ")
            );

            assertThat(workOrder.getComments()).hasSize(1);
            WorkOrderComment comment = workOrder.getComments().getFirst();
            assertThat(comment.getAuthorName()).isEqualTo("Dispatcher");
            assertThat(comment.getBody()).isEqualTo("Technician dispatched to site");
            assertThat(response.comments()).hasSize(1);
                        verify(auditClient).recordEvent(any());
        }
    }

    @Nested
    @DisplayName("Custom Metrics")
    class Metrics {

        @Test
        @DisplayName("Should increment workorders.created counter on successful create")
        void shouldIncrementCreatedCounterOnCreate() {
            CreateWorkOrderRequest request = new CreateWorkOrderRequest(
                    "Install network switch", "New office floor 3", "US", null, WorkOrderPriority.MEDIUM);

            when(workOrderRepository.save(any(WorkOrder.class))).thenAnswer(invocation -> {
                WorkOrder entity = invocation.getArgument(0);
                entity.setId(UUID.randomUUID());
                entity.setCreatedAt(Instant.now());
                entity.setUpdatedAt(Instant.now());
                return entity;
            });

            workOrderService.create(request);

            assertThat(meterRegistry.counter("workorders.created").count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should increment assignments.attempted on every assignment call")
        void shouldIncrementAttemptedCounterOnAssignment() {
            WorkOrder workOrder = buildWorkOrder();
            UUID technicianId = UUID.randomUUID();

            when(workOrderRepository.findByIdWithDetails(workOrder.getId()))
                    .thenReturn(Optional.of(workOrder));
            when(technicianClient.getTechnician(technicianId))
                    .thenReturn(buildValidTechnician("US"));
            when(workOrderRepository.save(any(WorkOrder.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            workOrderService.assignTechnician(workOrder.getId(), technicianId);

            assertThat(meterRegistry.counter("assignments.attempted").count()).isEqualTo(1.0);
        }

        @Test
        @DisplayName("Should increment assignments.failed when technician is inactive")
        void shouldIncrementFailedCounterWhenTechnicianInactive() {
            WorkOrder workOrder = buildWorkOrder();
            UUID technicianId = UUID.randomUUID();
            TechnicianValidationResponse inactiveTech = new TechnicianValidationResponse(
                    technicianId, "US", false, "AVAILABLE");

            when(workOrderRepository.findByIdWithDetails(workOrder.getId()))
                    .thenReturn(Optional.of(workOrder));
            when(technicianClient.getTechnician(technicianId))
                    .thenReturn(inactiveTech);

            try {
                workOrderService.assignTechnician(workOrder.getId(), technicianId);
            } catch (BusinessRuleException ignored) {
            }

            assertThat(meterRegistry.counter("assignments.attempted").count()).isEqualTo(1.0);
            assertThat(meterRegistry.counter("assignments.failed").count()).isEqualTo(1.0);
        }
    }
}