package com.teacher.service;

import com.teacher.dto.WorkloadAuditRequest;
import com.teacher.entity.Workload;

import java.util.List;

public interface WorkloadService {
    List<Workload> getAllWorkloads();

    Workload getWorkloadById(Long id);

    Workload createWorkload(Workload workload);

    Workload updateWorkload(Long id, Workload workload);

    void deleteWorkload(Long id);

    List<Workload> getWorkloadsByTeacherId(Long teacherId);

    Workload auditWorkload(Long id, WorkloadAuditRequest request);
}
