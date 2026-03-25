package com.teacher.controller;

import com.teacher.entity.WorkloadType;
import com.teacher.service.WorkloadTypeService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/workload-types")
@CrossOrigin
public class WorkloadTypeController {

    private final WorkloadTypeService workloadTypeService;

    public WorkloadTypeController(WorkloadTypeService workloadTypeService) {
        this.workloadTypeService = workloadTypeService;
    }

    @GetMapping
    public List<WorkloadType> getAllWorkloadTypes() {
        return workloadTypeService.getAllWorkloadTypes();
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getWorkloadTypeById(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(workloadTypeService.getWorkloadTypeById(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createWorkloadType(@RequestBody WorkloadType workloadType) {
        try {
            WorkloadType created = workloadTypeService.createWorkloadType(workloadType);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateWorkloadType(@PathVariable Long id, @RequestBody WorkloadType workloadType) {
        try {
            WorkloadType updated = workloadTypeService.updateWorkloadType(id, workloadType);
            return ResponseEntity.ok(updated);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteWorkloadType(@PathVariable Long id) {
        Map<String, String> result = new HashMap<>();
        try {
            workloadTypeService.deleteWorkloadType(id);
            result.put("message", "删除成功");
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            result.put("message", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(result);
        }
    }
}
