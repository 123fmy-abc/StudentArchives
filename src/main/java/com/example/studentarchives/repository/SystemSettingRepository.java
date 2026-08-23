package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 系统配置 Repository
 */
@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long>, JpaSpecificationExecutor<SystemSetting> {

    /**
     * 按配置键查询（deleted_at IS NULL 由 @SQLRestriction 保证）
     */
    Optional<SystemSetting> findBySettingKey(String settingKey);
}