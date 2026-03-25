package com.teacher.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "warning_record")
public class WarningRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "rule_id")
    private Long ruleId;

    @Column(name = "warning_message")
    private String warningMessage;

    private String status;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;
}