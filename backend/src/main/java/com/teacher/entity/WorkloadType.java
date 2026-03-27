package com.teacher.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "workload_type")
public class WorkloadType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_name")
    private String typeName;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "sub_type_name")
    private String subTypeName;

    @Column(name = "unit_value")
    private BigDecimal unitValue;

    private String remark;
}
