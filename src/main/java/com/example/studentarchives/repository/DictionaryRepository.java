package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.Dictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 字典 Repository
 */
@Repository
public interface DictionaryRepository extends JpaRepository<Dictionary, Long>, JpaSpecificationExecutor<Dictionary> {

    /**
     * 根据字典类型查询启用的字典列表，按 sort 正序
     */
    @Query("SELECT d FROM Dictionary d WHERE d.dictType = :dictType AND d.status = 1 ORDER BY d.sort ASC")
    List<Dictionary> findActiveByDictType(@Param("dictType") String dictType);

    /**
     * 同一类型内按字典值查询（用于唯一性校验，deleted_at IS NULL 由 @SQLRestriction 保证）
     */
    Optional<Dictionary> findByDictTypeAndDictCode(String dictType, String dictCode);

    /**
     * 是否存在以指定字典项为父级的子级字典项（删除前引用校验）
     */
    boolean existsByParentId(Long parentId);

    /**
     * 软删除字典项（deleted_at 置为当前时间，仅命中未删除记录）
     */
    @Modifying
    @Query(value = "UPDATE dictionaries SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
