package com.teacher.service;

import com.teacher.entity.Teacher;

import java.util.List;

public interface TeacherService {
    List<Teacher> getAllTeachers();

    Teacher getTeacherById(Long id);

    Teacher createTeacher(Teacher teacher);

    Teacher updateTeacher(Long id, Teacher teacher);

    void deleteTeacher(Long id);
}
