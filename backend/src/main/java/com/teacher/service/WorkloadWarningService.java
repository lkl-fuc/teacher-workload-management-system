package com.teacher.service;

import com.teacher.dto.WorkloadAnalysisResponse;
import com.teacher.entity.WarningRecord;

import java.util.List;
import java.util.Map;

public interface WorkloadWarningService {
    WorkloadAnalysisResponse analyzeTeacherWorkloads(boolean persistWarnings);

    List<WarningRecord> getTeacherWarnings(Long teacherId);

    WarningRecord acknowledgeWarning(Long teacherId, Long warningId);

    List<Map<String, Object>> getWarningRecordsForAdmin();
}
