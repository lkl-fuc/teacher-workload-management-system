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
                    buildType("论文", "SCI", "高质量国际检索论文", "25"),
                    buildType("论文", "EI", "工程索引收录论文", "20"),
                    buildType("论文", "CSSCI", "CSSCI来源期刊论文", "16"),
                    buildType("论文", "普通期刊", "一般中文期刊论文", "10"),
                    buildType("论文", "会议论文", "学术会议论文", "8"),

                    buildType("项目", "国家级", "国家级纵向项目", "30"),
                    buildType("项目", "省部级", "省部级项目", "20"),
                    buildType("项目", "厅局级", "厅局级项目", "12"),
                    buildType("项目", "校级", "校级教改或科研项目", "8"),

                    buildType("专利", "发明专利", "已授权发明专利", "24"),
                    buildType("专利", "实用新型专利", "已授权实用新型专利", "12"),
                    buildType("专利", "外观设计专利", "已授权外观设计专利", "8"),
                    buildType("专利", "国际专利", "PCT或境外授权专利", "30"),
                    buildType("专利", "软件著作权", "软件著作权登记", "10"),

                    buildType("竞赛", "教学能力竞赛", "教师教学能力类竞赛", "12"),
                    buildType("竞赛", "说课比赛", "说课类竞赛", "8"),
                    buildType("竞赛", "微课比赛", "微课制作比赛", "8"),
                    buildType("竞赛", "青教赛", "青年教师教学竞赛", "12"),

                    buildType("教学", "理论教学", "理论课课堂教学", "6"),
                    buildType("教学", "实验教学", "实验课教学", "7"),
                    buildType("教学", "实训教学", "实训课程教学", "8"),
                    buildType("教学", "课程设计", "课程设计指导", "9"),
                    buildType("教学", "毕业设计（论文）指导", "毕业设计或毕业论文指导", "12"),

                    buildType("行政事务", "流程执行", "行政岗流程执行类任务", "5"),
                    buildType("行政事务", "数据上报", "行政岗统计上报类任务", "4"),
                    buildType("管理工作", "审核监督", "管理岗审核监督类任务", "8"),
                    buildType("管理工作", "战略规划", "管理岗统筹规划类任务", "9"),
                    buildType("教辅服务", "教学保障", "教辅岗教学保障类任务", "6"),
                    buildType("教辅服务", "实训支持", "教辅岗实训支持类任务", "7")
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
