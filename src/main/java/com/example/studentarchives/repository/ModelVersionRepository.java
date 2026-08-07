package com.example.studentarchives.repository;

import com.example.studentarchives.entity.version.ModelVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 模型版本记录 Repository（对应表 model_versions）
 */
@Repository
public interface ModelVersionRepository extends JpaRepository<ModelVersion, Long> {

    /** 按模型类型 + 模型 ID 查询版本历史，按版本号正序 */
    List<ModelVersion> findByModelTypeAndModelIdOrderByVersionAsc(String modelType, Long modelId);
}
