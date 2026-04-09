package com.globalfieldops.workorder.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.transaction.annotation.Transactional;

import com.globalfieldops.workorder.entity.WorkOrder;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class WorkOrderRepositoryImpl implements WorkOrderRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkOrder> findByIdWithDetails(UUID id) {
        List<WorkOrder> results = entityManager.createQuery(
                        "SELECT DISTINCT wo FROM WorkOrder wo LEFT JOIN FETCH wo.comments WHERE wo.id = :id",
                        WorkOrder.class)
                .setParameter("id", id)
                .getResultList();

        if (results.isEmpty()) {
            return Optional.empty();
        }

        // Second query eagerly fetches status history — Hibernate L1 cache
        // merges it into the entity already loaded by the first query
        entityManager.createQuery(
                        "SELECT DISTINCT wo FROM WorkOrder wo LEFT JOIN FETCH wo.statusHistory WHERE wo.id = :id",
                        WorkOrder.class)
                .setParameter("id", id)
                .getResultList();

        return Optional.of(results.getFirst());
    }
}
