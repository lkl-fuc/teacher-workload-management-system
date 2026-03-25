package com.teacher.repository;

import com.teacher.entity.Teacher;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TeacherRepository extends JpaRepository<Teacher, Long> {
    Optional<Teacher> findByTeacherNo(String teacherNo);

    Optional<Teacher> findByTeacherNoAndIdNot(String teacherNo, Long id);

    boolean existsByTeacherNo(String teacherNo);
}
