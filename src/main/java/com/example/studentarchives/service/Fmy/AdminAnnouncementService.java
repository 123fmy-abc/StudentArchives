package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.announcement.request.AnnouncementPublishRequest;
import com.example.studentarchives.dto.Fmy.announcement.response.AnnouncementResponse;
import com.example.studentarchives.entity.message.Announcement;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AnnouncementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端信息发布（公告）服务
 * <p>
 * 明确图片中“表单自定义 → 发布信息”语义：此处为向用户发布公告/通知，
 * 与 {@link AdminFormTemplateService#publish(Long, Long)} 的“表单模板发布”完全区分。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAnnouncementService {

    private final AdminAuthService adminAuthService;
    private final AnnouncementRepository announcementRepository;

    @Transactional(readOnly = true)
    public List<AnnouncementResponse> list(Long userId, Long schoolId) {
        adminAuthService.requireAdminOrPermission(userId, "announcement:view", "announcement:manage");
        return announcementRepository.findBySchoolIdAndStatusOrderByPublishedAtDesc(schoolId, 1).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public AnnouncementResponse publish(Long userId, AnnouncementPublishRequest request) {
        adminAuthService.requireAdminOrPermission(userId, "announcement:manage");
        Announcement announcement = new Announcement();
        announcement.setSchoolId(request.getSchoolId());
        announcement.setTitle(request.getTitle());
        announcement.setContent(request.getContent());
        announcement.setPublisherId(userId);
        announcement.setTargetType(request.getTargetType());
        announcement.setTargetId(request.getTargetId());
        announcement.setPublishedAt(LocalDateTime.now());
        announcement.setStatus(1);
        announcementRepository.save(announcement);

        log.info("发布公告: id={}, schoolId={}, targetType={}, operatorId={}",
                announcement.getId(), request.getSchoolId(), request.getTargetType(), userId);
        return toResponse(announcement);
    }

    @Transactional
    public void delete(Long userId, Long announcementId) {
        adminAuthService.requireAdminOrPermission(userId, "announcement:manage");
        Announcement announcement = announcementRepository.findById(announcementId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "公告不存在"));
        announcement.setStatus(0);
        announcementRepository.save(announcement);
        log.info("删除公告: id={}, operatorId={}", announcementId, userId);
    }

    private AnnouncementResponse toResponse(Announcement a) {
        return AnnouncementResponse.builder()
                .id(a.getId())
                .schoolId(a.getSchoolId())
                .title(a.getTitle())
                .content(a.getContent())
                .publisherId(a.getPublisherId())
                .targetType(a.getTargetType())
                .targetId(a.getTargetId())
                .publishedAt(a.getPublishedAt() != null ? a.getPublishedAt().toString() : null)
                .status(a.getStatus())
                .build();
    }
}
