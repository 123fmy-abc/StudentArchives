package com.example.studentarchives.repository;

import com.example.studentarchives.entity.user.RoleScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户角色数据范围 Repository
 */
@Repository
public interface RoleScopeRepository extends JpaRepository<RoleScope, Long> {

    List<RoleScope> findByUserIdAndStatus(Long userId, Integer status);

    List<RoleScope> findByUserId(Long userId);
}
