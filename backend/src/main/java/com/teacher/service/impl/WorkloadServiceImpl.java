package com.teacher.service.impl;

import com.teacher.entity.Workload;
import com.teacher.entity.WorkloadType;
import com.teacher.repository.TeacherRepository;
import com.teacher.repository.WorkloadRepository;
import com.teacher.repository.WorkloadTypeRepository;
import com.teacher.service.WorkloadService;
import com.teacher.service.WorkloadWarningService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.List;

@Service
public class WorkloadServiceImpl implements WorkloadService {

    private final WorkloadRepository workloadRepository;
    private final TeacherRepository teacherRepository;
    private final WorkloadTypeRepository workloadTypeRepository;
    private final WorkloadWarningService workloadWarningService;

    public WorkloadServiceImpl(WorkloadRepository workloadRepository,
                               TeacherRepository teacherRepository,
                               WorkloadTypeRepository workloadTypeRepository,
                               WorkloadWarningService workloadWarningService) {
        this.workloadRepository = workloadRepository;
        this.teacherRepository = teacherRepository;
        this.workloadTypeRepository = workloadTypeRepository;
        this.workloadWarningService = workloadWarningService;
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
        validateRelation(workload.getTeacherId(), workload.getTypeId(), true);

        workload.setId(null);
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

        validateRelation(teacherIdToCheck, typeIdToCheck, false);

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

        Workload saved = workloadRepository.save(existing);
        String status = existing.getStatus() == null ? "" : existing.getStatus().toUpperCase();
        if ("APPROVED".equals(status) || "REJECTED".equals(status)) {
            workloadWarningService.analyzeTeacherWorkloads(true);
        }
        return saved;
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

    private void validateRelation(Long teacherId, Long typeId, boolean strictPostMatch) {
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

        if (strictPostMatch) {
            String postType = teacherRepository.findById(teacherId)
                    .map(teacher -> teacher.getPostType())
                    .orElse("");
            WorkloadType type = workloadTypeRepository.findById(typeId).orElse(null);
            if (!matchesPostType(postType, type)) {
                throw new IllegalArgumentException("任务类型与教师岗位不匹配，请选择对应岗位任务");
            }
        }
    }

    private boolean matchesPostType(String postType, WorkloadType type) {
        String post = normalize(postType);
        if (post.isBlank() || type == null) return true;
        String keywords = String.join(" ",
                normalize(type.getTypeName()),
                normalize(type.getCategoryName()),
                normalize(type.getSubTypeName()),
                normalize(type.getRemark())
        );

        if (post.contains("专任教师")) {
            return containsAny(keywords, "专任", "教学", "授课", "课程", "教研");
        }
        if (post.contains("实验教师")) {
            return containsAny(keywords, "实验", "实验室", "指导", "实训", "教学");
        }
        if (post.contains("辅导员")) {
            return containsAny(keywords, "辅导员", "学生", "思政", "教育", "班会", "事务");
        }
        if (post.contains("教辅")) {
            return containsAny(keywords, "教辅", "图书", "设备", "秘书", "保障", "服务");
        }
        if (post.contains("行政兼课")) {
            return containsAny(keywords, "行政", "兼课", "教学", "授课", "管理");
        }
        if (post.contains("外聘教师")) {
            return containsAny(keywords, "外聘", "授课", "课程", "答疑", "教学");
        }
        if (post.contains("行政")) {
            return containsAny(keywords, "管理", "行政", "服务", "支撑");
        }
        if (post.contains("管理")) {
            return containsAny(keywords, "管理", "审核", "质量", "规划");
        }
        return true;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(normalize(keyword))) return true;
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) return "";
        return value.toLowerCase(Locale.ROOT).replace(" ", "");
    }
}
