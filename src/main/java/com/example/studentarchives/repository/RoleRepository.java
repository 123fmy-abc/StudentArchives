package com.example.studentarchives.repository;

import com.example.studentarchives.entity.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色 Repository
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    List<Role> findByIdIn(List<Long> ids);

    Optional<Role> findByCode(String code);
}
