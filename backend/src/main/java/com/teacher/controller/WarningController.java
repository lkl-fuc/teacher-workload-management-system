package com.teacher.controller;

import com.teacher.service.WorkloadWarningService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/warnings")
@CrossOrigin
public class WarningController {

    private final WorkloadWarningService workloadWarningService;

    public WarningController(WorkloadWarningService workloadWarningService) {
        this.workloadWarningService = workloadWarningService;
    }

    @GetMapping("/analysis")
    public ResponseEntity<?> getAnalysis() {
        return ResponseEntity.ok(workloadWarningService.analyzeTeacherWorkloads(false));
    }

    @PostMapping("/recalculate")
    public ResponseEntity<?> recalculateWarnings() {
        return ResponseEntity.ok(workloadWarningService.analyzeTeacherWorkloads(true));
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<?> getTeacherWarnings(@PathVariable Long teacherId) {
        try {
            return ResponseEntity.ok(workloadWarningService.getTeacherWarnings(teacherId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/{warningId}/acknowledge")
    public ResponseEntity<?> acknowledgeWarning(@PathVariable Long warningId,
                                                @RequestParam Long teacherId) {
        try {
            return ResponseEntity.ok(workloadWarningService.acknowledgeWarning(teacherId, warningId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/records")
    public ResponseEntity<?> getWarningRecordsForAdmin() {
        return ResponseEntity.ok(workloadWarningService.getWarningRecordsForAdmin());
    }
}
