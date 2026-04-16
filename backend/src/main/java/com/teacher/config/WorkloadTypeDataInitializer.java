package com.teacher.config;

import com.teacher.entity.WorkloadType;
import com.teacher.repository.WorkloadTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Configuration
public class WorkloadTypeDataInitializer {
    private static final Set<String> ALLOWED_CATEGORIES = new LinkedHashSet<>(List.of(
            "专任教师岗",
            "实验教师岗",
            "辅导员岗",
            "教辅岗",
            "行政兼课岗",
            "外聘教师岗"
    ));

    @Bean
    public CommandLineRunner initWorkloadTypes(WorkloadTypeRepository workloadTypeRepository) {
        return args -> {
            workloadTypeRepository.deleteAll(workloadTypeRepository.findByCategoryNameNotIn(ALLOWED_CATEGORIES));
            workloadTypeRepository.deleteAll(
                    workloadTypeRepository.findAll().stream()
                            .filter(type -> ALLOWED_CATEGORIES.contains(type.getCategoryName()))
                            .filter(type -> type.getRemark() == null || !type.getRemark().contains("考核"))
                            .toList()
            );

            List<WorkloadType> defaultTypes = List.of(
                    buildType("专任教师岗", "理论课授课达成", "考核：年度授课学时达标、课堂巡查合格率≥95%、教学事故为0", "9"),
                    buildType("专任教师岗", "课程资源建设", "考核：教案/课件更新完整，课程资源学期更新次数达标", "6"),
                    buildType("专任教师岗", "作业批改与反馈", "考核：批改及时率≥98%，学业预警反馈闭环率≥95%", "5"),
                    buildType("专任教师岗", "教研听评课", "考核：教研参与次数达标，听评课记录齐全并形成改进报告", "6"),

                    buildType("实验教师岗", "实验课程实施", "考核：实验开出率100%，课堂组织规范、无重大差错", "8"),
                    buildType("实验教师岗", "实验项目指导", "考核：实验项目指导覆盖完整，学生满意度≥90%", "7"),
                    buildType("实验教师岗", "实验室安全巡检", "考核：巡检台账完整，隐患整改闭环率100%", "6"),
                    buildType("实验教师岗", "仪器设备台账", "考核：设备完好率≥98%，台账一致性100%", "5"),

                    buildType("辅导员岗", "学生分层管理", "考核：重点学生台账完整，预警处置及时率100%", "7"),
                    buildType("辅导员岗", "思想教育活动", "考核：主题教育活动覆盖率100%，过程材料齐全", "7"),
                    buildType("辅导员岗", "谈心谈话干预", "考核：重点学生谈话与回访记录齐全，闭环率≥95%", "6"),
                    buildType("辅导员岗", "奖助勤贷事务", "考核：事务办理准确率100%，投诉率低于1%", "5"),

                    buildType("教辅岗", "排课排考支持", "考核：排课排考零重大差错，关键节点按时完成率100%", "6"),
                    buildType("教辅岗", "教材资料服务", "考核：教材发放及时准确，资源借用登记完整", "5"),
                    buildType("教辅岗", "教学设备保障", "考核：故障响应及时率≥95%，维修闭环率≥98%", "6"),
                    buildType("教辅岗", "教学档案质检", "考核：教学档案归档完整率100%，抽检合格率≥98%", "5"),

                    buildType("行政兼课岗", "行政任务执行", "考核：专项任务按期完成率≥98%，协同评价良好", "6"),
                    buildType("行政兼课岗", "兼课授课质量", "考核：兼课学时达标，学生评教达标率≥90%", "7"),
                    buildType("行政兼课岗", "流程优化改进", "考核：形成可量化流程优化成果并落地执行", "6"),
                    buildType("行政兼课岗", "教学检查整改", "考核：问题整改闭环率100%，复检通过率≥95%", "5"),

                    buildType("外聘教师岗", "协议课程授课", "考核：按合同完成授课学时，调停课流程合规率100%", "7"),
                    buildType("外聘教师岗", "课程考核阅卷", "考核：命题规范、阅卷及时，成绩提交准时率100%", "6"),
                    buildType("外聘教师岗", "课后答疑支持", "考核：答疑响应及时率≥95%，问题解决率≥90%", "5"),
                    buildType("外聘教师岗", "教学资料归档", "考核：教学资料提交完整率100%，归档符合规范", "5")
            );

            for (WorkloadType type : defaultTypes) {
                if (!workloadTypeRepository.existsByTypeName(type.getTypeName())) {
                    workloadTypeRepository.save(type);
                }
            }
        };
    }

    private WorkloadType buildType(String category, String subType, String remark, String unitValue) {
        WorkloadType workloadType = new WorkloadType();
        workloadType.setCategoryName(category);
        workloadType.setSubTypeName(subType);
        workloadType.setTypeName(category + " - " + subType);
        workloadType.setRemark(remark);
        workloadType.setUnitValue(new BigDecimal(unitValue));
        return workloadType;
    }
}
