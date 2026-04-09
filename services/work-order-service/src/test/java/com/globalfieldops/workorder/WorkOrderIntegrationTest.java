package com.globalfieldops.workorder;

import java.util.UUID;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.globalfieldops.workorder.entity.WorkOrder;
import com.globalfieldops.workorder.entity.WorkOrderStatus;
import com.globalfieldops.workorder.repository.WorkOrderRepository;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfiguration.class)
class WorkOrderIntegrationTest {

    private static WireMockServer wireMockServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkOrderRepository workOrderRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startWireMock() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("services.technician-service.url", wireMockServer::baseUrl);
        registry.add("services.audit-service.url", wireMockServer::baseUrl);
    }

    @BeforeEach
    void setUp() {
        workOrderRepository.deleteAllInBatch();
        wireMockServer.resetAll();
        stubAuditSuccess();
    }

    @Nested
    @DisplayName("Create flow")
    class CreateFlow {

        @Test
        @DisplayName("Should persist OPEN work order with initial status history")
        void shouldPersistOpenWorkOrderWithInitialHistory() throws Exception {
            String response = mockMvc.perform(post("/api/work-orders")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "title": " Replace failed router ",
                                      "summary": " Core router in branch office is offline ",
                                      "countryCode": "us",
                                      "region": "  North East ",
                                      "priority": "HIGH"
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Replace failed router"))
                    .andExpect(jsonPath("$.countryCode").value("US"))
                    .andExpect(jsonPath("$.status").value("OPEN"))
                    .andExpect(jsonPath("$.statusHistory.length()").value(1))
                    .andExpect(jsonPath("$.statusHistory[0].fromStatus").doesNotExist())
                    .andExpect(jsonPath("$.statusHistory[0].toStatus").value("OPEN"))
                    .andReturn().getResponse().getContentAsString();

            UUID id = UUID.fromString(objectMapper.readTree(response).get("id").asText());
            WorkOrder persisted = workOrderRepository.findByIdWithDetails(id).orElseThrow();

            assertThat(persisted.getStatus()).isEqualTo(WorkOrderStatus.OPEN);
            assertThat(persisted.getCountryCode()).isEqualTo("US");
            assertThat(persisted.getRegion()).isEqualTo("North East");
            assertThat(persisted.getStatusHistory()).hasSize(1);
            assertThat(persisted.getStatusHistory().getFirst().getFromStatus()).isNull();
            assertThat(persisted.getStatusHistory().getFirst().getToStatus()).isEqualTo(WorkOrderStatus.OPEN);
                        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo("/api/audit-events")));
                }

                @Test
                @DisplayName("Should create work order even when audit-service returns 503")
                void shouldCreateWorkOrderWhenAuditServiceUnavailable() throws Exception {
                        wireMockServer.resetAll();
                            wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathEqualTo("/api/audit-events"))
                                        .willReturn(aResponse().withStatus(503).withBody("Audit unavailable")));

                        mockMvc.perform(post("/api/work-orders")
                                                        .contentType(MediaType.APPLICATION_JSON)
                                                        .content("""
                                                                        {
                                                                            "title": "Replace failed router",
                                                                            "summary": "Core router in branch office is offline",
                                                                            "countryCode": "US",
                                                                            "priority": "HIGH"
                                                                        }
                                                                        """))
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.status").value("OPEN"));

                        assertThat(workOrderRepository.count()).isEqualTo(1);
                        wireMockServer.verify(1, postRequestedFor(urlPathEqualTo("/api/audit-events")));
        }
    }

    @Nested
    @DisplayName("Comment flow")
    class CommentFlow {

        @Test
        @DisplayName("Should persist comment row and return it in response")
        void shouldPersistCommentRowAndReturnIt() throws Exception {
            UUID id = createWorkOrder("Comment order", "Comment flow test", "US", "HIGH");
            wireMockServer.resetRequests();

            mockMvc.perform(post("/api/work-orders/{id}/comments", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "authorName": " Dispatcher ",
                                      "body": "  Technician dispatched to site  "
                                    }
                                    """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.comments.length()").value(1))
                    .andExpect(jsonPath("$.comments[0].authorName").value("Dispatcher"))
                    .andExpect(jsonPath("$.comments[0].body").value("Technician dispatched to site"));

            WorkOrder persisted = workOrderRepository.findByIdWithDetails(id).orElseThrow();
            assertThat(persisted.getComments()).hasSize(1);
            assertThat(persisted.getComments().getFirst().getAuthorName()).isEqualTo("Dispatcher");
            assertThat(persisted.getComments().getFirst().getBody()).isEqualTo("Technician dispatched to site");
            wireMockServer.verify(1, postRequestedFor(urlPathEqualTo("/api/audit-events")));
        }
    }

    @Nested
    @DisplayName("Status transition flow")
    class StatusTransitionFlow {

        @Test
        @DisplayName("Should persist COMPLETED status and appended history for valid transition")
        void shouldPersistCompletedStatusAndHistory() throws Exception {
            UUID id = createWorkOrder("Transition order", "Transition flow test", "GB", "MEDIUM");
            wireMockServer.resetRequests();

            mockMvc.perform(post("/api/work-orders/{id}/status", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "targetStatus": "COMPLETED",
                                      "changedBy": "dispatcher",
                                      "reason": "job finished"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("COMPLETED"))
                    .andExpect(jsonPath("$.statusHistory.length()").value(2))
                    .andExpect(jsonPath("$.statusHistory[1].fromStatus").value("OPEN"))
                    .andExpect(jsonPath("$.statusHistory[1].toStatus").value("COMPLETED"));

            WorkOrder persisted = workOrderRepository.findByIdWithDetails(id).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(WorkOrderStatus.COMPLETED);
            assertThat(persisted.getStatusHistory()).hasSize(2);
            assertThat(persisted.getStatusHistory().getLast().getFromStatus()).isEqualTo(WorkOrderStatus.OPEN);
            assertThat(persisted.getStatusHistory().getLast().getToStatus()).isEqualTo(WorkOrderStatus.COMPLETED);
            wireMockServer.verify(1, postRequestedFor(urlPathEqualTo("/api/audit-events")));
        }

        @Test
        @DisplayName("Should return 409 for invalid terminal transition and keep persisted state unchanged")
        void shouldReturn409AndNotMutateStateForInvalidTerminalTransition() throws Exception {
            UUID id = createWorkOrder("Terminal order", "Terminal flow test", "FR", "LOW");

            mockMvc.perform(post("/api/work-orders/{id}/status", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "targetStatus": "COMPLETED",
                                      "changedBy": "dispatcher"
                                    }
                                    """))
                    .andExpect(status().isOk());

            mockMvc.perform(post("/api/work-orders/{id}/status", id)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "targetStatus": "CANCELLED",
                                      "changedBy": "dispatcher"
                                    }
                                    """))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("WORK_ORDER_STATUS_TRANSITION_INVALID"));

            WorkOrder persisted = workOrderRepository.findByIdWithDetails(id).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(WorkOrderStatus.COMPLETED);
            assertThat(persisted.getStatusHistory()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("List flow")
    class ListFlow {

        @Test
        @DisplayName("Should return filtered results from real persisted data")
        void shouldReturnFilteredResultsFromPersistedData() throws Exception {
            createWorkOrder("US high open", "Filter include", "US", "HIGH");
            createWorkOrder("DE low open", "Filter exclude", "DE", "LOW");

            mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/work-orders")
                            .param("countryCode", "US")
                            .param("priority", "HIGH")
                            .param("status", "OPEN"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.content[0].title").value("US high open"))
                    .andExpect(jsonPath("$.content[0].countryCode").value("US"))
                    .andExpect(jsonPath("$.content[0].priority").value("HIGH"))
                    .andExpect(jsonPath("$.content[0].status").value("OPEN"));
        }
    }

    @Nested
    @DisplayName("Assignment flow")
    class AssignmentFlow {

        @Test
        @DisplayName("Should assign technician and persist full state via downstream validation")
        void shouldAssignTechnicianEndToEnd() throws Exception {
            UUID workOrderId = createWorkOrder("Assignment test", "End to end", "US", "HIGH");
            UUID technicianId = UUID.randomUUID();

            wireMockServer.resetRequests();
            stubTechnician(technicianId, "US", true, "AVAILABLE");

            mockMvc.perform(post("/api/work-orders/{id}/assign/{technicianId}",
                            workOrderId, technicianId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("ASSIGNED"))
                    .andExpect(jsonPath("$.assignedTechnicianId").value(technicianId.toString()))
                    .andExpect(jsonPath("$.statusHistory.length()").value(2))
                    .andExpect(jsonPath("$.statusHistory[1].fromStatus").value("OPEN"))
                    .andExpect(jsonPath("$.statusHistory[1].toStatus").value("ASSIGNED"));

            WorkOrder persisted = workOrderRepository.findByIdWithDetails(workOrderId).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(WorkOrderStatus.ASSIGNED);
            assertThat(persisted.getAssignedTechnicianId()).isEqualTo(technicianId);
            assertThat(persisted.getStatusHistory()).hasSize(2);
            wireMockServer.verify(1, postRequestedFor(urlPathEqualTo("/api/audit-events")));
        }

        @Test
        @DisplayName("Should return 409 and keep work order unchanged when technician is inactive")
        void shouldReturn409WhenTechnicianInactive() throws Exception {
            UUID workOrderId = createWorkOrder("Inactive tech test", "Failure path", "US", "MEDIUM");
            UUID technicianId = UUID.randomUUID();

            wireMockServer.resetRequests();
            stubTechnician(technicianId, "US", false, "AVAILABLE");

            mockMvc.perform(post("/api/work-orders/{id}/assign/{technicianId}",
                            workOrderId, technicianId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("TECHNICIAN_NOT_ACTIVE"));

            WorkOrder persisted = workOrderRepository.findByIdWithDetails(workOrderId).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(WorkOrderStatus.OPEN);
            assertThat(persisted.getAssignedTechnicianId()).isNull();
            wireMockServer.verify(0, postRequestedFor(urlPathEqualTo("/api/audit-events")));
        }

        @Test
        @DisplayName("Should return 409 and keep work order unchanged when country does not match")
        void shouldReturn409WhenCountryMismatch() throws Exception {
            UUID workOrderId = createWorkOrder("Country test", "Mismatch path", "US", "HIGH");
            UUID technicianId = UUID.randomUUID();

            wireMockServer.resetRequests();
            stubTechnician(technicianId, "DE", true, "AVAILABLE");

            mockMvc.perform(post("/api/work-orders/{id}/assign/{technicianId}",
                            workOrderId, technicianId))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("COUNTRY_MISMATCH"));

            WorkOrder persisted = workOrderRepository.findByIdWithDetails(workOrderId).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(WorkOrderStatus.OPEN);
            assertThat(persisted.getAssignedTechnicianId()).isNull();
            wireMockServer.verify(0, postRequestedFor(urlPathEqualTo("/api/audit-events")));
        }

        @Test
        @DisplayName("Should return 502 when technician service returns server error")
        void shouldReturn502WhenTechnicianServiceDown() throws Exception {
            UUID workOrderId = createWorkOrder("Service down test", "Failure path", "US", "LOW");
            UUID technicianId = UUID.randomUUID();

            wireMockServer.resetRequests();
            wireMockServer.stubFor(get(urlPathEqualTo("/api/technicians/" + technicianId))
                    .willReturn(aResponse()
                            .withStatus(500)
                            .withBody("Internal Server Error")));

            mockMvc.perform(post("/api/work-orders/{id}/assign/{technicianId}",
                            workOrderId, technicianId))
                    .andExpect(status().isBadGateway())
                    .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));

            WorkOrder persisted = workOrderRepository.findByIdWithDetails(workOrderId).orElseThrow();
            assertThat(persisted.getStatus()).isEqualTo(WorkOrderStatus.OPEN);
            assertThat(persisted.getAssignedTechnicianId()).isNull();
        }
    }

    private void stubAuditSuccess() {
        wireMockServer.stubFor(com.github.tomakehurst.wiremock.client.WireMock.post(urlPathEqualTo("/api/audit-events"))
                .willReturn(aResponse().withStatus(201)));
    }

    private void stubTechnician(UUID technicianId, String countryCode, boolean active, String availability) {
        wireMockServer.stubFor(get(urlPathEqualTo("/api/technicians/" + technicianId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                  "id": "%s",
                                  "countryCode": "%s",
                                  "active": %s,
                                  "availability": "%s"
                                }
                                """.formatted(technicianId, countryCode, active, availability))));
    }

    private UUID createWorkOrder(String title, String summary, String countryCode, String priority) throws Exception {
        String response = mockMvc.perform(post("/api/work-orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "summary": "%s",
                                  "countryCode": "%s",
                                  "priority": "%s"
                                }
                                """.formatted(title, summary, countryCode, priority)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        return UUID.fromString(objectMapper.readTree(response).get("id").asText());
    }
}