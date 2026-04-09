package com.globalfieldops.workorder.client;

import java.util.UUID;

import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.globalfieldops.workorder.dto.response.TechnicianValidationResponse;
import com.globalfieldops.common.exception.ResourceNotFoundException;
import com.globalfieldops.workorder.exception.ServiceCommunicationException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TechnicianClient {

    private final RestClient technicianRestClient;

    public TechnicianValidationResponse getTechnician(UUID technicianId) {
        try {
            return technicianRestClient.get()
                    .uri("/api/technicians/{id}", technicianId)
                    .retrieve()
                    .body(TechnicianValidationResponse.class);
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ResourceNotFoundException("Technician", technicianId);
        } catch (RestClientException ex) {
            log.warn("Failed to reach technician-service for technician {}: {}",
                    technicianId, ex.getMessage());
            throw new ServiceCommunicationException("technician-service", ex);
        }
    }
}
