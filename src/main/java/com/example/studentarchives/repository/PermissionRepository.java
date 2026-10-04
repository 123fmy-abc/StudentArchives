package com.example.studentarchives.repository;

import com.example.studentarchives.entity.user.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 权限 Repository
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long>, JpaSpecificationExecutor<Permission> {

    List<Permission> findByIdIn(List<Long> ids);

    /**
     * 按权限码查询（{@code permissions.code} 唯一，见迁移 {@code uk_permissions_code}）。
     * 软删除由 {@code Permission} 实体上的 {@code @SQLRestriction} 过滤。
     */
    Optional<Permission> findByCode(String code);
}
