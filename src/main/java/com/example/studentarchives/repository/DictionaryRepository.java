package com.example.studentarchives.repository;

import com.example.studentarchives.entity.foundation.Dictionary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 字典 Repository
 */
@Repository
public interface DictionaryRepository extends JpaRepository<Dictionary, Long> {

    /**
     * 根据字典类型查询启用的字典列表，按 sort 正序
     */
    @Query("SELECT d FROM Dictionary d WHERE d.dictType = :dictType AND d.status = 1 ORDER BY d.sort ASC")
    List<Dictionary> findActiveByDictType(@Param("dictType") String dictType);
}
