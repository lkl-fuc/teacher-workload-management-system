package com.teacher.repository;

import com.teacher.entity.WorkloadType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkloadTypeRepository extends JpaRepository<WorkloadType, Long> {
}