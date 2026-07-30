package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.entity.log.LoginLog;
import com.example.studentarchives.repository.LoginLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 登录日志服务
 * <p>
 * 独立事务保存登录日志，确保失败登录日志不会被主事务回滚。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LoginLogService {

    /** 登录类型：密码登录 */
    private static final int LOGIN_TYPE_PASSWORD = 1;

    private final LoginLogRepository loginLogRepository;

    /**
     * 记录登录日志（独立事务）
     * <p>
     * REQUIRES_NEW 确保无论主事务是否回滚，日志都能写入数据库。
     * flush() 让 INSERT 在 try 块内执行，异常在 catch 中可见。
     *
     * @param schoolId   学校 ID
     * @param userId     用户 ID
     * @param status     登录状态：1=成功 0=失败
     * @param failReason 失败原因（成功时为 null）
     * @param ipAddress  IP 地址
     * @param userAgent  客户端 User-Agent
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordLoginLog(Long schoolId, Long userId, int status, String failReason,
                               String ipAddress, String userAgent) {
        log.info("记录登录日志: schoolId={}, userId={}, status={}, failReason={}, ip={}",
                schoolId, userId, status, failReason, ipAddress);
        try {
            LoginLog loginLog = new LoginLog();
            loginLog.setSchoolId(schoolId != null ? schoolId : 0L);
            loginLog.setUserId(userId);
            loginLog.setLoginType(LOGIN_TYPE_PASSWORD);
            loginLog.setIpAddress(ipAddress);
            loginLog.setUserAgent(userAgent);
            loginLog.setLoginStatus(status);
            loginLog.setFailReason(failReason);
            loginLog.setRetentionUntil(LocalDateTime.now().plusDays(180));
            loginLogRepository.save(loginLog);
            loginLogRepository.flush();
        } catch (Exception e) {
            log.error("记录登录日志失败: schoolId={}, userId={}, status={}, ip={}, userAgent={}",
                    schoolId, userId, status, ipAddress, userAgent, e);
        }
    }
}
