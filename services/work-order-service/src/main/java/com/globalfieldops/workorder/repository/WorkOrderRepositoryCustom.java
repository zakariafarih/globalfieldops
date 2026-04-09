package com.globalfieldops.workorder.repository;

import java.util.Optional;
import java.util.UUID;

import com.globalfieldops.workorder.entity.WorkOrder;

public interface WorkOrderRepositoryCustom {

    Optional<WorkOrder> findByIdWithDetails(UUID id);
}
