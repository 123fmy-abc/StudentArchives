package com.example.studentarchives.repository.foundation;

import com.example.studentarchives.entity.foundation.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    /** 根据配置键查找 */
    Optional<SystemSetting> findBySettingKey(String settingKey);

    /** 根据分组查找所有配置 */
    List<SystemSetting> findBySettingGroupOrderBySettingKeyAsc(String settingGroup);

    /** 根据键名模糊查找 */
    List<SystemSetting> findBySettingKeyStartingWith(String prefix);
}
