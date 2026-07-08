package com.example.studentarchives.repository.user;

import com.example.studentarchives.entity.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** 根据学号/工号查找用户（自动过滤软删除） */
    Optional<User> findByUserNo(String userNo);
}
