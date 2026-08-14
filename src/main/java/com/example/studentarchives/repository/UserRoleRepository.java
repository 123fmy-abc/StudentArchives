package com.example.studentarchives.repository;

import com.example.studentarchives.entity.user.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

/**
 * 用户角色关联 Repository
 */
@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

    List<UserRole> findByUserId(Long userId);

    List<UserRole> findByUserIdIn(Collection<Long> userIds);

    List<UserRole> findByRoleId(Long roleId);
}
