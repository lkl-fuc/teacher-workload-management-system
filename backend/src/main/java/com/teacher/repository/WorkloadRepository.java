package com.teacher.repository;

import com.teacher.entity.Workload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkloadRepository extends JpaRepository<Workload, Long> {
    List<Workload> findByTeacherId(Long teacherId);

    List<Workload> findByStatusIgnoreCase(String status);
}
