package com.teacher.dto;

import lombok.Data;

@Data
public class WorkloadAnalysisSummary {
    private int lowCount;
    private int normalCount;
    private int highCount;
    private int teacherCount;
}
