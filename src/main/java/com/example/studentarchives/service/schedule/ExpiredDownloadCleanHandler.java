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
 * 过期下载链接清理处理器（task_code = expired_download_clean）。
 * <p>
 * 删除指定学校下状态为已完成（final）且创建超过 30 天的导出任务记录，
 * 同时删除关联的附件记录与 OSS 物理文件。
 * 可在 scheduled_tasks.task_params 中覆盖保留天数：
 * <pre>
 * {
 *   "retentionDays": 30
 * }
 * </pre>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExpiredDownloadCleanHandler implements ScheduledTaskHandler {

    public static final String TASK_CODE = "expired_download_cleanup";

    private static final int DEFAULT_RETENTION_DAYS = 30;

    private final ExportJobRepository exportJobRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;
    private final OssFileService ossFileService;

    @Override
    public String getTaskCode() {
        return TASK_CODE;
    }

    @Override
    public String getDescription() {
        return "删除已完成超过 30 天的导出任务及物理文件";
    }

    @Override
    @Transactional
    public void execute(TaskExecutionContext context) {
        Long schoolId = context.getSchoolId();
        int retentionDays = parseRetentionDays(context.getTaskParams());
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);

        List<ExportJob> expiredJobs = exportJobRepository.findBySchoolIdAndStatusAndCreatedAtBefore(
                schoolId, ExportTaskStatusEnum.COMPLETED.getValue(), cutoff);

        int totalFiles = 0;
        for (ExportJob job : expiredJobs) {
            totalFiles += deleteAttachments(job.getFileId());
            softDeleteJob(job);
        }

        log.info("过期下载清理完成: schoolId={}, retentionDays={}, jobs={}, files={}",
                schoolId, retentionDays, expiredJobs.size(), totalFiles);
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
