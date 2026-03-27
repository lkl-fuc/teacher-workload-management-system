package com.teacher.service.impl;

import com.teacher.entity.Teacher;
import com.teacher.repository.TeacherRepository;
import com.teacher.service.TeacherService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeacherServiceImpl implements TeacherService {

    private final TeacherRepository teacherRepository;

    public TeacherServiceImpl(TeacherRepository teacherRepository) {
        this.teacherRepository = teacherRepository;
    }

    @Override
    public List<Teacher> getAllTeachers() {
        return teacherRepository.findAll();
    }

    @Override
    public Teacher getTeacherById(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("教师不存在，id=" + id));
    }

    @Override
    @Transactional
    public Teacher createTeacher(Teacher teacher) {
        if (teacher.getTeacherNo() == null || teacher.getTeacherNo().isBlank()) {
            throw new IllegalArgumentException("教师工号不能为空");
        }
        if (teacherRepository.existsByTeacherNo(teacher.getTeacherNo())) {
            throw new IllegalArgumentException("教师工号已存在：" + teacher.getTeacherNo());
        }
        teacher.setId(null);
        return teacherRepository.save(teacher);
    }

    @Override
    @Transactional
    public Teacher updateTeacher(Long id, Teacher teacher) {
        Teacher existing = teacherRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("教师不存在，id=" + id));

        if (teacher.getTeacherNo() != null && !teacher.getTeacherNo().isBlank()) {
            teacherRepository.findByTeacherNoAndIdNot(teacher.getTeacherNo(), id)
                    .ifPresent(t -> {
                        throw new IllegalArgumentException("教师工号已存在：" + teacher.getTeacherNo());
                    });
            existing.setTeacherNo(teacher.getTeacherNo());
        }

        if (teacher.getPassword() != null) {
            existing.setPassword(teacher.getPassword());
        }
        if (teacher.getName() != null) {
            existing.setName(teacher.getName());
        }
        if (teacher.getGender() != null) {
            existing.setGender(teacher.getGender());
        }
        if (teacher.getPhone() != null) {
            existing.setPhone(teacher.getPhone());
        }
        if (teacher.getEmail() != null) {
            existing.setEmail(teacher.getEmail());
        }
        if (teacher.getDepartment() != null) {
            existing.setDepartment(teacher.getDepartment());
        }
        if (teacher.getTitle() != null) {
            existing.setTitle(teacher.getTitle());
        }
        if (teacher.getPostType() != null) {
            existing.setPostType(teacher.getPostType());
        }
        if (teacher.getStatus() != null) {
            existing.setStatus(teacher.getStatus());
        }

        return teacherRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteTeacher(Long id) {
        if (!teacherRepository.existsById(id)) {
            throw new IllegalArgumentException("教师不存在，id=" + id);
        }
        teacherRepository.deleteById(id);
    }
}
