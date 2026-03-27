package com.teacher.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class TeacherWorkloadAnalysisItem {
    private Long teacherId;
    private String teacherName;
    private String postType;
    private String title;
    private BigDecimal totalAmount;
    private BigDecimal equivalentAmount;
    private BigDecimal minThreshold;
    private BigDecimal maxThreshold;
    private String level;
    private String warningMessage;
}
