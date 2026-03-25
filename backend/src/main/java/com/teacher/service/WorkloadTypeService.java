package com.teacher.service;

import com.teacher.entity.WorkloadType;

import java.util.List;

public interface WorkloadTypeService {
    List<WorkloadType> getAllWorkloadTypes();

    WorkloadType getWorkloadTypeById(Long id);

    WorkloadType createWorkloadType(WorkloadType workloadType);

    WorkloadType updateWorkloadType(Long id, WorkloadType workloadType);

    void deleteWorkloadType(Long id);
}
