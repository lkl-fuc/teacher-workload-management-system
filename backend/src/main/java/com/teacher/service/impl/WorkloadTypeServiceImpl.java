package com.teacher.service.impl;

import com.teacher.entity.WorkloadType;
import com.teacher.repository.WorkloadTypeRepository;
import com.teacher.service.WorkloadTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class WorkloadTypeServiceImpl implements WorkloadTypeService {
    private static final Set<String> ALLOWED_CATEGORIES = new LinkedHashSet<>(List.of(
            "专任教师岗",
            "实验教师岗",
            "辅导员岗",
            "教辅岗",
            "行政兼课岗",
            "外聘教师岗"
    ));

    private final WorkloadTypeRepository workloadTypeRepository;

    public WorkloadTypeServiceImpl(WorkloadTypeRepository workloadTypeRepository) {
        this.workloadTypeRepository = workloadTypeRepository;
    }

    @Override
    public List<WorkloadType> getAllWorkloadTypes() {
        return workloadTypeRepository.findAll();
    }

    @Override
    public WorkloadType getWorkloadTypeById(Long id) {
        return workloadTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工作量类型不存在，id=" + id));
    }

    @Override
    @Transactional
    public WorkloadType createWorkloadType(WorkloadType workloadType) {
        String normalizedTypeName = buildNormalizedTypeName(workloadType);
        if (normalizedTypeName == null || normalizedTypeName.isBlank()) {
            throw new IllegalArgumentException("工作量类型名称不能为空");
        }
        validateCategory(workloadType.getCategoryName());
        if (workloadTypeRepository.existsByTypeName(normalizedTypeName)) {
            throw new IllegalArgumentException("工作量类型名称已存在：" + normalizedTypeName);
        }
        if (workloadType.getUnitValue() == null) {
            throw new IllegalArgumentException("单位分值不能为空");
        }
        if (workloadType.getUnitValue().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("单位分值不能小于0");
        }
        workloadType.setTypeName(normalizedTypeName);
        workloadType.setCategoryName(trimToNull(workloadType.getCategoryName()));
        workloadType.setSubTypeName(trimToNull(workloadType.getSubTypeName()));
        workloadType.setRemark(trimToNull(workloadType.getRemark()));
        workloadType.setId(null);
        return workloadTypeRepository.save(workloadType);
    }

    @Override
    @Transactional
    public WorkloadType updateWorkloadType(Long id, WorkloadType workloadType) {
        WorkloadType existing = workloadTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工作量类型不存在，id=" + id));

        String normalizedTypeName = buildNormalizedTypeName(workloadType);
        if (normalizedTypeName != null && !normalizedTypeName.isBlank()) {
            workloadTypeRepository.findByTypeNameAndIdNot(normalizedTypeName, id)
                    .ifPresent(w -> {
                        throw new IllegalArgumentException("工作量类型名称已存在：" + normalizedTypeName);
                    });
            existing.setTypeName(normalizedTypeName);
        }
        validateCategory(workloadType.getCategoryName());
        existing.setCategoryName(trimToNull(workloadType.getCategoryName()));
        existing.setSubTypeName(trimToNull(workloadType.getSubTypeName()));

        if (workloadType.getUnitValue() != null) {
            if (workloadType.getUnitValue().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("单位分值不能小于0");
            }
            existing.setUnitValue(workloadType.getUnitValue());
        }
        if (workloadType.getRemark() != null || existing.getRemark() != null) {
            existing.setRemark(trimToNull(workloadType.getRemark()));
        }

        return workloadTypeRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteWorkloadType(Long id) {
        if (!workloadTypeRepository.existsById(id)) {
            throw new IllegalArgumentException("工作量类型不存在，id=" + id);
        }
        workloadTypeRepository.deleteById(id);
    }

    private String buildNormalizedTypeName(WorkloadType workloadType) {
        String categoryName = trimToNull(workloadType.getCategoryName());
        String subTypeName = trimToNull(workloadType.getSubTypeName());
        String typeName = trimToNull(workloadType.getTypeName());

        if (categoryName != null && subTypeName != null) {
            return categoryName + " - " + subTypeName;
        }
        if (typeName != null) {
            return typeName;
        }
        if (categoryName != null) {
            return categoryName;
        }
        return subTypeName;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void validateCategory(String categoryName) {
        String normalized = trimToNull(categoryName);
        if (normalized == null) {
            throw new IllegalArgumentException("工作大类不能为空");
        }
        if (!ALLOWED_CATEGORIES.contains(normalized)) {
            throw new IllegalArgumentException("仅支持以下工作大类：" + String.join("、", ALLOWED_CATEGORIES));
        }
    }
}
