package com.teacher.repository;

import com.teacher.entity.WorkloadType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkloadTypeRepository extends JpaRepository<WorkloadType, Long> {
    Optional<WorkloadType> findByTypeNameAndIdNot(String typeName, Long id);

    boolean existsByTypeName(String typeName);
}
