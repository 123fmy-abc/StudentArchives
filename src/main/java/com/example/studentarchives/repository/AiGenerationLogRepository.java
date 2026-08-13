package com.example.studentarchives.repository;

import com.example.studentarchives.entity.ai.AiGenerationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * AI 生成记录 Repository（对应表 ai_generation_logs）
 */
@Repository
public interface AiGenerationLogRepository extends JpaRepository<AiGenerationLog, Long> {

    /**
     * 查询某关联对象最新一条生成记录（用于重新生成时定位原调用）
     */
    Optional<AiGenerationLog> findFirstByRelatedTypeAndRelatedIdOrderByIdDesc(String relatedType, Long relatedId);

    /**
     * 批量查询关联记录（用于建议列表聚合教师反馈）
     */
    List<AiGenerationLog> findByRelatedTypeAndRelatedIdIn(String relatedType, Collection<Long> relatedIds);
}