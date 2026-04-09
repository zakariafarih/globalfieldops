package com.globalfieldops.workorder.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.globalfieldops.workorder.entity.WorkOrder;

public interface WorkOrderRepository extends JpaRepository<WorkOrder, UUID>,
        JpaSpecificationExecutor<WorkOrder>, WorkOrderRepositoryCustom {
}