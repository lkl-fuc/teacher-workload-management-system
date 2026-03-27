package com.teacher.repository;

import com.teacher.entity.WarningRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarningRecordRepository extends JpaRepository<WarningRecord, Long> {
    List<WarningRecord> findByTeacherId(Long teacherId);

    List<WarningRecord> findTop20ByTeacherIdOrderByCreateTimeDesc(Long teacherId);

    List<WarningRecord> findTop200ByOrderByCreateTimeDesc();

    boolean existsByTeacherIdAndRuleIdAndWarningMessageAndStatus(Long teacherId,
                                                                 Long ruleId,
                                                                 String warningMessage,
                                                                 String status);
}
