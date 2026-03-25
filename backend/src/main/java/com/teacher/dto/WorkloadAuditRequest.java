package com.teacher.dto;

import lombok.Data;

@Data
public class WorkloadAuditRequest {
    private String status;
    private String rejectReason;
}
