package com.teacher.dto;

import lombok.Data;

import java.util.List;

@Data
public class WorkloadAnalysisResponse {
    private WorkloadAnalysisSummary summary;
    private List<TeacherWorkloadAnalysisItem> items;
}
