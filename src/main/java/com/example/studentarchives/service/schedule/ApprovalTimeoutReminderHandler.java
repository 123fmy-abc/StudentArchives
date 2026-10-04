package com.example.studentarchives.service.schedule;

import com.example.studentarchives.entity.approval.PendingApproval;
import com.example.studentarchives.entity.user.UserContactInfo;
import com.example.studentarchives.repository.PendingApprovalRepository;
import com.example.studentarchives.repository.UserContactInfoRepository;
import com.example.studentarchives.service.Fmy.EmailService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审批超时提醒处理器（task_code = approval_timeout_reminder）。
 * <p>
 * 查询指定学校下提交时间超过 24 小时仍未审批的待办任务，按审批人聚合后发送提醒邮件。
 * 超时阈值可在 scheduled_tasks.task_params 中覆盖：
 * <pre>
 * {
 *   "timeoutHours": 24
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ApprovalTimeoutReminderHandler implements ScheduledTaskHandler {

    public static final String TASK_CODE = "approval_timeout_remind";

    private static final int DEFAULT_TIMEOUT_HOURS = 24;

    private final PendingApprovalRepository pendingApprovalRepository;
    private final UserContactInfoRepository userContactInfoRepository;
    private final EmailService emailService;

    @Override
    public String getTaskCode() {
        return TASK_CODE;
    }

    @Override
    public String getDescription() {
        return "查询超时 24 小时未审批的待办并邮件提醒审批人";
    }

    @Override
    public void execute(TaskExecutionContext context) {
        Long schoolId = context.getSchoolId();
        int timeoutHours = parseTimeoutHours(context.getTaskParams());
        LocalDateTime cutoff = LocalDateTime.now().minusHours(timeoutHours);

        // status=1 表示待审批
        List<PendingApproval> timeoutApprovals = pendingApprovalRepository
                .findBySchoolIdAndStatusAndSubmittedAtBefore(schoolId, 1, cutoff);
        if (timeoutApprovals.isEmpty()) {
            log.info("审批超时提醒: 无超时待审批任务, schoolId={}, timeoutHours={}", schoolId, timeoutHours);
            return;
        }

        Map<Long, List<PendingApproval>> byAuditor = timeoutApprovals.stream()
                .collect(Collectors.groupingBy(PendingApproval::getAuditorId));

        List<Long> auditorIds = byAuditor.keySet().stream().toList();
        Map<Long, String> emails = userContactInfoRepository.findByUserIdIn(auditorIds).stream()
                .filter(u -> u.getEmail() != null && !u.getEmail().isBlank())
                .collect(Collectors.toMap(UserContactInfo::getUserId, UserContactInfo::getEmail, (a, b) -> a));

        int sent = 0;
        int skipped = 0;
        for (Map.Entry<Long, List<PendingApproval>> entry : byAuditor.entrySet()) {
            Long auditorId = entry.getKey();
            List<PendingApproval> list = entry.getValue();
            String email = emails.get(auditorId);
            if (email == null) {
                log.warn("审批人未配置邮箱，跳过提醒: auditorId={}, count={}", auditorId, list.size());
                skipped += list.size();
                continue;
            }
            try {
                sendReminder(email, list, timeoutHours);
                sent++;
            } catch (Exception e) {
                log.error("审批超时提醒邮件发送失败: auditorId={}, email={}, err={}",
                        auditorId, email, e.getMessage(), e);
                skipped += list.size();
            }
        }

        log.info("审批超时提醒完成: schoolId={}, timeoutHours={}, auditors={}, sent={}, skipped={}",
                schoolId, timeoutHours, byAuditor.size(), sent, skipped);
    }

    private void sendReminder(String to, List<PendingApproval> approvals, int timeoutHours) {
        String subject = "您有 " + approvals.size() + " 条审批任务已超时 " + timeoutHours + " 小时";
        StringBuilder body = new StringBuilder();
        body.append("以下为已超时审批任务，请及时处理：\n\n");
        int idx = 1;
        for (PendingApproval approval : approvals) {
            body.append(idx).append(". ")
                    .append(approval.getTitle())
                    .append("（").append(approval.getCategoryLabel()).append("）\n")
                    .append("   提交时间：").append(approval.getSubmittedAt()).append("\n")
                    .append("   当前步骤：").append(approval.getStepName()).append("\n\n");
            idx++;
        }
        emailService.sendSimpleMail(to, subject, body.toString());
    }

    private int parseTimeoutHours(JsonNode taskParams) {
        if (taskParams == null || !taskParams.hasNonNull("timeoutHours")) {
            return DEFAULT_TIMEOUT_HOURS;
        }
        int hours = taskParams.get("timeoutHours").asInt(DEFAULT_TIMEOUT_HOURS);
        return hours > 0 ? hours : DEFAULT_TIMEOUT_HOURS;
    }
}
