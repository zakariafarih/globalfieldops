package com.globalfieldops.workorder.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.globalfieldops.workorder.dto.response.WorkOrderCommentResponse;
import com.globalfieldops.workorder.dto.response.WorkOrderResponse;
import com.globalfieldops.workorder.dto.response.WorkOrderStatusHistoryResponse;
import com.globalfieldops.workorder.dto.response.WorkOrderSummaryResponse;
import com.globalfieldops.workorder.entity.WorkOrder;
import com.globalfieldops.workorder.entity.WorkOrderComment;
import com.globalfieldops.workorder.entity.WorkOrderStatusHistory;

@Component
public class WorkOrderMapper {

    public WorkOrderResponse toResponse(WorkOrder entity) {
        return new WorkOrderResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getSummary(),
                entity.getCountryCode(),
                entity.getRegion(),
                entity.getPriority().name(),
                entity.getStatus().name(),
                entity.getAssignedTechnicianId(),
                toCommentResponseList(entity.getComments()),
                toStatusHistoryResponseList(entity.getStatusHistory()),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public WorkOrderSummaryResponse toSummaryResponse(WorkOrder entity) {
        return new WorkOrderSummaryResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getCountryCode(),
                entity.getRegion(),
                entity.getPriority().name(),
                entity.getStatus().name(),
                entity.getAssignedTechnicianId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public WorkOrderCommentResponse toCommentResponse(WorkOrderComment entity) {
        return new WorkOrderCommentResponse(
                entity.getId(),
                entity.getAuthorName(),
                entity.getBody(),
                entity.getCreatedAt()
        );
    }

    public WorkOrderStatusHistoryResponse toStatusHistoryResponse(WorkOrderStatusHistory entity) {
        return new WorkOrderStatusHistoryResponse(
                entity.getId(),
                entity.getFromStatus() != null ? entity.getFromStatus().name() : null,
                entity.getToStatus().name(),
                entity.getChangedBy(),
                entity.getChangeReason(),
                entity.getChangedAt()
        );
    }

    private List<WorkOrderCommentResponse> toCommentResponseList(List<WorkOrderComment> comments) {
        return comments.stream()
                .map(this::toCommentResponse)
                .toList();
    }

    private List<WorkOrderStatusHistoryResponse> toStatusHistoryResponseList(List<WorkOrderStatusHistory> statusHistory) {
        return statusHistory.stream()
                .map(this::toStatusHistoryResponse)
                .toList();
    }
}