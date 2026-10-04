package com.example.studentarchives.repository;

import com.example.studentarchives.entity.user.Role;
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
 * 角色 Repository
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long>, JpaSpecificationExecutor<Role> {

    List<Role> findByIdIn(List<Long> ids);

    /**
     * 按 ID 批量查询指定状态的启用角色。
     * <p>
     * 鉴权链路统一使用本方法而非 {@link #findByIdIn}：{@code roles.status=0}（禁用）的角色
     * 不应再授予任何权限（见《学生端接口文档》1.2「账号状态」：{@code roles.status} 0=禁用 1=正常）。
     * 软删除（{@code deleted_at}）已由实体上的 {@code @SQLRestriction} 自动过滤。
     *
     * @param ids    角色 ID 列表
     * @param status 角色状态：1=启用
     */
    List<Role> findByIdInAndStatus(List<Long> ids, Integer status);

    Optional<Role> findByCode(String code);

    /**
     * 软删除角色（置 deleted_at）
     */
    @Modifying
    @Query(value = "UPDATE roles SET deleted_at = :deletedAt WHERE id = :id AND deleted_at IS NULL", nativeQuery = true)
    int softDeleteById(@Param("id") Long id, @Param("deletedAt") LocalDateTime deletedAt);
}
