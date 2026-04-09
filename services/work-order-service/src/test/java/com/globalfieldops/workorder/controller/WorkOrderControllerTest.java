package com.globalfieldops.workorder.controller;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.globalfieldops.workorder.dto.response.WorkOrderCommentResponse;
import com.globalfieldops.workorder.dto.response.WorkOrderResponse;
import com.globalfieldops.workorder.dto.response.WorkOrderStatusHistoryResponse;
import com.globalfieldops.workorder.dto.response.WorkOrderSummaryResponse;
import com.globalfieldops.workorder.entity.WorkOrderPriority;
import com.globalfieldops.workorder.entity.WorkOrderStatus;
import com.globalfieldops.common.exception.BadRequestException;
import com.globalfieldops.common.exception.BusinessRuleException;
import com.globalfieldops.common.exception.ResourceNotFoundException;
import com.globalfieldops.workorder.exception.ServiceCommunicationException;
import com.globalfieldops.workorder.security.SecurityConfig;
import com.globalfieldops.workorder.service.WorkOrderService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkOrderController.class)
@Import(SecurityConfig.class)
class WorkOrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkOrderService workOrderService;

    private static final UUID WORK_ORDER_ID = UUID.randomUUID();
    private static final UUID TECHNICIAN_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-04-01T11:10:00Z");

    private static WorkOrderResponse sampleResponse() {
        return new WorkOrderResponse(
                WORK_ORDER_ID,
                "Replace failed router",
                "Core router in branch office is offline",
                "US",
                "North East",
                "HIGH",
                "OPEN",
                null,
                List.of(new WorkOrderCommentResponse(UUID.randomUUID(), "Dispatcher", "Assigned for review", NOW)),
                List.of(new WorkOrderStatusHistoryResponse(UUID.randomUUID(), null, "OPEN", "system", "Work order created", NOW)),
                NOW,
                NOW
        );
    }

    private static WorkOrderSummaryResponse sampleSummary() {
        return new WorkOrderSummaryResponse(
                WORK_ORDER_ID,
                "Replace failed router",
                "US",
                "North East",
                "HIGH",
                "OPEN",
                null,
                NOW,
                NOW
        );
    }

    private static WorkOrderResponse assignedResponse() {
        return new WorkOrderResponse(
                WORK_ORDER_ID,
                "Replace failed router",
                "Core router in branch office is offline",
                "US",
                "North East",
                "HIGH",
                "ASSIGNED",
                TECHNICIAN_ID,
                List.of(),
                List.of(
                        new WorkOrderStatusHistoryResponse(UUID.randomUUID(), null, "OPEN", "system", "Work order created", NOW),
                        new WorkOrderStatusHistoryResponse(UUID.randomUUID(), "OPEN", "ASSIGNED", "system", "Technician assigned", NOW)
                ),
                NOW,
                NOW
        );
    }

    @Nested
    @DisplayName("POST /api/work-orders")
    class CreateEndpoint {

        @Test
        @DisplayName("Should return 201 with valid request")
        void shouldReturn201() throws Exception {
            when(workOrderService.create(any())).thenReturn(sampleResponse());

            mockMvc.perform(post("/api/work-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": "Replace failed router",
                                      "summary": "Core router in branch office is offline",
                                      "countryCode": "US",
                                      "region": "North East",
                                      "priority": "HIGH"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(WORK_ORDER_ID.toString()))
                    .andExpect(jsonPath("$.status").value("OPEN"));
        }

        @Test
        @DisplayName("Should return 400 with missing required fields")
        void shouldReturn400ForMissingFields() throws Exception {
            mockMvc.perform(post("/api/work-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.details.title").exists())
                    .andExpect(jsonPath("$.details.summary").exists())
                    .andExpect(jsonPath("$.details.countryCode").exists())
                    .andExpect(jsonPath("$.details.priority").exists());
        }

        @Test
        @DisplayName("Should return 400 for malformed JSON body")
        void shouldReturn400ForMalformedJson() throws Exception {
            mockMvc.perform(post("/api/work-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                    .andExpect(jsonPath("$.details.body").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/work-orders/{id}")
    class FindByIdEndpoint {

        @Test
        @DisplayName("Should return 200 with valid ID")
        void shouldReturn200() throws Exception {
            when(workOrderService.findById(WORK_ORDER_ID)).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/work-orders/{id}", WORK_ORDER_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(WORK_ORDER_ID.toString()))
                    .andExpect(jsonPath("$.title").value("Replace failed router"));
        }

        @Test
        @DisplayName("Should return 404 for unknown ID")
        void shouldReturn404() throws Exception {
            UUID unknownId = UUID.randomUUID();
            when(workOrderService.findById(unknownId))
                    .thenThrow(new ResourceNotFoundException("WorkOrder", unknownId));

            mockMvc.perform(get("/api/work-orders/{id}", unknownId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 400 for invalid UUID format")
        void shouldReturn400ForInvalidUuid() throws Exception {
            mockMvc.perform(get("/api/work-orders/{id}", "not-a-uuid"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"))
                    .andExpect(jsonPath("$.details.id").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/work-orders")
    class FindAllEndpoint {

        @Test
        @DisplayName("Should return 200 with paginated results")
        void shouldReturn200WithPage() throws Exception {
            PageImpl<WorkOrderSummaryResponse> page = new PageImpl<>(
                    List.of(sampleSummary()),
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt")),
                    1
            );

            when(workOrderService.findAll(any(), any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/api/work-orders"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].title").value("Replace failed router"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("Should return 400 for invalid priority query parameter")
        void shouldReturn400ForInvalidPriorityQueryParameter() throws Exception {
            mockMvc.perform(get("/api/work-orders").param("priority", "NOT_REAL"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST_PARAMETER"))
                    .andExpect(jsonPath("$.details.priority").exists());
        }

        @Test
        @DisplayName("Should return 400 for invalid sort property")
        void shouldReturn400ForBadSort() throws Exception {
            when(workOrderService.findAll(any(), any(), any(), any()))
                    .thenThrow(new BadRequestException("INVALID_SORT_PROPERTY", "Unsupported sort property: comments"));

            mockMvc.perform(get("/api/work-orders").param("sort", "comments"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_SORT_PROPERTY"));
        }
    }

    @Nested
    @DisplayName("POST /api/work-orders/{id}/assign/{technicianId}")
    class AssignTechnicianEndpoint {

        @Test
        @DisplayName("Should return 200 with assigned work order")
        void shouldReturn200WithAssignedWorkOrder() throws Exception {
            when(workOrderService.assignTechnician(WORK_ORDER_ID, TECHNICIAN_ID))
                    .thenReturn(assignedResponse());

            mockMvc.perform(post("/api/work-orders/{id}/assign/{technicianId}",
                            WORK_ORDER_ID, TECHNICIAN_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ASSIGNED"))
                    .andExpect(jsonPath("$.assignedTechnicianId").value(TECHNICIAN_ID.toString()))
                    .andExpect(jsonPath("$.statusHistory.length()").value(2));
        }

        @Test
        @DisplayName("Should return 404 when work order not found")
        void shouldReturn404WhenWorkOrderNotFound() throws Exception {
            when(workOrderService.assignTechnician(eq(WORK_ORDER_ID), any()))
                    .thenThrow(new ResourceNotFoundException("WorkOrder", WORK_ORDER_ID));

            mockMvc.perform(post("/api/work-orders/{id}/assign/{technicianId}",
                            WORK_ORDER_ID, TECHNICIAN_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
        }

        @Test
        @DisplayName("Should return 409 when work order is not assignable")
        void shouldReturn409WhenWorkOrderNotAssignable() throws Exception {
            when(workOrderService.assignTechnician(eq(WORK_ORDER_ID), any()))
                    .thenThrow(new BusinessRuleException(
                            "WORK_ORDER_NOT_ASSIGNABLE",
                            "Only work orders in OPEN status can be assigned"));

            mockMvc.perform(post("/api/work-orders/{id}/assign/{technicianId}",
                            WORK_ORDER_ID, TECHNICIAN_ID))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("WORK_ORDER_NOT_ASSIGNABLE"));
        }

        @Test
        @DisplayName("Should return 409 when technician validation fails")
        void shouldReturn409WhenTechnicianValidationFails() throws Exception {
            when(workOrderService.assignTechnician(eq(WORK_ORDER_ID), any()))
                    .thenThrow(new BusinessRuleException(
                            "TECHNICIAN_NOT_ACTIVE",
                            "Technician is not active"));

            mockMvc.perform(post("/api/work-orders/{id}/assign/{technicianId}",
                            WORK_ORDER_ID, TECHNICIAN_ID))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("TECHNICIAN_NOT_ACTIVE"));
        }

        @Test
        @DisplayName("Should return 502 when technician service is unavailable")
        void shouldReturn502WhenTechnicianServiceUnavailable() throws Exception {
            when(workOrderService.assignTechnician(eq(WORK_ORDER_ID), any()))
                    .thenThrow(new ServiceCommunicationException("technician-service",
                            new RuntimeException("Connection refused")));

            mockMvc.perform(post("/api/work-orders/{id}/assign/{technicianId}",
                            WORK_ORDER_ID, TECHNICIAN_ID))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                    .andExpect(jsonPath("$.details.service").value("technician-service"));
        }
    }

    @Nested
    @DisplayName("POST /api/work-orders/{id}/status")
    class ChangeStatusEndpoint {

        @Test
        @DisplayName("Should return 200 with valid status change")
        void shouldReturn200() throws Exception {
            WorkOrderResponse completed = new WorkOrderResponse(
                    WORK_ORDER_ID,
                    "Replace failed router",
                    "Core router in branch office is offline",
                    "US",
                    "North East",
                    "HIGH",
                    "COMPLETED",
                    null,
                    List.of(),
                    List.of(),
                    NOW,
                    NOW
            );

            when(workOrderService.changeStatus(eq(WORK_ORDER_ID), any())).thenReturn(completed);

            mockMvc.perform(post("/api/work-orders/{id}/status", WORK_ORDER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "targetStatus": "COMPLETED",
                                      "changedBy": "dispatcher",
                                      "reason": "job finished"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"));
        }

        @Test
        @DisplayName("Should return 409 for ASSIGNED via status change")
        void shouldReturn409ForAssignedViaStatusChange() throws Exception {
            when(workOrderService.changeStatus(eq(WORK_ORDER_ID), any()))
                    .thenThrow(new BusinessRuleException(
                            "WORK_ORDER_STATUS_TRANSITION_INVALID",
                            "Status ASSIGNED can only be set through the assignment endpoint"
                    ));

            mockMvc.perform(post("/api/work-orders/{id}/status", WORK_ORDER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "targetStatus": "ASSIGNED",
                                      "changedBy": "dispatcher"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("WORK_ORDER_STATUS_TRANSITION_INVALID"));
        }

        @Test
        @DisplayName("Should return 400 for invalid status enum in body")
        void shouldReturn400ForInvalidStatusEnum() throws Exception {
            mockMvc.perform(post("/api/work-orders/{id}/status", WORK_ORDER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "targetStatus": "NOT_REAL",
                                      "changedBy": "dispatcher"
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_REQUEST_BODY"))
                    .andExpect(jsonPath("$.details.body").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/work-orders/{id}/comments")
    class AddCommentEndpoint {

        @Test
        @DisplayName("Should return 201 with valid comment")
        void shouldReturn201() throws Exception {
            when(workOrderService.addComment(eq(WORK_ORDER_ID), any())).thenReturn(sampleResponse());

            mockMvc.perform(post("/api/work-orders/{id}/comments", WORK_ORDER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "authorName": "Dispatcher",
                                      "body": "Technician dispatched to site"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.comments[0].authorName").value("Dispatcher"));
        }

        @Test
        @DisplayName("Should return 400 with blank comment body")
        void shouldReturn400ForBlankCommentBody() throws Exception {
            mockMvc.perform(post("/api/work-orders/{id}/comments", WORK_ORDER_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "authorName": "Dispatcher",
                                      "body": ""
                                    }
                                    """))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.details.body").exists());
        }
    }
}