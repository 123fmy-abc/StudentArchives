package com.example.studentarchives.repository.role;

import com.example.studentarchives.entity.user.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    /** 根据角色ID列表查询所有关联的权限 */
    @Query("SELECT p FROM Permission p JOIN RolePermission rp ON p.id = rp.permissionId WHERE rp.roleId IN :roleIds")
    List<Permission> findByRoleIds(List<Long> roleIds);
}
