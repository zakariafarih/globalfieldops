package com.globalfieldops.technician.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    @DisplayName("Should propagate existing correlation ID to MDC and response")
    void shouldPropagateExistingCorrelationId() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        String incomingId = "abc-123-def";
        request.addHeader("X-Correlation-Id", incomingId);

        filter.doFilterInternal(request, response, (req, res) -> {
            assertThat(MDC.get("correlationId")).isEqualTo(incomingId);
        });

        assertThat(response.getHeader("X-Correlation-Id")).isEqualTo(incomingId);
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    @DisplayName("Should generate correlation ID when absent")
    void shouldGenerateCorrelationIdWhenAbsent() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, (req, res) -> {
            assertThat(MDC.get("correlationId")).isNotBlank();
        });

        String generatedId = response.getHeader("X-Correlation-Id");
        assertThat(generatedId).isNotBlank();
        assertThat(MDC.get("correlationId")).isNull();
    }

    @Test
    @DisplayName("Should clean up MDC even when filter chain throws")
    void shouldCleanUpMdcOnException() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        request.addHeader("X-Correlation-Id", "error-flow-id");

        try {
            filter.doFilterInternal(request, response, (req, res) -> {
                assertThat(MDC.get("correlationId")).isEqualTo("error-flow-id");
                throw new ServletException("simulated failure");
            });
        } catch (Exception ignored) {
            // expected
        }

        assertThat(MDC.get("correlationId")).isNull();
    }
}
