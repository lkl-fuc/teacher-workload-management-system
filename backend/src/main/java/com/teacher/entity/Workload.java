package com.teacher.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "workload")
public class Workload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "teacher_id")
    private Long teacherId;

    @Column(name = "type_id")
    private Long typeId;

    @Column(name = "workload_title")
    private String workloadTitle;

    private String description;

    private BigDecimal amount;

    @Column(name = "submit_date")
    private LocalDate submitDate;

    private String status;

    @Column(name = "reject_reason")
    private String rejectReason;

    @Column(name = "source_type")
    private String sourceType;

    @Column(name = "create_time", insertable = false, updatable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", insertable = false, updatable = false)
    private LocalDateTime updateTime;
}
