package com.teacher.service.impl;

import com.teacher.entity.WorkloadType;
import com.teacher.repository.WorkloadTypeRepository;
import com.teacher.service.WorkloadTypeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkloadTypeServiceImpl implements WorkloadTypeService {

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
        if (workloadType.getTypeName() == null || workloadType.getTypeName().isBlank()) {
            throw new IllegalArgumentException("工作量类型名称不能为空");
        }
        if (workloadTypeRepository.existsByTypeName(workloadType.getTypeName())) {
            throw new IllegalArgumentException("工作量类型名称已存在：" + workloadType.getTypeName());
        }
        workloadType.setId(null);
        return workloadTypeRepository.save(workloadType);
    }

    @Override
    @Transactional
    public WorkloadType updateWorkloadType(Long id, WorkloadType workloadType) {
        WorkloadType existing = workloadTypeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工作量类型不存在，id=" + id));

        if (workloadType.getTypeName() != null && !workloadType.getTypeName().isBlank()) {
            workloadTypeRepository.findByTypeNameAndIdNot(workloadType.getTypeName(), id)
                    .ifPresent(w -> {
                        throw new IllegalArgumentException("工作量类型名称已存在：" + workloadType.getTypeName());
                    });
            existing.setTypeName(workloadType.getTypeName());
        }

        if (workloadType.getUnitValue() != null) {
            existing.setUnitValue(workloadType.getUnitValue());
        }
        if (workloadType.getRemark() != null) {
            existing.setRemark(workloadType.getRemark());
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
}
