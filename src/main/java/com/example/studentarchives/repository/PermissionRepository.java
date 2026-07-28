package com.example.studentarchives.repository;

import com.example.studentarchives.entity.user.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 权限 Repository
 */
@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    List<Permission> findByIdIn(List<Long> ids);
}
