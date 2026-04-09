package com.globalfieldops.workorder.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.globalfieldops.workorder.dto.request.AddWorkOrderCommentRequest;
import com.globalfieldops.workorder.dto.request.ChangeWorkOrderStatusRequest;
import com.globalfieldops.workorder.dto.request.CreateWorkOrderRequest;
import com.globalfieldops.workorder.dto.response.WorkOrderResponse;
import com.globalfieldops.workorder.dto.response.WorkOrderSummaryResponse;
import com.globalfieldops.workorder.entity.WorkOrderPriority;
import com.globalfieldops.workorder.entity.WorkOrderStatus;
import com.globalfieldops.workorder.service.WorkOrderService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final WorkOrderService workOrderService;

    @PostMapping
    public ResponseEntity<WorkOrderResponse> create(
            @Valid @RequestBody CreateWorkOrderRequest request) {
        WorkOrderResponse response = workOrderService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkOrderResponse> findById(@PathVariable UUID id) {
        WorkOrderResponse response = workOrderService.findById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<WorkOrderSummaryResponse>> findAll(
            @RequestParam(required = false) String countryCode,
            @RequestParam(required = false) WorkOrderPriority priority,
            @RequestParam(required = false) WorkOrderStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<WorkOrderSummaryResponse> page = workOrderService.findAll(countryCode, priority, status, pageable);
        return ResponseEntity.ok(page);
    }

    @PostMapping("/{id}/assign/{technicianId}")
    public ResponseEntity<WorkOrderResponse> assignTechnician(
            @PathVariable UUID id,
            @PathVariable UUID technicianId) {
        WorkOrderResponse response = workOrderService.assignTechnician(id, technicianId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/status")
    public ResponseEntity<WorkOrderResponse> changeStatus(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeWorkOrderStatusRequest request) {
        WorkOrderResponse response = workOrderService.changeStatus(id, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<WorkOrderResponse> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody AddWorkOrderCommentRequest request) {
        WorkOrderResponse response = workOrderService.addComment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}