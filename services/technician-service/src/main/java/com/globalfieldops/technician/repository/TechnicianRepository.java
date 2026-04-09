package com.globalfieldops.technician.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import com.globalfieldops.technician.entity.Technician;

public interface TechnicianRepository
        extends JpaRepository<Technician, UUID>, JpaSpecificationExecutor<Technician> {

    @Query("SELECT t FROM Technician t LEFT JOIN FETCH t.skills WHERE t.id = :id")
    Optional<Technician> findByIdWithSkills(UUID id);

    boolean existsByEmployeeCode(String employeeCode);

    boolean existsByEmail(String email);
}
