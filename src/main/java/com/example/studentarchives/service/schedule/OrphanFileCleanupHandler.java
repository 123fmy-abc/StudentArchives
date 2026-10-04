package com.example.studentarchives.service.schedule;

import com.example.studentarchives.entity.export.ExportJob;
import com.example.studentarchives.entity.file.AttachmentRelation;
import com.example.studentarchives.enums.ExportTaskStatusEnum;
import com.example.studentarchives.repository.AttachmentRelationRepository;
import com.example.studentarchives.repository.ExportJobRepository;
import com.example.studentarchives.service.Fmy.OssFileService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 孤儿文件清理处理器（task_code = orphan_file_cleanup）。
 * <p>
 * 清理指定学校下处于失败状态，或长期处于 pending/running 卡死的导出任务及其关联文件。
 * 默认清理 7 天前的记录，可在 scheduled_tasks.task_params 中覆盖：
 * <pre>
 * {
 *   "retentionDays": 7
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanFileCleanupHandler implements ScheduledTaskHandler {

    public static final String TASK_CODE = "orphan_file_cleanup";

    private static final int DEFAULT_RETENTION_DAYS = 7;

    private final ExportJobRepository exportJobRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;
    private final OssFileService ossFileService;

    @Override
    public String getTaskCode() {
        return TASK_CODE;
    }

    @Override
    public String getDescription() {
        return "清理失败或卡死的导出任务及其临时文件";
    }

    @Override
    @Transactional
    public void execute(TaskExecutionContext context) {
        Long schoolId = context.getSchoolId();
        int retentionDays = parseRetentionDays(context.getTaskParams());
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        int totalJobs = 0;
        int totalFiles = 0;

        // 1) 清理失败的导出任务
        List<ExportJob> failedJobs = exportJobRepository.findBySchoolIdAndStatusAndCreatedAtBefore(
                schoolId, ExportTaskStatusEnum.FAILED.getValue(), cutoff);
        for (ExportJob job : failedJobs) {
            totalFiles += deleteAttachments(job.getFileId());
            softDeleteJob(job);
            totalJobs++;
        }

        // 2) 清理长期卡住（pending/running）的导出任务
        LocalDateTime stuckCutoff = LocalDateTime.now().minusDays(1);
        List<ExportJob> pendingJobs = exportJobRepository.findBySchoolIdAndStatusAndCreatedAtBefore(
                schoolId, ExportTaskStatusEnum.PENDING.getValue(), stuckCutoff);
        for (ExportJob job : pendingJobs) {
            totalFiles += deleteAttachments(job.getFileId());
            softDeleteJob(job);
            totalJobs++;
        }
        List<ExportJob> runningJobs = exportJobRepository.findBySchoolIdAndStatusAndCreatedAtBefore(
                schoolId, ExportTaskStatusEnum.RUNNING.getValue(), stuckCutoff);
        for (ExportJob job : runningJobs) {
            totalFiles += deleteAttachments(job.getFileId());
            softDeleteJob(job);
            totalJobs++;
        }

        log.info("孤儿文件清理完成: schoolId={}, retentionDays={}, jobs={}, files={}",
                schoolId, retentionDays, totalJobs, totalFiles);
    }

    private int deleteAttachments(Long fileId) {
        if (fileId == null) {
            return 0;
        }
        AttachmentRelation attachment = attachmentRelationRepository.findById(fileId).orElse(null);
        if (attachment == null) {
            return 0;
        }
        try {
            ossFileService.deleteFile(attachment.getFilePath());
        } catch (Exception e) {
            log.warn("删除 OSS 文件失败，继续清理附件记录: fileId={}, path={}, err={}",
                    fileId, attachment.getFilePath(), e.getMessage());
        }
        attachmentRelationRepository.softDeleteById(fileId, LocalDateTime.now(), null);
        return 1;
    }

    private void softDeleteJob(ExportJob job) {
        exportJobRepository.softDeleteById(job.getId(), LocalDateTime.now());
    }

    private int parseRetentionDays(JsonNode taskParams) {
        if (taskParams == null || !taskParams.hasNonNull("retentionDays")) {
            return DEFAULT_RETENTION_DAYS;
        }
        int days = taskParams.get("retentionDays").asInt(DEFAULT_RETENTION_DAYS);
        return days > 0 ? days : DEFAULT_RETENTION_DAYS;
    }
}
