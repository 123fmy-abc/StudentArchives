package com.example.studentarchives.repository;

import com.example.studentarchives.entity.award.AwardTypeConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 奖项类型配置 Repository
 */
@Repository
public interface AwardTypeConfigRepository extends JpaRepository<AwardTypeConfig, Long> {

    Optional<AwardTypeConfig> findByAwardTypeAndStatus(String awardType, Integer status);
}