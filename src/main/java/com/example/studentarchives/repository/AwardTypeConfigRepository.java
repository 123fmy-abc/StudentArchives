package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.AwardTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 奖项类型配置 Repository
 * <p>
 * 评选说明自 V57 起按学校（school_id）独立配置。
 */
@Repository
public interface AwardTypeConfigRepository extends JpaRepository<AwardTypeConfig, Long> {

    /**
     * 查询指定学校下全部奖项类型配置（含禁用），按 sort 正序，供管理端配置列表使用
     */
    List<AwardTypeConfig> findBySchoolIdOrderBySortAsc(Long schoolId);

    /**
     * 按学校 + 奖项类型编码查询配置（含禁用/启用），供管理端保存时复用已有行
     */
    Optional<AwardTypeConfig> findBySchoolIdAndAwardType(Long schoolId, String awardType);

    /**
     * 按学校 + 奖项类型编码查询已启用的配置（学生端评选说明）
     */
    Optional<AwardTypeConfig> findBySchoolIdAndAwardTypeAndStatus(Long schoolId, String awardType, Integer status);

    /**
     * 取该校排序值最大的配置，用于新增时计算 sort
     */
    Optional<AwardTypeConfig> findTopBySchoolIdOrderBySortDesc(Long schoolId);
}
