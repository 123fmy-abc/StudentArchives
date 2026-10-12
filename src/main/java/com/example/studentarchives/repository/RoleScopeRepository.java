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

    /** 按学校 + 范围类型 + 范围ID + 状态查询授权（校级范围兜底审批人解析用） */
    List<RoleScope> findBySchoolIdAndScopeTypeAndScopeIdAndStatus(Long schoolId, Integer scopeType,
                                                                 Long scopeId, Integer status);
}
