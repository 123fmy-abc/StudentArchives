package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.entity.message.Announcement;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.enums.ApplyStatusEnum;
import com.example.studentarchives.repository.AnnouncementRepository;
import com.example.studentarchives.service.Lzw.ApplicationService;
import com.example.studentarchives.service.Lzw.AwardService;
import com.example.studentarchives.service.Lzw.SystemConfigManageService;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 消息生产切面（AOP 接线，不改动任何既有业务代码）
 * <p>
 * 在既有业务服务（Lzw 包）的申报提交、公告发布等事件完成后，通过本切面调用
 * {@link MessageProducer} 自动写入站内消息，补齐"消息生产端缺口"：
 * <ul>
 *   <li>档案申报提交（{@code ApplicationService.submit*} / {@code correction}）→ 给学生发审核提醒（audit_remind）；</li>
 *   <li>奖项申报提交（{@code AwardService.submit*}）→ 给学生发审核提醒（audit_remind）；</li>
 *   <li>公告发布（{@code SystemConfigManageService.createAnnouncement}）→ 按发布对象粉丝播系统通知（system_notice）。</li>
 * </ul>
 * <p>
 * 可靠性设计：
 * <ul>
 *   <li>全部通知体用 try/catch 包裹并记日志，消息生产失败绝不向上抛出，不影响业务主流程；</li>
 *   <li>申报类通知仅在返回响应 status=PENDING（真实提交，草稿不通知）时触发；</li>
 *   <li>公告类通知仅在公告 status=1（已发布）时触发。</li>
 * </ul>
 * <p>
 * 尚未接线的扩展点（待对应流程落地后在切面/业务处补发）：
 * 审批通过/驳回（当前后端无状态流转到 2/3 的代码）、动态提醒（成长时间轴由申报派生，无独立写入方法）。
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class MessageProduceAspect {

    /** 公告已发布状态（对齐 announcements.status） */
    private static final int ANNOUNCEMENT_STATUS_PUBLISHED = 1;

    private final MessageProducer messageProducer;
    private final AnnouncementRepository announcementRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;

    // ==================== 切点 ====================

    /** 档案类申报提交（竞赛/奖学金/创新/科研/证书/实习/组织/培训/实践/书评） */
    @Pointcut("execution(* com.example.studentarchives.service.Lzw.ApplicationService.submit*(..))")
    private void archiveSubmitMethods() {
    }

    /** 档案申报被退回后的重新提交 */
    @Pointcut("execution(* com.example.studentarchives.service.Lzw.ApplicationService.correction(..))")
    private void archiveCorrectionMethod() {
    }

    /** 奖项类申报提交（竞赛之星/科研之星/创新之星） */
    @Pointcut("execution(* com.example.studentarchives.service.Lzw.AwardService.submit*(..))")
    private void awardSubmitMethods() {
    }

    /** 管理端发布公告 */
    @Pointcut("execution(* com.example.studentarchives.service.Lzw.SystemConfigManageService.createAnnouncement(..))")
    private void announcementCreateMethod() {
    }

    // ==================== 档案申报提交通知 ====================

    /**
     * 档案申报提交成功 → 给学生发审核提醒（audit_remind，重要）。
     * 仅在返回响应 status=PENDING（真实提交）时触发，草稿（status=0）不通知。
     */
    @AfterReturning(pointcut = "archiveSubmitMethods() || archiveCorrectionMethod()", returning = "result")
    public void notifyArchiveSubmitted(JoinPoint joinPoint, Object result) {
        try {
            if (!(result instanceof ApplicationService.ArchiveSubmitResponse r)) {
                return;
            }
            if (r.getStatus() == null || r.getStatus() != ApplyStatusEnum.PENDING.getValue()) {
                return;
            }
            Long userId = extractLastUserId(joinPoint);
            if (userId == null || r.getArchiveId() == null) {
                return;
            }
            boolean isCorrection = "correction".equals(joinPoint.getSignature().getName());
            String title = isCorrection ? "申报已重新提交" : "申报提交成功";
            String content = isCorrection
                    ? "您被退回的申报已修改并重新提交，请等待审核。"
                    : "您的申报已提交，请等待审核。";
            messageProducer.auditRemind(userId, title, content, "archive", r.getArchiveId(), null, 1);
        } catch (Exception e) {
            log.warn("档案申报提交消息通知失败（不影响主流程）: {}", e.getMessage());
        }
    }

    // ==================== 奖项申报提交通知 ====================

    /**
     * 奖项申报提交成功 → 给学生发审核提醒（audit_remind，重要）。
     * 仅在返回响应 status=PENDING（真实提交）时触发。
     */
    @AfterReturning(pointcut = "awardSubmitMethods()", returning = "result")
    public void notifyAwardSubmitted(JoinPoint joinPoint, Object result) {
        try {
            if (!(result instanceof AwardService.AwardSubmitResponse r)) {
                return;
            }
            if (r.getStatus() == null || r.getStatus() != ApplyStatusEnum.PENDING.getValue()) {
                return;
            }
            Long userId = extractLastUserId(joinPoint);
            if (userId == null || r.getApplicationId() == null) {
                return;
            }
            messageProducer.auditRemind(userId, "奖项申报提交成功",
                    "您的奖项申报已提交，请等待审核。", "award_application", r.getApplicationId(), null, 1);
        } catch (Exception e) {
            log.warn("奖项申报提交消息通知失败（不影响主流程）: {}", e.getMessage());
        }
    }

    // ==================== 公告发布系统通知 ====================

    /**
     * 公告发布 → 按发布对象（all/class/major/college）粉丝播系统通知（system_notice）。
     * 仅在公告 status=1（已发布）时触发；已发布对象无学生时静默跳过。
     */
    @AfterReturning(pointcut = "announcementCreateMethod()", returning = "result")
    public void notifyAnnouncementPublished(Object result) {
        try {
            if (!(result instanceof SystemConfigManageService.AnnouncementIdResponse r)
                    || r.getAnnouncementId() == null) {
                return;
            }
            Announcement announcement = announcementRepository.findById(r.getAnnouncementId()).orElse(null);
            if (announcement == null
                    || announcement.getStatus() == null
                    || announcement.getStatus() != ANNOUNCEMENT_STATUS_PUBLISHED) {
                return;
            }
            List<Long> userIds = resolveAnnouncementRecipients(
                    announcement.getTargetType(), announcement.getTargetId(), announcement.getSchoolId());
            if (userIds.isEmpty()) {
                return;
            }
            MessageProducer.MessageSpec spec = MessageProducer.MessageSpec.systemNotice(
                    announcement.getTitle(), announcement.getContent(),
                    "announcement", announcement.getId(), null, 0);
            messageProducer.sendToUsers(userIds, spec);
        } catch (Exception e) {
            log.warn("公告发布系统通知失败（不影响主流程）: {}", e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    /** 从切点方法参数中取接收者用户 ID（各 submit/correction 方法最后一个参数均为 userId） */
    private Long extractLastUserId(JoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args.length == 0) {
            return null;
        }
        Object last = args[args.length - 1];
        return last instanceof Long userId ? userId : null;
    }

    /**
     * 解析公告发布对象 → 学生用户 ID 列表（去重）。
     * all=全校、class=班级、major=专业（→班级→学生）、college=学院（→专业→班级→学生）。
     */
    private List<Long> resolveAnnouncementRecipients(String targetType, Long targetId, Long schoolId) {
        if (targetType == null) {
            return Collections.emptyList();
        }
        List<StudentProfile> profiles;
        switch (targetType) {
            case "all" -> profiles = studentProfileRepository.findBySchoolId(schoolId);
            case "class" -> {
                if (targetId == null) {
                    return Collections.emptyList();
                }
                profiles = studentProfileRepository.findByClassId(targetId);
            }
            case "major" -> {
                if (targetId == null) {
                    return Collections.emptyList();
                }
                List<Long> classIds = clazzRepository.findByMajorId(targetId).stream()
                        .map(Clazz::getId).toList();
                if (classIds.isEmpty()) {
                    return Collections.emptyList();
                }
                profiles = studentProfileRepository.findByClassIdIn(classIds);
            }
            case "college" -> {
                if (targetId == null) {
                    return Collections.emptyList();
                }
                List<Long> majorIds = majorRepository.findByCollegeIdIn(List.of(targetId)).stream()
                        .map(Major::getId).toList();
                if (majorIds.isEmpty()) {
                    return Collections.emptyList();
                }
                List<Long> classIds = clazzRepository.findByMajorIdIn(majorIds).stream()
                        .map(Clazz::getId).toList();
                if (classIds.isEmpty()) {
                    return Collections.emptyList();
                }
                profiles = studentProfileRepository.findByClassIdIn(classIds);
            }
            default -> {
                return Collections.emptyList();
            }
        }
        return profiles.stream()
                .map(StudentProfile::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
