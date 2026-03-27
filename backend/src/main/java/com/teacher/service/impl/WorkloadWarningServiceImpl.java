package com.teacher.service.impl;

import com.teacher.dto.TeacherWorkloadAnalysisItem;
import com.teacher.dto.WorkloadAnalysisResponse;
import com.teacher.dto.WorkloadAnalysisSummary;
import com.teacher.entity.Teacher;
import com.teacher.entity.WarningRecord;
import com.teacher.entity.WarningRule;
import com.teacher.entity.Workload;
import com.teacher.entity.WorkloadType;
import com.teacher.repository.TeacherRepository;
import com.teacher.repository.WarningRecordRepository;
import com.teacher.repository.WarningRuleRepository;
import com.teacher.repository.WorkloadRepository;
import com.teacher.repository.WorkloadTypeRepository;
import com.teacher.service.WorkloadWarningService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class WorkloadWarningServiceImpl implements WorkloadWarningService {

    private static final BigDecimal ONE = BigDecimal.ONE;

    private final TeacherRepository teacherRepository;
    private final WorkloadRepository workloadRepository;
    private final WorkloadTypeRepository workloadTypeRepository;
    private final WarningRuleRepository warningRuleRepository;
    private final WarningRecordRepository warningRecordRepository;

    public WorkloadWarningServiceImpl(TeacherRepository teacherRepository,
                                      WorkloadRepository workloadRepository,
                                      WorkloadTypeRepository workloadTypeRepository,
                                      WarningRuleRepository warningRuleRepository,
                                      WarningRecordRepository warningRecordRepository) {
        this.teacherRepository = teacherRepository;
        this.workloadRepository = workloadRepository;
        this.workloadTypeRepository = workloadTypeRepository;
        this.warningRuleRepository = warningRuleRepository;
        this.warningRecordRepository = warningRecordRepository;
    }

    @Override
    @Transactional
    public WorkloadAnalysisResponse analyzeTeacherWorkloads(boolean persistWarnings) {
        List<Teacher> teachers = teacherRepository.findAll();
        Map<Long, Teacher> teacherMap = new HashMap<>();
        teachers.forEach(teacher -> teacherMap.put(teacher.getId(), teacher));
        List<Workload> approvedWorkloads = workloadRepository.findByStatusIgnoreCase("APPROVED");
        Map<Long, WorkloadType> typeMap = new HashMap<>();
        workloadTypeRepository.findAll().forEach(type -> typeMap.put(type.getId(), type));

        Map<Long, BigDecimal> totalByTeacher = new HashMap<>();
        Map<Long, BigDecimal> equivalentByTeacher = new HashMap<>();

        for (Workload workload : approvedWorkloads) {
            Long teacherId = workload.getTeacherId();
            if (teacherId == null) {
                continue;
            }
            BigDecimal amount = workload.getAmount() == null ? BigDecimal.ZERO : workload.getAmount();
            totalByTeacher.merge(teacherId, amount, BigDecimal::add);

            Teacher teacher = teacherMap.get(teacherId);
            WorkloadType type = typeMap.get(workload.getTypeId());
            BigDecimal equivalent = amount.multiply(resolveTypeWeight(type))
                    .multiply(resolveTitleWeight(teacher == null ? null : teacher.getTitle()))
                    .multiply(resolvePostWeight(teacher == null ? null : teacher.getPostType()))
                    .multiply(resolveRoleWeight(type));

            equivalentByTeacher.merge(teacherId, equivalent, BigDecimal::add);
        }

        List<TeacherWorkloadAnalysisItem> items = new ArrayList<>();
        WorkloadAnalysisSummary summary = new WorkloadAnalysisSummary();

        for (Teacher teacher : teachers) {
            BigDecimal total = totalByTeacher.getOrDefault(teacher.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
            BigDecimal equivalent = equivalentByTeacher.getOrDefault(teacher.getId(), BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

            WarningRule thresholdRule = resolveThresholdRule(teacher.getPostType());
            BigDecimal min = thresholdRule.getMinValue() == null ? BigDecimal.ZERO : thresholdRule.getMinValue();
            BigDecimal max = thresholdRule.getMaxValue() == null ? new BigDecimal("999999") : thresholdRule.getMaxValue();

            String level = "NORMAL";
            String message = "工作量处于合理区间";

            if (equivalent.compareTo(min) < 0) {
                level = "LOW";
                message = String.format("预警：当前折算工作量 %.2f 低于阈值 %.2f，请关注任务分配", equivalent, min);
                summary.setLowCount(summary.getLowCount() + 1);
            } else if (equivalent.compareTo(max) > 0) {
                level = "HIGH";
                message = String.format("预警：当前折算工作量 %.2f 高于阈值 %.2f，请关注负荷均衡", equivalent, max);
                summary.setHighCount(summary.getHighCount() + 1);
            } else {
                summary.setNormalCount(summary.getNormalCount() + 1);
            }

            if (persistWarnings && !"NORMAL".equals(level)) {
                saveWarningIfNeeded(teacher.getId(), thresholdRule.getId(), message);
            }

            TeacherWorkloadAnalysisItem item = new TeacherWorkloadAnalysisItem();
            item.setTeacherId(teacher.getId());
            item.setTeacherName(teacher.getName());
            item.setPostType(emptyAsDefault(teacher.getPostType(), "未设置岗位"));
            item.setTitle(emptyAsDefault(teacher.getTitle(), "未设置职称"));
            item.setTotalAmount(total);
            item.setEquivalentAmount(equivalent);
            item.setMinThreshold(min);
            item.setMaxThreshold(max);
            item.setLevel(level);
            item.setWarningMessage(message);
            items.add(item);
        }

        summary.setTeacherCount(teachers.size());

        WorkloadAnalysisResponse response = new WorkloadAnalysisResponse();
        response.setSummary(summary);
        response.setItems(items);
        return response;
    }

    @Override
    public List<WarningRecord> getTeacherWarnings(Long teacherId) {
        if (!teacherRepository.existsById(teacherId)) {
            throw new IllegalArgumentException("教师不存在，id=" + teacherId);
        }
        return warningRecordRepository.findTop20ByTeacherIdOrderByCreateTimeDesc(teacherId);
    }

    private WarningRule resolveThresholdRule(String postType) {
        List<WarningRule> rules = warningRuleRepository.findByEnabled(1);
        String target = emptyAsDefault(postType, "默认").toLowerCase(Locale.ROOT);

        Optional<WarningRule> matched = rules.stream()
                .filter(rule -> rule.getRuleName() != null)
                .filter(rule -> rule.getRuleName().toLowerCase(Locale.ROOT).contains(target))
                .findFirst();

        if (matched.isPresent()) {
            return matched.get();
        }

        BigDecimal min = new BigDecimal("80");
        BigDecimal max = new BigDecimal("200");
        String normalized = target.replace(" ", "");
        if (normalized.contains("教学")) {
            min = new BigDecimal("120");
            max = new BigDecimal("260");
        } else if (normalized.contains("科研")) {
            min = new BigDecimal("100");
            max = new BigDecimal("220");
        } else if (normalized.contains("行政")) {
            min = new BigDecimal("60");
            max = new BigDecimal("160");
        }

        String ruleName = "默认阈值-" + emptyAsDefault(postType, "通用");
        WarningRule existing = warningRuleRepository.findByRuleName(ruleName).orElse(null);
        if (existing != null) {
            return existing;
        }

        WarningRule rule = new WarningRule();
        rule.setRuleName(ruleName);
        rule.setMinValue(min);
        rule.setMaxValue(max);
        rule.setEnabled(1);
        rule.setTypeId(null);
        rule.setRemark("系统自动创建的岗位阈值规则");
        return warningRuleRepository.save(rule);
    }

    private void saveWarningIfNeeded(Long teacherId, Long ruleId, String message) {
        boolean exists = warningRecordRepository.existsByTeacherIdAndRuleIdAndWarningMessageAndStatus(
                teacherId,
                ruleId,
                message,
                "NEW"
        );
        if (exists) {
            return;
        }

        WarningRecord record = new WarningRecord();
        record.setTeacherId(teacherId);
        record.setRuleId(ruleId);
        record.setWarningMessage(message);
        record.setStatus("NEW");
        warningRecordRepository.save(record);
    }

    private BigDecimal resolveTypeWeight(WorkloadType type) {
        String text = "";
        if (type != null) {
            text = String.join("-",
                    emptyAsDefault(type.getTypeName(), ""),
                    emptyAsDefault(type.getCategoryName(), ""),
                    emptyAsDefault(type.getSubTypeName(), "")
            ).toLowerCase(Locale.ROOT);
        }
        if (text.contains("教学") || text.contains("课程")) return new BigDecimal("1.20");
        if (text.contains("科研") || text.contains("论文")) return new BigDecimal("1.10");
        if (text.contains("管理") || text.contains("行政")) return new BigDecimal("0.90");
        return ONE;
    }

    private BigDecimal resolveTitleWeight(String title) {
        String value = emptyAsDefault(title, "").toLowerCase(Locale.ROOT);
        if (value.contains("教授")) return new BigDecimal("1.20");
        if (value.contains("副教授")) return new BigDecimal("1.10");
        if (value.contains("讲师")) return new BigDecimal("1.00");
        if (value.contains("助教")) return new BigDecimal("0.90");
        return ONE;
    }

    private BigDecimal resolvePostWeight(String postType) {
        String value = emptyAsDefault(postType, "").toLowerCase(Locale.ROOT);
        if (value.contains("教学")) return new BigDecimal("1.15");
        if (value.contains("科研")) return new BigDecimal("1.10");
        if (value.contains("行政")) return new BigDecimal("0.95");
        return ONE;
    }

    private BigDecimal resolveRoleWeight(WorkloadType type) {
        String roleText = type == null ? "" : emptyAsDefault(type.getSubTypeName(), "").toLowerCase(Locale.ROOT);
        if (roleText.contains("主讲") || roleText.contains("负责人")) return new BigDecimal("1.20");
        if (roleText.contains("辅讲") || roleText.contains("协同")) return new BigDecimal("1.00");
        if (roleText.contains("指导") || roleText.contains("助理")) return new BigDecimal("0.90");
        return ONE;
    }

    private String emptyAsDefault(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
