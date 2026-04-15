package com.teacher.config;

import com.teacher.entity.Teacher;
import com.teacher.repository.TeacherRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Configuration
public class TeacherDataInitializer {

    @Bean
    public CommandLineRunner initTeachers(TeacherRepository teacherRepository) {
        return args -> {
            List<Teacher> defaultTeachers = List.of(
                    buildTeacher("T001", "张三", "男", "13800138000", "zhangsan@example.com", "计算机学院", "讲师", "专任教师岗"),
                    buildTeacher("T002", "李四", "女", "13800138001", "lisi@example.com", "计算机学院", "副教授", "实验教师岗"),
                    buildTeacher("T003", "王五", "男", "13800138002", "wangwu@example.com", "软件学院", "讲师", "辅导员岗"),
                    buildTeacher("T004", "赵六", "女", "13800138003", "zhaoliu@example.com", "人工智能学院", "副教授", "教辅岗"),
                    buildTeacher("T005", "孙七", "男", "13800138004", "sunqi@example.com", "数据科学学院", "教授", "行政兼课岗"),
                    buildTeacher("T006", "周敏", "女", "13800138005", "zhoumin@example.com", "计算机学院", "讲师", "外聘教师岗"),
                    buildTeacher("T007", "钱涛", "男", "13800138006", "qiantao@example.com", "软件学院", "副教授", "专任教师岗"),
                    buildTeacher("T008", "吴婷", "女", "13800138007", "wuting@example.com", "人工智能学院", "讲师", "实验教师岗"),
                    buildTeacher("T009", "郑凯", "男", "13800138008", "zhengkai@example.com", "数据科学学院", "讲师", "教辅岗")
            );

            for (Teacher teacher : defaultTeachers) {
                Optional<Teacher> existingTeacher = teacherRepository.findByTeacherNo(teacher.getTeacherNo());
                if (existingTeacher.isEmpty()) {
                    teacherRepository.save(teacher);
                    continue;
                }

                Teacher existing = existingTeacher.get();
                boolean shouldUpdate = false;
                if (!teacher.getName().equals(existing.getName())) {
                    existing.setName(teacher.getName());
                    shouldUpdate = true;
                }
                if (!teacher.getPostType().equals(existing.getPostType())) {
                    existing.setPostType(teacher.getPostType());
                    shouldUpdate = true;
                }
                if (shouldUpdate) {
                    teacherRepository.save(existing);
                }
            }

            List<Teacher> allTeachers = teacherRepository.findAll();
            boolean normalized = false;
            for (Teacher teacher : allTeachers) {
                if (teacher.getName() != null && teacher.getName().contains("张三（已修改）")) {
                    teacher.setName(teacher.getName().replace("张三（已修改）", "张三"));
                    teacherRepository.save(teacher);
                    normalized = true;
                }
            }
            if (normalized) {
                allTeachers = teacherRepository.findAll();
            }

            Map<String, Teacher> teacherByPost = allTeachers.stream()
                    .filter(item -> item.getPostType() != null && !item.getPostType().isBlank())
                    .collect(Collectors.toMap(
                            Teacher::getPostType,
                            Function.identity(),
                            (first, second) -> first
                    ));

            Set<String> existingTeacherNos = allTeachers.stream()
                    .map(Teacher::getTeacherNo)
                    .collect(Collectors.toSet());

            for (Teacher teacher : defaultTeachers) {
                if (teacherByPost.containsKey(teacher.getPostType())) {
                    continue;
                }
                if (existingTeacherNos.contains(teacher.getTeacherNo())) {
                    continue;
                }
                teacherRepository.save(teacher);
                existingTeacherNos.add(teacher.getTeacherNo());
            }
        };
    }

    private Teacher buildTeacher(String teacherNo,
                                 String name,
                                 String gender,
                                 String phone,
                                 String email,
                                 String department,
                                 String title,
                                 String postType) {
        Teacher teacher = new Teacher();
        teacher.setTeacherNo(teacherNo);
        teacher.setPassword("123456");
        teacher.setName(name);
        teacher.setGender(gender);
        teacher.setPhone(phone);
        teacher.setEmail(email);
        teacher.setDepartment(department);
        teacher.setTitle(title);
        teacher.setPostType(postType);
        teacher.setStatus(1);
        return teacher;
    }
}
