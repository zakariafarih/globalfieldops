package com.globalfieldops.technician.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.globalfieldops.technician.dto.request.ChangeActivationRequest;
import com.globalfieldops.technician.dto.request.ChangeAvailabilityRequest;
import com.globalfieldops.technician.dto.request.CreateTechnicianRequest;
import com.globalfieldops.technician.dto.response.TechnicianResponse;
import com.globalfieldops.technician.dto.response.TechnicianSummaryResponse;
import com.globalfieldops.technician.entity.AvailabilityStatus;
import com.globalfieldops.technician.service.TechnicianService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/technicians")
@RequiredArgsConstructor
public class TechnicianController {

    private final TechnicianService technicianService;

    @PostMapping
    public ResponseEntity<TechnicianResponse> create(
            @Valid @RequestBody CreateTechnicianRequest request) {
        TechnicianResponse response = technicianService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TechnicianResponse> findById(@PathVariable UUID id) {
        TechnicianResponse response = technicianService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<TechnicianSummaryResponse>> findAll(
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) AvailabilityStatus availability,
            @RequestParam(required = false) String skillName,
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = org.springframework.data.domain.Sort.Direction.DESC) Pageable pageable) {
        Page<TechnicianSummaryResponse> page = technicianService.findAll(
                countryCode, active, availability, skillName, pageable);
        return ResponseEntity.ok(page);
    }

    @PatchMapping("/{id}/availability")
    public ResponseEntity<TechnicianResponse> changeAvailability(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeAvailabilityRequest request) {
        TechnicianResponse response = technicianService.changeAvailability(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/activation")
    public ResponseEntity<TechnicianResponse> changeActivation(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeActivationRequest request) {
        TechnicianResponse response = technicianService.changeActivation(id, request);
        return ResponseEntity.ok(response);
    }
}
