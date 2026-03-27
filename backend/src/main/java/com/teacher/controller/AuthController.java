package com.teacher.controller;

import com.teacher.dto.LoginRequest;
import com.teacher.dto.LoginResponse;
import com.teacher.entity.Admin;
import com.teacher.entity.Teacher;
import com.teacher.repository.AdminRepository;
import com.teacher.repository.TeacherRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private TeacherRepository teacherRepository;

    @PostMapping("/login")
    public Object login(@RequestBody LoginRequest request) {
        if ("ADMIN".equalsIgnoreCase(request.getRole())) {
            Optional<Admin> optionalAdmin = adminRepository.findByUsername(request.getUsername());
            if (optionalAdmin.isPresent()) {
                Admin admin = optionalAdmin.get();
                if (admin.getPassword().equals(request.getPassword())) {
                    LoginResponse response = new LoginResponse();
                    response.setId(admin.getId());
                    response.setUsername(admin.getUsername());
                    response.setName(admin.getName());
                    response.setRole("ADMIN");
                    return response;
                }
            }
            return "管理员用户名或密码错误";
        }

        if ("TEACHER".equalsIgnoreCase(request.getRole())) {
            Optional<Teacher> optionalTeacher = teacherRepository.findByTeacherNo(request.getUsername());
            if (optionalTeacher.isPresent()) {
                Teacher teacher = optionalTeacher.get();
                if (teacher.getPassword().equals(request.getPassword())) {
                    LoginResponse response = new LoginResponse();
                    response.setId(teacher.getId());
                    response.setUsername(teacher.getTeacherNo());
                    response.setName(teacher.getName());
                    response.setRole("TEACHER");
                    response.setTeacherPost(
                            teacher.getPostType() == null || teacher.getPostType().isBlank()
                                    ? "行政岗"
                                    : teacher.getPostType()
                    );
                    return response;
                }
            }
            return "教师工号或密码错误";
        }

        return "角色参数错误";
    }
}
