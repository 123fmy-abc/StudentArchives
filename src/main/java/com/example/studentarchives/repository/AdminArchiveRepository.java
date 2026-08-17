package com.example.studentarchives.repository;

import com.example.studentarchives.entity.archive.Archive;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * 档案补充 Repository（对应表 archives）
 * <p>
 * 管理端档案管理模块 15.1 列表使用 Specification 组合（组织维度学生集合 +
 * 档案类型/状态/学期/关键词）做数据库级分页，避免全量载入内存。
 */
@Repository
public interface AdminArchiveRepository extends JpaRepository<Archive, Long>, JpaSpecificationExecutor<Archive> {
}
