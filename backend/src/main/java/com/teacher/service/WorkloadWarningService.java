package com.teacher.service;

import com.teacher.dto.WorkloadAnalysisResponse;
import com.teacher.entity.WarningRecord;

import java.util.List;

public interface WorkloadWarningService {
    WorkloadAnalysisResponse analyzeTeacherWorkloads(boolean persistWarnings);

    List<WarningRecord> getTeacherWarnings(Long teacherId);
}
