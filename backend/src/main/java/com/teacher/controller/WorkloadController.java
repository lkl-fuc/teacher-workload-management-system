package com.teacher.controller;

import com.teacher.dto.WorkloadAuditRequest;
import com.teacher.entity.Workload;
import com.teacher.service.WorkloadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workloads")
@CrossOrigin
public class WorkloadController {

    private final WorkloadService workloadService;

    public WorkloadController(WorkloadService workloadService) {
        this.workloadService = workloadService;
    }

    @GetMapping
    public List<Workload> getAllWorkloads() {
        return workloadService.getAllWorkloads();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkloadById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(workloadService.getWorkloadById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/teacher/{teacherId}")
    public ResponseEntity<?> getWorkloadsByTeacherId(@PathVariable Long teacherId) {
        try {
            return ResponseEntity.ok(workloadService.getWorkloadsByTeacherId(teacherId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createWorkload(@RequestBody Workload workload) {
        try {
            Workload created = workloadService.createWorkload(workload);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkload(@PathVariable Long id, @RequestBody Workload workload) {
        try {
            Workload updated = workloadService.updateWorkload(id, workload);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}/audit")
    public ResponseEntity<?> auditWorkload(@PathVariable Long id, @RequestBody WorkloadAuditRequest request) {
        try {
            return ResponseEntity.ok(workloadService.auditWorkload(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteWorkload(@PathVariable Long id) {
        Map<String, String> result = new HashMap<>();
        try {
            workloadService.deleteWorkload(id);
            result.put("message", "删除成功");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            result.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }
}
