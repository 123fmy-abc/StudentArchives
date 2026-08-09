package com.example.studentarchives.repository;

import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.repository.projection.UserAuthStatus;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 用户 Repository
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUserNo(String userNo);

    Optional<User> findByUserNoAndStatus(String userNo, Integer status);

    /**
     * 查询指定学校下 ID 最小的用户（用于导出模板种子器的 created_by）
     */
    Optional<User> findFirstBySchoolIdOrderByIdAsc(Long schoolId);

    /**
     * 批量查询用户（用于审核人/教师姓名聚合查询）
     */
    List<User> findByIdIn(Collection<Long> ids);

    /**
     * 查询用户认证状态（用于 JWT 过滤器与刷新令牌）
     */
    @Cacheable(value = "userAuth", key = "#userId")
    @Query("SELECT u.status AS status, u.tokenVersion AS tokenVersion, u.refreshTokenVersion AS refreshTokenVersion FROM User u WHERE u.id = :userId")
    Optional<UserAuthStatus> findAuthStatusById(@Param("userId") Long userId);

    /**
     * 原子递增 tokenVersion，并清除用户认证状态缓存。
     * 事务由调用方 Service 层控制。
     * clearAutomatically = true：执行后清除一级缓存，防止后续查询读到脏数据。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @CacheEvict(value = "userAuth", key = "#userId")
    @Query("UPDATE User u SET u.tokenVersion = u.tokenVersion + 1 WHERE u.id = :userId")
    int incrementTokenVersion(@Param("userId") Long userId);

    /**
     * 原子递增 refreshTokenVersion，并清除用户认证状态缓存。
     * 事务由调用方 Service 层控制。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @CacheEvict(value = "userAuth", key = "#userId")
    @Query("UPDATE User u SET u.refreshTokenVersion = u.refreshTokenVersion + 1 WHERE u.id = :userId")
    int incrementRefreshTokenVersion(@Param("userId") Long userId);

    /**
     * CAS 递增 refreshTokenVersion，用于防止并发刷新导致令牌混乱。
     * 事务由调用方 Service 层控制。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @CacheEvict(value = "userAuth", key = "#userId")
    @Query("UPDATE User u SET u.refreshTokenVersion = u.refreshTokenVersion + 1 WHERE u.id = :userId AND u.refreshTokenVersion = :expectedVersion")
    int compareAndIncrementRefreshTokenVersion(@Param("userId") Long userId, @Param("expectedVersion") Integer expectedVersion);

    /**
     * 同时递增 tokenVersion 与 refreshTokenVersion，吊销用户全部令牌。
     * 事务由调用方 Service 层控制。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @CacheEvict(value = "userAuth", key = "#userId")
    @Query("UPDATE User u SET u.tokenVersion = u.tokenVersion + 1, u.refreshTokenVersion = u.refreshTokenVersion + 1 WHERE u.id = :userId")
    int revokeAllTokens(@Param("userId") Long userId);
}
