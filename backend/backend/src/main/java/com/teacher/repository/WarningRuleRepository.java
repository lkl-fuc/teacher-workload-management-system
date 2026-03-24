package com.teacher.repository;

import com.teacher.entity.WarningRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarningRuleRepository extends JpaRepository<WarningRule, Long> {
    List<WarningRule> findByEnabled(Integer enabled);
}