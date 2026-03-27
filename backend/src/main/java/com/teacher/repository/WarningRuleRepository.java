package com.teacher.repository;

import com.teacher.entity.WarningRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarningRuleRepository extends JpaRepository<WarningRule, Long> {
    List<WarningRule> findByEnabled(Integer enabled);

    Optional<WarningRule> findByRuleName(String ruleName);
}
