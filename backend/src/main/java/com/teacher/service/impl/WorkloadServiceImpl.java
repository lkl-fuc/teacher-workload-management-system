package com.teacher.service.impl;

import com.teacher.entity.Workload;
import com.teacher.repository.TeacherRepository;
import com.teacher.repository.WorkloadRepository;
import com.teacher.repository.WorkloadTypeRepository;
import com.teacher.service.WorkloadService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class WorkloadServiceImpl implements WorkloadService {

    private static final String STATUS_PENDING = "待审核";
    private static final String STATUS_APPROVED = "已通过";
    private static final String STATUS_REJECTED = "已驳回";

    private final WorkloadRepository workloadRepository;
    private final TeacherRepository teacherRepository;
    private final WorkloadTypeRepository workloadTypeRepository;

    public WorkloadServiceImpl(WorkloadRepository workloadRepository,
                               TeacherRepository teacherRepository,
                               WorkloadTypeRepository workloadTypeRepository) {
        this.workloadRepository = workloadRepository;
        this.teacherRepository = teacherRepository;
        this.workloadTypeRepository = workloadTypeRepository;
    }

    @Override
    public List<Workload> getAllWorkloads() {
        return workloadRepository.findAll();
    }

    @Override
    public Workload getWorkloadById(Long id) {
        return workloadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工作量记录不存在，id=" + id));
    }

    @Override
    @Transactional
    public Workload createWorkload(Workload workload) {
        validateRequiredFields(workload);
        validateRelation(workload.getTeacherId(), workload.getTypeId());

        workload.setId(null);
        if (workload.getStatus() == null || workload.getStatus().isBlank()) {
            workload.setStatus(STATUS_PENDING);
        }
        workload.setRejectReason(null);
        return workloadRepository.save(workload);
    }

    @Override
    @Transactional
    public Workload updateWorkload(Long id, Workload workload) {
        Workload existing = workloadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工作量记录不存在，id=" + id));

        Long teacherIdToCheck = existing.getTeacherId();
        Long typeIdToCheck = existing.getTypeId();

        if (workload.getTeacherId() != null) {
            teacherIdToCheck = workload.getTeacherId();
            existing.setTeacherId(workload.getTeacherId());
        }
        if (workload.getTypeId() != null) {
            typeIdToCheck = workload.getTypeId();
            existing.setTypeId(workload.getTypeId());
        }

        validateRelation(teacherIdToCheck, typeIdToCheck);

        if (workload.getWorkloadTitle() != null) {
            existing.setWorkloadTitle(workload.getWorkloadTitle());
        }
        if (workload.getDescription() != null) {
            existing.setDescription(workload.getDescription());
        }
        if (workload.getAmount() != null) {
            existing.setAmount(workload.getAmount());
        }
        if (workload.getSubmitDate() != null) {
            existing.setSubmitDate(workload.getSubmitDate());
        }
        if (workload.getStatus() != null) {
            existing.setStatus(workload.getStatus());
        }
        if (workload.getRejectReason() != null) {
            existing.setRejectReason(workload.getRejectReason());
        }

        return workloadRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteWorkload(Long id) {
        if (!workloadRepository.existsById(id)) {
            throw new IllegalArgumentException("工作量记录不存在，id=" + id);
        }
        workloadRepository.deleteById(id);
    }

    @Override
    public List<Workload> getWorkloadsByTeacherId(Long teacherId) {
        if (!teacherRepository.existsById(teacherId)) {
            throw new IllegalArgumentException("教师不存在，id=" + teacherId);
        }
        return workloadRepository.findByTeacherId(teacherId);
    }

    @Override
    @Transactional
    public Workload approveWorkload(Long id) {
        Workload workload = workloadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工作量记录不存在，id=" + id));
        workload.setStatus(STATUS_APPROVED);
        workload.setRejectReason(null);
        return workloadRepository.save(workload);
    }

    @Override
    @Transactional
    public Workload rejectWorkload(Long id, String rejectReason) {
        if (rejectReason == null || rejectReason.isBlank()) {
            throw new IllegalArgumentException("驳回原因不能为空");
        }

        Workload workload = workloadRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工作量记录不存在，id=" + id));

        workload.setStatus(STATUS_REJECTED);
        workload.setRejectReason(rejectReason);
        return workloadRepository.save(workload);
    }

    private void validateRequiredFields(Workload workload) {
        if (workload.getTeacherId() == null) {
            throw new IllegalArgumentException("teacherId 不能为空");
        }
        if (workload.getTypeId() == null) {
            throw new IllegalArgumentException("typeId 不能为空");
        }
        if (workload.getWorkloadTitle() == null || workload.getWorkloadTitle().isBlank()) {
            throw new IllegalArgumentException("工作量标题不能为空");
        }
    }

    private void validateRelation(Long teacherId, Long typeId) {
        if (teacherId == null) {
            throw new IllegalArgumentException("teacherId 不能为空");
        }
        if (typeId == null) {
            throw new IllegalArgumentException("typeId 不能为空");
        }
        if (!teacherRepository.existsById(teacherId)) {
            throw new IllegalArgumentException("教师不存在，id=" + teacherId);
        }
        if (!workloadTypeRepository.existsById(typeId)) {
            throw new IllegalArgumentException("工作量类型不存在，id=" + typeId);
        }
    }
}
