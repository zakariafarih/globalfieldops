package com.globalfieldops.gateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.globalfieldops.gateway.support.JwtTestUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.matching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingTest {

    static WireMockServer downstreamServer = new WireMockServer(wireMockConfig().dynamicPort());

    static {
        downstreamServer.start();
    }

    @DynamicPropertySource
    static void overrideRoutes(DynamicPropertyRegistry registry) {
        String baseUrl = downstreamServer.baseUrl();
        registry.add("TECHNICIAN_SERVICE_URL", () -> baseUrl);
        registry.add("WORK_ORDER_SERVICE_URL", () -> baseUrl);
        registry.add("AUDIT_SERVICE_URL", () -> baseUrl);
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @AfterAll
    static void stopWireMock() {
        downstreamServer.stop();
    }

    @BeforeEach
    void resetStubs() {
        downstreamServer.resetAll();
    }

    private ResponseEntity<String> authenticatedGet(String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        return restTemplate.exchange(path, HttpMethod.GET, new HttpEntity<>(headers), String.class);
    }

    // ---- Route forwarding ----

    @Test
    @DisplayName("GET /api/technicians is forwarded to technician-service")
    void technicianRouteForwarded() {
        downstreamServer.stubFor(WireMock.get(urlPathEqualTo("/api/technicians"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        String token = JwtTestUtil.adminToken();
        ResponseEntity<String> response = authenticatedGet("/api/technicians", token);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
        downstreamServer.verify(getRequestedFor(urlPathEqualTo("/api/technicians"))
                .withHeader("Authorization", matching("Bearer .+")));
    }

    @Test
    @DisplayName("GET /api/work-orders is forwarded to work-order-service")
    void workOrderRouteForwarded() {
        downstreamServer.stubFor(WireMock.get(urlPathEqualTo("/api/work-orders"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        ResponseEntity<String> response = authenticatedGet("/api/work-orders", JwtTestUtil.dispatcherToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
        downstreamServer.verify(getRequestedFor(urlPathEqualTo("/api/work-orders"))
                .withHeader("Authorization", matching("Bearer .+")));
    }

    @Test
    @DisplayName("GET /api/audit-events is forwarded to audit-service")
    void auditRouteForwarded() {
        downstreamServer.stubFor(WireMock.get(urlPathEqualTo("/api/audit-events"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        ResponseEntity<String> response = authenticatedGet("/api/audit-events", JwtTestUtil.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("[]");
        downstreamServer.verify(getRequestedFor(urlPathEqualTo("/api/audit-events"))
                .withHeader("Authorization", matching("Bearer .+")));
    }

    // ---- Correlation ID forwarding ----

    @Test
    @DisplayName("Existing X-Correlation-Id is forwarded to downstream service")
    void existingCorrelationIdForwarded() {
        String correlationId = "test-correlation-123";

        downstreamServer.stubFor(WireMock.get(urlPathEqualTo("/api/technicians"))
                .willReturn(aResponse().withStatus(200).withBody("[]")));

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(JwtTestUtil.adminToken());
        headers.set("X-Correlation-Id", correlationId);

        ResponseEntity<String> response = restTemplate.exchange(
                "/api/technicians", HttpMethod.GET, new HttpEntity<>(headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-Correlation-Id")).isEqualTo(correlationId);

        downstreamServer.verify(getRequestedFor(urlPathEqualTo("/api/technicians"))
                .withHeader("X-Correlation-Id", equalTo(correlationId)));
    }

    @Test
    @DisplayName("X-Correlation-Id is generated when absent and forwarded downstream")
    void correlationIdGeneratedWhenAbsent() {
        downstreamServer.stubFor(WireMock.get(urlPathEqualTo("/api/technicians"))
                .willReturn(aResponse().withStatus(200).withBody("[]")));

        ResponseEntity<String> response = authenticatedGet("/api/technicians", JwtTestUtil.adminToken());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String generatedId = response.getHeaders().getFirst("X-Correlation-Id");
        assertThat(generatedId).isNotNull().isNotBlank();
        assertThatNoException().isThrownBy(() -> UUID.fromString(generatedId));

        // Verify it was also forwarded to the downstream
        downstreamServer.verify(getRequestedFor(urlPathEqualTo("/api/technicians"))
                .withHeader("X-Correlation-Id", equalTo(generatedId)));
    }
}
