package com.globalfieldops.gateway;

import com.globalfieldops.gateway.support.JwtTestUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewaySecurityTest {

    @Autowired
    private TestRestTemplate restTemplate;

    // ---- Helper methods ----

    private ResponseEntity<String> request(HttpMethod method, String path, String token) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return restTemplate.exchange(path, method, new HttpEntity<>(headers), String.class);
    }

    private ResponseEntity<String> get(String path) {
        return request(HttpMethod.GET, path, null);
    }

    private ResponseEntity<String> get(String path, String token) {
        return request(HttpMethod.GET, path, token);
    }

    private ResponseEntity<String> post(String path, String token) {
        return request(HttpMethod.POST, path, token);
    }

    // ---- Public endpoints ----

    @Nested
    @DisplayName("Public endpoints")
    class PublicEndpoints {

        @Test
        @DisplayName("Health endpoint is accessible without authentication")
        void healthEndpointIsPublic() {
            ResponseEntity<String> response = get("/actuator/health");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }
    }

    // ---- Unauthenticated access ----

    @Nested
    @DisplayName("Unauthenticated requests")
    class UnauthenticatedRequests {

        @Test
        @DisplayName("Audit route returns 401 without token")
        void auditRouteDeniedWithoutToken() {
            ResponseEntity<String> response = get("/api/audit-events");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Technician route returns 401 without token")
        void technicianRouteDeniedWithoutToken() {
            ResponseEntity<String> response = get("/api/technicians");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Work-order route returns 401 without token")
        void workOrderRouteDeniedWithoutToken() {
            ResponseEntity<String> response = get("/api/work-orders");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    // ---- Role-based access: Audit routes ----

    @Nested
    @DisplayName("Audit route authorization")
    class AuditRouteAuthorization {

        @Test
        @DisplayName("Dispatcher cannot access audit events")
        void dispatcherDeniedAuditAccess() {
            ResponseEntity<String> response = get("/api/audit-events", JwtTestUtil.dispatcherToken());
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Technician cannot access audit events")
        void technicianDeniedAuditAccess() {
            ResponseEntity<String> response = get("/api/audit-events", JwtTestUtil.technicianToken());
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Admin can access audit events (passes security)")
        void adminAllowedAuditAccess() {
            ResponseEntity<String> response = get("/api/audit-events", JwtTestUtil.adminToken());
            // Downstream not running, but security filter passed — NOT 401/403
            assertThat(response.getStatusCode()).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        }
    }

    // ---- Role-based access: Technician write routes ----

    @Nested
    @DisplayName("Technician write authorization")
    class TechnicianWriteAuthorization {

        @Test
        @DisplayName("Dispatcher cannot create technicians")
        void dispatcherDeniedTechnicianWrite() {
            ResponseEntity<String> response = post("/api/technicians", JwtTestUtil.dispatcherToken());
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Technician cannot create technicians")
        void technicianDeniedTechnicianWrite() {
            ResponseEntity<String> response = post("/api/technicians", JwtTestUtil.technicianToken());
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Admin can create technicians (passes security)")
        void adminAllowedTechnicianWrite() {
            ResponseEntity<String> response = post("/api/technicians", JwtTestUtil.adminToken());
            assertThat(response.getStatusCode()).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        }
    }

    // ---- Role-based access: Work-order write routes ----

    @Nested
    @DisplayName("Work-order write authorization")
    class WorkOrderWriteAuthorization {

        @Test
        @DisplayName("Technician cannot create work orders")
        void technicianDeniedWorkOrderWrite() {
            ResponseEntity<String> response = post("/api/work-orders", JwtTestUtil.technicianToken());
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Dispatcher can create work orders (passes security)")
        void dispatcherAllowedWorkOrderWrite() {
            ResponseEntity<String> response = post("/api/work-orders", JwtTestUtil.dispatcherToken());
            assertThat(response.getStatusCode()).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Admin can create work orders (passes security)")
        void adminAllowedWorkOrderWrite() {
            ResponseEntity<String> response = post("/api/work-orders", JwtTestUtil.adminToken());
            assertThat(response.getStatusCode()).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        }
    }

    // ---- Role-based access: Read routes ----

    @Nested
    @DisplayName("Authenticated read access")
    class AuthenticatedReadAccess {

        @Test
        @DisplayName("Any authenticated user can read technicians")
        void authenticatedUserCanReadTechnicians() {
            ResponseEntity<String> response = get("/api/technicians", JwtTestUtil.technicianToken());
            assertThat(response.getStatusCode()).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("Any authenticated user can read work orders")
        void authenticatedUserCanReadWorkOrders() {
            ResponseEntity<String> response = get("/api/work-orders", JwtTestUtil.technicianToken());
            assertThat(response.getStatusCode()).isNotIn(HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        }
    }

    // ---- Default deny ----

    @Nested
    @DisplayName("Default deny policy")
    class DefaultDenyPolicy {

        @Test
        @DisplayName("Unknown route returns 401 without token")
        void unknownRouteDeniedWithoutToken() {
            ResponseEntity<String> response = get("/api/unknown");
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("Unknown route returns 403 with valid token")
        void unknownRouteDeniedWithToken() {
            ResponseEntity<String> response = get("/api/unknown", JwtTestUtil.adminToken());
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
