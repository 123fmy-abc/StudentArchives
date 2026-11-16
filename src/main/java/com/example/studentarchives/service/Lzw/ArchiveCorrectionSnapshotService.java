package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.entity.BaseEntity;
import com.example.studentarchives.entity.archive.*;
import com.example.studentarchives.entity.embed.ArchiveAuditInfo;
import com.example.studentarchives.entity.version.ModelVersion;
import com.example.studentarchives.enums.ApplyStatusEnum;
import com.example.studentarchives.enums.ModelVersionModelTypeEnum;
import com.example.studentarchives.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 更正已通过档案的快照与还原服务（更正语义为「原地更新」）。
 * <p>
 * 更正提交前把原已通过版本（档案字段 + 扩展表数据）写入 model_versions 快照；
 * 更正被驳回时按快照还原档案字段与扩展表数据，状态回到已通过，
 * 保证一条档案始终只有一行记录，不会出现新旧两条已通过数据。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArchiveCorrectionSnapshotService {

    private final ArchiveRepository archiveRepository;
    private final ModelVersionRepository modelVersionRepository;
    private final ObjectMapper objectMapper;

    // 10 个扩展表 Repository
    private final ArchiveCompetitionRepository competitionRepository;
    private final ArchiveScholarshipRepository scholarshipRepository;
    private final ArchiveInnovationRepository innovationRepository;
    private final ArchiveResearchRepository researchRepository;
    private final ArchiveCertificateRepository certificateRepository;
    private final ArchiveInternshipRepository internshipRepository;
    private final ArchiveOrganizationRepository organizationRepository;
    private final ArchiveTrainingProjectRepository trainingProjectRepository;
    private final ArchiveSocialPracticeRepository socialPracticeRepository;
    private final ArchiveBookReviewRepository bookReviewRepository;

    /**
     * 更正提交前快照当前版本（含扩展表），写入 model_versions 的当前版本号行。
     * 与 {@code writeArchiveVersion} 的幂等语义一致：同版本已存在时覆盖更新。
     */
    @Transactional
    public void writePreCorrectionSnapshot(Archive archive, Long userId) {
        int preVersion = currentVersion(archive);
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", archive.getId());
        snapshot.put("archiveType", archive.getArchiveType());
        snapshot.put("title", archive.getTitle());
        snapshot.put("semesterId", archive.getSemesterId());
        snapshot.put("obtainedAt", archive.getObtainedAt() != null ? archive.getObtainedAt().toString() : null);
        snapshot.put("status", archive.getStatus());
        snapshot.put("extension", snapshotExtensionJson(archive.getArchiveType(), archive.getId()));

        ModelVersion mv = modelVersionRepository
                .findByModelTypeAndModelIdAndVersion(
                        ModelVersionModelTypeEnum.ARCHIVE.getValue(), archive.getId(), preVersion)
                .orElseGet(ModelVersion::new);
        mv.setModelType(ModelVersionModelTypeEnum.ARCHIVE.getValue());
        mv.setModelId(archive.getId());
        mv.setVersion(preVersion);
        mv.setTitle(archive.getTitle());
        mv.setDataSnapshot(writeJson(snapshot));
        mv.setStatus(archive.getStatus());
        mv.setCreatedBy(userId);
        modelVersionRepository.save(mv);
    }

    /**
     * 更正被驳回时还原原已通过版本：按当前版本的前一版快照恢复档案字段与扩展表数据，
     * 状态回到已通过并清除更正原因。非更正驳回（correctionReason 为空）不做处理。
     * <p>
     * 快照缺失时仅记警告并恢复状态，不阻断驳回流程。
     */
    @Transactional
    public void restoreOnRejected(Archive archive) {
        if (archive.getCorrectionReason() == null || archive.getCorrectionReason().isBlank()) {
            return;
        }
        int preVersion = Math.max(1, currentVersion(archive) - 1);
        String json = modelVersionRepository
                .findByModelTypeAndModelIdAndVersion(
                        ModelVersionModelTypeEnum.ARCHIVE.getValue(), archive.getId(), preVersion)
                .map(ModelVersion::getDataSnapshot)
                .orElse(null);
        if (json == null) {
            log.warn("更正驳回还原失败：缺少版本快照 archiveId={}, version={}", archive.getId(), preVersion);
        } else {
            applySnapshot(archive, json);
        }
        archive.setStatus(ApplyStatusEnum.APPROVED.getValue());
        archive.setCorrectionReason(null);
        archiveRepository.save(archive);
    }

    private void applySnapshot(Archive archive, String json) {
        Map<String, Object> snapshot;
        try {
            snapshot = objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("更正驳回还原失败：快照反序列化异常 archiveId={}", archive.getId(), e);
            return;
        }
        if (snapshot.containsKey("title") && snapshot.get("title") != null) {
            archive.setTitle(snapshot.get("title").toString());
        }
        if (snapshot.get("semesterId") != null) {
            archive.setSemesterId(toLong(snapshot.get("semesterId")));
        }
        if (snapshot.get("obtainedAt") != null) {
            try {
                archive.setObtainedAt(LocalDate.parse(snapshot.get("obtainedAt").toString()));
            } catch (Exception e) {
                log.warn("更正驳回还原获得时间解析失败 archiveId={}", archive.getId(), e);
            }
        }
        restoreExtension(archive.getArchiveType(), archive.getId(), snapshot.get("extension"));
    }

    // ==================== 扩展表快照与还原 ====================

    private String snapshotExtensionJson(String archiveType, Long archiveId) {
        return findExtension(archiveType, archiveId).map(this::writeJson).orElse(null);
    }

    private Optional<?> findExtension(String archiveType, Long archiveId) {
        return switch (archiveType) {
            case "academic_competition" -> competitionRepository.findByArchiveId(archiveId);
            case "scholarship" -> scholarshipRepository.findByArchiveId(archiveId);
            case "innovation_entrepreneurship" -> innovationRepository.findByArchiveId(archiveId);
            case "academic_research" -> researchRepository.findByArchiveId(archiveId);
            case "honor_certificate" -> certificateRepository.findByArchiveId(archiveId);
            case "internship" -> internshipRepository.findByArchiveId(archiveId);
            case "organization" -> organizationRepository.findByArchiveId(archiveId);
            case "training_project" -> trainingProjectRepository.findByArchiveId(archiveId);
            case "social_practice" -> socialPracticeRepository.findByArchiveId(archiveId);
            case "book_review" -> bookReviewRepository.findByArchiveId(archiveId);
            default -> Optional.empty();
        };
    }

    @SuppressWarnings("unchecked")
    private void restoreExtension(String archiveType, Long archiveId, Object extension) {
        if (!(extension instanceof Map)) {
            return;
        }
        String extJson = writeJson(extension);
        switch (archiveType) {
            case "academic_competition" -> findExtension(archiveType, archiveId)
                    .ifPresent(cur -> restoreExtensionRow(extJson, ArchiveCompetition.class, (ArchiveCompetition) cur, competitionRepository));
            case "scholarship" -> findExtension(archiveType, archiveId)
                    .ifPresent(cur -> restoreExtensionRow(extJson, ArchiveScholarship.class, (ArchiveScholarship) cur, scholarshipRepository));
            case "innovation_entrepreneurship" -> findExtension(archiveType, archiveId)
                    .ifPresent(cur -> restoreExtensionRow(extJson, ArchiveInnovation.class, (ArchiveInnovation) cur, innovationRepository));
            case "academic_research" -> findExtension(archiveType, archiveId)
                    .ifPresent(cur -> restoreExtensionRow(extJson, ArchiveResearch.class, (ArchiveResearch) cur, researchRepository));
            case "honor_certificate" -> findExtension(archiveType, archiveId)
                    .ifPresent(cur -> restoreExtensionRow(extJson, ArchiveCertificate.class, (ArchiveCertificate) cur, certificateRepository));
            case "internship" -> findExtension(archiveType, archiveId)
                    .ifPresent(cur -> restoreExtensionRow(extJson, ArchiveInternship.class, (ArchiveInternship) cur, internshipRepository));
            case "organization" -> findExtension(archiveType, archiveId)
                    .ifPresent(cur -> restoreExtensionRow(extJson, ArchiveOrganization.class, (ArchiveOrganization) cur, organizationRepository));
            case "training_project" -> findExtension(archiveType, archiveId)
                    .ifPresent(cur -> restoreExtensionRow(extJson, ArchiveTrainingProject.class, (ArchiveTrainingProject) cur, trainingProjectRepository));
            case "social_practice" -> findExtension(archiveType, archiveId)
                    .ifPresent(cur -> restoreExtensionRow(extJson, ArchiveSocialPractice.class, (ArchiveSocialPractice) cur, socialPracticeRepository));
            case "book_review" -> findExtension(archiveType, archiveId)
                    .ifPresent(cur -> restoreExtensionRow(extJson, ArchiveBookReview.class, (ArchiveBookReview) cur, bookReviewRepository));
            default -> { }
        }
    }

    /** 用快照 JSON 反序列化并覆盖扩展表原行（保持主键不变，即原地更新） */
    private <T extends BaseEntity> void restoreExtensionRow(String json, Class<T> clazz, T current, JpaRepository<T, Long> repo) {
        try {
            T restored = objectMapper.readValue(json, clazz);
            restored.setId(current.getId());
            repo.save(restored);
        } catch (Exception e) {
            log.warn("更正驳回还原扩展表失败 entity={}", clazz.getSimpleName(), e);
        }
    }

    // ==================== 工具 ====================

    private int currentVersion(Archive archive) {
        ArchiveAuditInfo audit = archive.getAuditInfo();
        return audit != null && audit.getCurrentVersion() != null ? audit.getCurrentVersion() : 1;
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("更正快照序列化失败", e);
            return null;
        }
    }
}
