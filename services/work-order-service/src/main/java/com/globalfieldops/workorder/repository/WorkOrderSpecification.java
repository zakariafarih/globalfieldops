package com.globalfieldops.workorder.repository;

import org.springframework.data.jpa.domain.Specification;

import com.globalfieldops.workorder.entity.WorkOrder;
import com.globalfieldops.workorder.entity.WorkOrderPriority;
import com.globalfieldops.workorder.entity.WorkOrderStatus;

public final class WorkOrderSpecification {

    private WorkOrderSpecification() {
        // utility class - no instantiation
    }

    public static Specification<WorkOrder> hasCountryCode(String countryCode) {
        return (root, query, cb) ->
                cb.equal(root.get("countryCode"), countryCode.toUpperCase());
    }

    public static Specification<WorkOrder> hasPriority(WorkOrderPriority priority) {
        return (root, query, cb) ->
                cb.equal(root.get("priority"), priority);
    }

    public static Specification<WorkOrder> hasStatus(WorkOrderStatus status) {
        return (root, query, cb) ->
                cb.equal(root.get("status"), status);
    }
}