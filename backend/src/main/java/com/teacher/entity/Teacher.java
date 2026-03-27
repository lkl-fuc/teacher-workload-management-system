package com.teacher.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "teacher")
public class Teacher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_no")
    private String teacherNo;

    private String password;

    private String name;

    private String gender;

    private String phone;

    private String email;

    private String department;

    private String title;

    @Column(name = "post_type")
    private String postType;

    private Integer status;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
}
