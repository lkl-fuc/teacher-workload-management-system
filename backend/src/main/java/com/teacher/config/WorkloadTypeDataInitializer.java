package com.teacher.config;

import com.teacher.entity.WorkloadType;
import com.teacher.repository.WorkloadTypeRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.util.List;

@Configuration
public class WorkloadTypeDataInitializer {

    @Bean
    public CommandLineRunner initWorkloadTypes(WorkloadTypeRepository workloadTypeRepository) {
        return args -> {
            List<WorkloadType> defaultTypes = List.of(
                    buildType("专任教师岗", "课堂授课", "专任教师每周授课工作量", "8"),
                    buildType("专任教师岗", "教学准备", "教案、备课与课程资源建设", "4"),
                    buildType("专任教师岗", "教研活动", "教研会议、听评课与教学改进", "5"),

                    buildType("实验教师岗", "实验教学", "实验课程教学与课堂组织", "8"),
                    buildType("实验教师岗", "实验室指导", "实验项目指导与答疑", "6"),
                    buildType("实验教师岗", "实验室安全维护", "实验室安全巡检与设备管理", "5"),

                    buildType("辅导员岗", "学生管理", "学生日常管理与数据维护", "6"),
                    buildType("辅导员岗", "思想教育", "主题班会、思政教育与谈心谈话", "7"),
                    buildType("辅导员岗", "日常事务", "请销假、奖助勤贷与突发事务处理", "5"),

                    buildType("教辅岗", "图书资源服务", "图书借阅管理、教材发放支持", "5"),
                    buildType("教辅岗", "设备保障服务", "教学设备巡检、报修与保障", "6"),
                    buildType("教辅岗", "教学秘书支持", "教学秘书排课、考试组织与资料归档", "5"),

                    buildType("行政兼课岗", "行政管理事务", "行政管理、流程审批与协调工作", "6"),
                    buildType("行政兼课岗", "兼课教学任务", "行政人员承担课程授课任务", "7"),
                    buildType("行政兼课岗", "行政教学协同", "教学检查、质量反馈与改进", "5"),

                    buildType("外聘教师岗", "外聘授课", "外聘教师课程授课任务", "7"),
                    buildType("外聘教师岗", "课程考核", "作业批改、考试命题与阅卷", "5"),
                    buildType("外聘教师岗", "答疑辅导", "课后答疑与学习辅导", "4")
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
