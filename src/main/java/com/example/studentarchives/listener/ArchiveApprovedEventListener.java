package com.example.studentarchives.listener;

import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.event.ArchiveApprovedEvent;
import com.example.studentarchives.service.Fmy.AdminScoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 档案审核通过事件监听器。
 * <p>
 * 监听到 {@link ArchiveApprovedEvent} 后，自动触发该学生对应学期的评分重算。
 * 若档案无学期归属，则跳过自动评分（无学期归属的档案会在学期级重算时统一处理）。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ArchiveApprovedEventListener {

    private final AdminScoreService adminScoreService;

    @EventListener
    public void onArchiveApproved(ArchiveApprovedEvent event) {
        Archive archive = event.getArchive();
        if (archive == null || archive.getUserId() == null || archive.getSchoolId() == null) {
            log.warn("档案审核通过事件缺少必要字段，跳过自动评分: archive={}", archive);
            return;
        }
        if (archive.getSemesterId() == null) {
            log.info("档案通过但无学期归属，暂不自动触发评分: archiveId={}", archive.getId());
            return;
        }

        log.info("档案审核通过，触发自动评分: archiveId={}, userId={}, semesterId={}",
                archive.getId(), archive.getUserId(), archive.getSemesterId());
        adminScoreService.recalculateStudent(
                archive.getSchoolId(),
                archive.getUserId(),
                archive.getSemesterId(),
                event.getOperatorId());
    }
}
