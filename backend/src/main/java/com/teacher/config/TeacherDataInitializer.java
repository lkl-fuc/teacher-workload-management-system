package com.teacher.config;

import com.teacher.entity.Teacher;
import com.teacher.repository.TeacherRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Optional;

@Configuration
public class TeacherDataInitializer {

    @Bean
    public CommandLineRunner initTeachers(TeacherRepository teacherRepository) {
        return args -> {
            List<Teacher> defaultTeachers = List.of(
                    buildTeacher("T001", "张三", "男", "13800138000", "zhangsan@example.com", "计算机学院", "讲师", "行政岗"),
                    buildTeacher("T002", "李四", "女", "13800138001", "lisi@example.com", "计算机学院", "副教授", "管理岗"),
                    buildTeacher("T003", "王五", "男", "13800138002", "wangwu@example.com", "软件学院", "讲师", "教辅岗"),
                    buildTeacher("T004", "赵六", "女", "13800138003", "zhaoliu@example.com", "人工智能学院", "副教授", "管理岗"),
                    buildTeacher("T005", "孙七", "男", "13800138004", "sunqi@example.com", "数据科学学院", "教授", "行政岗")
            );

            for (Teacher teacher : defaultTeachers) {
                Optional<Teacher> existingTeacher = teacherRepository.findByTeacherNo(teacher.getTeacherNo());
                if (existingTeacher.isEmpty()) {
                    teacherRepository.save(teacher);
                    continue;
                }

                Teacher existing = existingTeacher.get();
                if (existing.getPostType() == null || existing.getPostType().isBlank()) {
                    existing.setPostType(teacher.getPostType());
                    teacherRepository.save(existing);
                }
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
