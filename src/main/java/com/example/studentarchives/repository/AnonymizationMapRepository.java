package com.example.studentarchives.repository;

import com.example.studentarchives.entity.export.AnonymizationMap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 匿名化映射 Repository（对应表 anonymization_maps）
 * <p>
 * 管理端研究数据导出（《管理端接口文档》5.1）使用：以学校为维度维护
 * {@code user_id → anonymous_code} 的稳定映射，同一学生多次导出复用同一匿名编号。
 */
@Repository
public interface AnonymizationMapRepository extends JpaRepository<AnonymizationMap, Long> {

    /**
     * 查询指定学校的全部匿名化映射（用于构建 userId → anonymousCode 索引与生成新编号）
     */
    List<AnonymizationMap> findBySchoolId(Long schoolId);
}
