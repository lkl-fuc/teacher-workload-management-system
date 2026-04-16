package com.teacher.config;

import com.teacher.service.WorkloadWarningService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class WorkloadWarningScheduler {

    private final WorkloadWarningService workloadWarningService;

    public WorkloadWarningScheduler(WorkloadWarningService workloadWarningService) {
        this.workloadWarningService = workloadWarningService;
    }

    @Scheduled(cron = "0 0 9 ? * MON")
    public void scanWarningsPeriodically() {
        workloadWarningService.analyzeTeacherWorkloads(true);
    }
}
