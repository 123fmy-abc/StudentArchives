package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.DuplicateDetectionRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 重复检测规则 Repository
 */
@Repository
public interface DuplicateDetectionRuleRepository extends JpaRepository<DuplicateDetectionRule, Long> {

    /**
     * 按学校 + 档案类型查询已启用的检测规则
     */
    Optional<DuplicateDetectionRule> findFirstBySchoolIdAndArchiveTypeAndStatus(Long schoolId, String archiveType, Integer status);
}
