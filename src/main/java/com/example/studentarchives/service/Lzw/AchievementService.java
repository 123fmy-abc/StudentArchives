package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Lzw.achievement.response.AchievementDetailResponse;
import com.example.studentarchives.dto.Lzw.achievement.response.AchievementDownloadTicketResponse;
import com.example.studentarchives.dto.Lzw.achievement.response.AchievementListItemResponse;
import com.example.studentarchives.dto.Lzw.achievement.response.AchievementListResponse;
import com.example.studentarchives.entity.archive.*;
import com.example.studentarchives.entity.file.AttachmentRelation;
import com.example.studentarchives.entity.foundation.Dictionary;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.enums.ApplyStatusEnum;
import com.example.studentarchives.enums.AttachmentBizTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.*;
import com.example.studentarchives.service.Fmy.CommonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 成果墙服务
 * <p>
 * 成果墙以 archives 主表为数据源，按 archive_type 映射到前端分类，
 * 详情补充字段从 archive_* 扩展表读取，附件来自 file_uploads（biz_type=archive）。
 * <p>
 * 说明：当前仓库没有独立的成果表，成果墙是档案数据的只读聚合视图。
 * 学生自传证书（POST）与材料完整性判定不在本次实现范围内。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementService {

    private static final String BIZ_TYPE_ARCHIVE = AttachmentBizTypeEnum.ARCHIVE.getValue();

    private static final DateTimeFormatter ISO_DATETIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private static final String CATEGORY_HONOR = "honor";
    private static final String CATEGORY_CONTEST = "contest";
    private static final String CATEGORY_PRACTICE = "practice";
    private static final String CATEGORY_RESEARCH = "research";
    private static final String CATEGORY_COMPLETION = "completion";
    private static final String CATEGORY_OTHER = "other";

    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            CATEGORY_HONOR, "荣誉",
            CATEGORY_CONTEST, "竞赛",
            CATEGORY_PRACTICE, "实践",
            CATEGORY_RESEARCH, "科研",
            CATEGORY_COMPLETION, "结课",
            CATEGORY_OTHER, "其他"
    );

    private static final Map<String, String> MATERIAL_LABELS = Map.of(
            "archived", "已归档",
            "missing", "缺材料",
            "pending", "待审核"
    );

    private final ArchiveRepository archiveRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;
    private final SemesterRepository semesterRepository;
    private final UserRepository userRepository;
    private final DictionaryRepository dictionaryRepository;
    private final CommonService commonService;

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

    // ==================== 列表 ====================

    @Transactional(readOnly = true)
    public AchievementListResponse list(String category, String status, String tag, Long semesterId,
                                        String keyword, String sort, int page, int perPage, Long userId) {
        List<Archive> all = archiveRepository.findByUserId(userId);

        int safePage = Math.max(page, 1);
        int safePerPage = Math.min(Math.max(perPage, 1), 50);

        List<Archive> filtered = all.stream()
                .filter(a -> matchesCategory(a, category))
                .filter(a -> matchesStatus(a, status))
                .filter(a -> matchesTag(a, tag))
                .filter(a -> semesterId == null || semesterId.equals(a.getSemesterId()))
                .filter(a -> matchesKeyword(a, keyword))
                .sorted(comparatorOf(sort))
                .toList();

        int total = filtered.size();
        int totalPages = total == 0 ? 0 : (total + safePerPage - 1) / safePerPage;
        int from = Math.min((safePage - 1) * safePerPage, total);
        int to = Math.min(from + safePerPage, total);
        List<Archive> pageItems = filtered.subList(from, to);

        ChildData children = loadChildren(pageItems.stream().map(Archive::getId).toList());
        Map<Long, Integer> fileCounts = loadFileCounts(pageItems.stream().map(Archive::getId).toList());
        Map<Long, String> semesterNames = loadSemesterNames(all);
        DictCache dict = new DictCache();

        List<AchievementListItemResponse> items = pageItems.stream()
                .map(a -> toListItem(a, children, fileCounts, semesterNames, dict))
                .toList();

        return AchievementListResponse.builder()
                .list(items)
                .counts(buildCounts(all))
                .statusCounts(buildStatusCounts(all))
                .pagination(AchievementListResponse.Pagination.builder()
                        .page(safePage)
                        .perPage(safePerPage)
                        .total(total)
                        .totalPages(totalPages)
                        .build())
                .build();
    }

    // ==================== 详情 ====================

    @Transactional(readOnly = true)
    public AchievementDetailResponse detail(String id, Long userId) {
        Archive archive = findOwnedArchive(id, userId);
        ChildData children = loadChildren(List.of(archive.getId()));
        Map<Long, String> semesterNames = loadSemesterNames(List.of(archive));
        Extra extra = extraOf(archive, children, new DictCache());

        List<AttachmentRelation> files = attachmentRelationRepository
                .findByBizTypeAndBizId(BIZ_TYPE_ARCHIVE, archive.getId());

        String semesterId = archive.getSemesterId() == null ? null : String.valueOf(archive.getSemesterId());
        String semesterName = semesterNames.get(archive.getSemesterId());

        List<AchievementDetailResponse.FieldInfo> fields = buildDetailFields(archive, extra, semesterName);

        return AchievementDetailResponse.builder()
                .id(String.valueOf(archive.getId()))
                .title(archive.getTitle())
                .category(categoryOf(archive.getArchiveType()))
                .categoryLabel(CATEGORY_LABELS.get(categoryOf(archive.getArchiveType())))
                .level(extra.level)
                .issuer(extra.issuer)
                .eventAt(formatDate(extra.eventDate != null ? extra.eventDate : archive.getObtainedAt()))
                .description(extra.description)
                .semester(semesterId == null ? null : AchievementDetailResponse.SemesterInfo.builder()
                        .id(semesterId)
                        .name(nvl(semesterName))
                        .build())
                .materialStatus(materialStatusOf(archive))
                .materialLabel(MATERIAL_LABELS.get(materialStatusOf(archive)))
                .attachments(files.stream().map(this::toAttachment).toList())
                .fields(fields)
                .audit(buildAudit(archive))
                .updatedAt(formatDateTime(archive.getUpdatedAt()))
                .tags(tagsOf(archive.getArchiveType()))
                .version(versionOf(archive))
                .build();
    }

    // ==================== 下载凭证 ====================

    @Transactional
    public AchievementDownloadTicketResponse downloadTicket(String achievementId, String fileId, Long userId) {
        Archive archive = findOwnedArchive(achievementId, userId);

        long attachmentId;
        try {
            attachmentId = Long.parseLong(fileId);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "文件不存在");
        }

        AttachmentRelation attachment = attachmentRelationRepository.findByIdAndUserId(attachmentId, userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "文件不存在"));
        if (!BIZ_TYPE_ARCHIVE.equals(attachment.getBizType())
                || !Objects.equals(attachment.getBizId(), archive.getId())) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "文件不存在");
        }

        String url = commonService.downloadFile(attachment.getId(), userId);

        return AchievementDownloadTicketResponse.builder()
                .url(url)
                .fileName(composeDownloadName(userId, archive, attachment))
                .mimeType(attachment.getMimeType())
                .size(attachment.getFileSize())
                .expiresAt(LocalDateTime.now().plusMinutes(10)
                        .atZone(ZoneId.systemDefault())
                        .format(ISO_WITH_ZONE))
                .build();
    }

    // ==================== 映射与组装 ====================

    private AchievementListItemResponse toListItem(Archive archive, ChildData children,
                                                   Map<Long, Integer> fileCounts, Map<Long, String> semesterNames,
                                                   DictCache dict) {
        Extra extra = extraOf(archive, children, dict);
        String category = categoryOf(archive.getArchiveType());
        String materialStatus = materialStatusOf(archive);
        return AchievementListItemResponse.builder()
                .id(String.valueOf(archive.getId()))
                .title(archive.getTitle())
                .category(category)
                .categoryLabel(CATEGORY_LABELS.get(category))
                .level(extra.level)
                .issuer(extra.issuer)
                .eventAt(formatDate(extra.eventDate != null ? extra.eventDate : archive.getObtainedAt()))
                .thumbnailUrl(null)
                .fileCount(fileCounts.getOrDefault(archive.getId(), 0))
                .materialStatus(materialStatus)
                .materialLabel(MATERIAL_LABELS.get(materialStatus))
                .sourceType(archive.getArchiveType())
                .sourceId(archive.getId())
                .updatedAt(formatDateTime(archive.getUpdatedAt()))
                .tags(tagsOf(archive.getArchiveType()))
                .version(versionOf(archive))
                .build();
    }

    /**
     * 档案主表没有等级/单位/描述字段，这些信息在 archive_* 扩展表里。
     * 扩展表缺失（历史种子数据）时保持空串，不用标题猜测填充。
     */
    private Extra extraOf(Archive archive, ChildData c, DictCache dict) {
        Extra extra = new Extra();
        Long id = archive.getId();
        switch (normalizeType(archive.getArchiveType())) {
            case "competition" -> {
                ArchiveCompetition x = c.competition.get(id);
                if (x != null) {
                    extra.level = dict.label(x.getAwardLevel(), "award_level");
                    put(extra, "竞赛类型", dict.label(x.getCompetitionType(), "competition_type"));
                    put(extra, "参赛角色", dict.label(x.getParticipantRole(), "participant_role"));
                }
            }
            case "scholarship" -> {
                ArchiveScholarship x = c.scholarship.get(id);
                if (x != null) {
                    extra.level = dict.label(x.getAwardLevel(), "scholarship_award_level", "award_level");
                    put(extra, "奖学金类别", dict.label(x.getScholarshipCategory(), "scholarship_level"));
                }
            }
            case "certificate" -> {
                ArchiveCertificate x = c.certificate.get(id);
                if (x != null) {
                    extra.issuer = nvl(x.getIssuingUnit());
                    put(extra, "证书类型", dict.label(x.getCertificateType(), "certificate_type"));
                    put(extra, "证书编号", x.getCertificateNo());
                }
            }
            case "research" -> {
                ArchiveResearch x = c.research.get(id);
                if (x != null) {
                    extra.level = dict.label(x.getProjectLevel(), "project_level");
                    put(extra, "项目类型", dict.label(x.getProjectType(), "research_type"));
                    put(extra, "参与角色", dict.label(x.getParticipantRole(), "participant_role"));
                }
            }
            case "social_practice" -> {
                ArchiveSocialPractice x = c.socialPractice.get(id);
                if (x != null) {
                    extra.issuer = nvl(x.getPracticeUnit());
                    if (x.getStartDate() != null) extra.eventDate = x.getStartDate();
                    put(extra, "实践地点", x.getPracticeLocation());
                    put(extra, "参与角色", dict.label(x.getParticipantRole(), "participant_role"));
                    if (x.getVolunteerHours() != null) {
                        put(extra, "志愿时长", x.getVolunteerHours().stripTrailingZeros().toPlainString() + " 小时");
                    }
                }
            }
            case "internship" -> {
                ArchiveInternship x = c.internship.get(id);
                if (x != null) {
                    extra.issuer = nvl(x.getCompanyName());
                    if (x.getStartDate() != null) extra.eventDate = x.getStartDate();
                    put(extra, "实习岗位", x.getPosition());
                    put(extra, "实习地点", x.getLocation());
                }
            }
            case "organization" -> {
                ArchiveOrganization x = c.organization.get(id);
                if (x != null) {
                    extra.level = dict.label(x.getOrgLevel(), "organization_level");
                    put(extra, "部门", x.getDepartment());
                    put(extra, "职务", x.getPositionTitle());
                }
            }
            case "training" -> {
                ArchiveTrainingProject x = c.training.get(id);
                if (x != null) {
                    extra.description = nvl(x.getProjectContent());
                    if (x.getStartDate() != null) extra.eventDate = x.getStartDate();
                }
            }
            case "book_review" -> {
                ArchiveBookReview x = c.bookReview.get(id);
                if (x != null) {
                    extra.description = nvl(x.getReviewContent());
                    if (x.getReadMonth() != null) extra.eventDate = x.getReadMonth();
                    put(extra, "书名", x.getBookName());
                }
            }
            case "innovation" -> {
                ArchiveInnovation x = c.innovation.get(id);
                if (x != null) {
                    extra.issuer = nvl(x.getCompanyName());
                    if (x.getRegisteredAt() != null) extra.eventDate = x.getRegisteredAt();
                    put(extra, "行业类型", dict.label(x.getIndustryType(), "industry_type"));
                    put(extra, "项目类型", dict.label(x.getProjectType(), "company_type"));
                    put(extra, "参与角色", dict.label(x.getParticipantRole(), "participant_role"));
                }
            }
            default -> {
                // 未知类型不猜测字段
            }
        }
        return extra;
    }

    private List<AchievementDetailResponse.FieldInfo> buildDetailFields(Archive archive, Extra extra,
                                                                        String semesterName) {
        List<AchievementDetailResponse.FieldInfo> fields = new ArrayList<>();
        fields.add(field("类型", CATEGORY_LABELS.get(categoryOf(archive.getArchiveType()))));
        addField(fields, "等级", extra.level);
        addField(fields, "颁发单位", extra.issuer);
        addField(fields, "学期", semesterName);
        extra.extraFields.forEach((label, value) -> addField(fields, label, value));
        return fields;
    }

    private AchievementDetailResponse.AuditInfo buildAudit(Archive archive) {
        Integer status = archive.getStatus();
        if (status == null) return null;
        ApplyStatusEnum statusEnum = ApplyStatusEnum.of(status);
        boolean hasFlow = archive.getAuditInfo() != null && archive.getAuditInfo().getSubmittedAt() != null;
        if (statusEnum == ApplyStatusEnum.DRAFT && !hasFlow) return null;

        Long auditorId = archive.getAuditInfo() == null ? null : archive.getAuditInfo().getAuditorId();
        String reviewerName = auditorId == null ? ""
                : userRepository.findById(auditorId).map(User::getName).orElse("");
        LocalDateTime reviewedAt = archive.getAuditInfo() == null ? null : archive.getAuditInfo().getAuditedAt();

        return AchievementDetailResponse.AuditInfo.builder()
                .status(statusEnum.name().toLowerCase(Locale.ROOT))
                .statusLabel(statusEnum.getLabel())
                .reviewedAt(formatDateTime(reviewedAt))
                .reviewerName(reviewerName)
                .build();
    }

    private AchievementDetailResponse.AttachmentInfo toAttachment(AttachmentRelation file) {
        String url = "/api/v1/files/" + file.getId() + "/download";
        return AchievementDetailResponse.AttachmentInfo.builder()
                .fileId(String.valueOf(file.getId()))
                .fileName(file.getOriginalName())
                .mimeType(file.getMimeType())
                .size(file.getFileSize())
                .url(url)
                .previewUrl(null)
                .uploadedAt(formatDateTime(file.getCreatedAt()))
                .isPrimary(file.getSortOrder() != null && file.getSortOrder() == 0)
                .sortOrder(file.getSortOrder())
                .build();
    }

    // ==================== 查询辅助 ====================

    private Archive findOwnedArchive(String id, Long userId) {
        long archiveId;
        try {
            archiveId = Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "成果不存在或已删除");
        }
        return archiveRepository.findById(archiveId)
                .filter(a -> Objects.equals(a.getUserId(), userId))
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "成果不存在或已删除"));
    }

    private Map<Long, Integer> loadFileCounts(List<Long> archiveIds) {
        if (archiveIds.isEmpty()) return Map.of();
        return attachmentRelationRepository.findByBizTypeAndBizIdIn(BIZ_TYPE_ARCHIVE, archiveIds)
                .stream()
                .collect(Collectors.groupingBy(AttachmentRelation::getBizId, Collectors.summingInt(f -> 1)));
    }

    private Map<Long, String> loadSemesterNames(Collection<Archive> archives) {
        Set<Long> ids = archives.stream()
                .map(Archive::getSemesterId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (ids.isEmpty()) return Map.of();
        return semesterRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(Semester::getId, Semester::getName));
    }

    private ChildData loadChildren(List<Long> archiveIds) {
        ChildData data = new ChildData();
        if (archiveIds.isEmpty()) return data;
        List<Long> ids = new ArrayList<>(archiveIds);

        data.competition = index(competitionRepository.findByArchiveIdIn(ids), ArchiveCompetition::getArchiveId);
        data.scholarship = index(scholarshipRepository.findByArchiveIdIn(ids), ArchiveScholarship::getArchiveId);
        data.innovation = index(innovationRepository.findByArchiveIdIn(ids), ArchiveInnovation::getArchiveId);
        data.research = index(researchRepository.findByArchiveIdIn(ids), ArchiveResearch::getArchiveId);
        data.certificate = index(certificateRepository.findByArchiveIdIn(ids), ArchiveCertificate::getArchiveId);
        data.organization = index(organizationRepository.findByArchiveIdIn(ids), ArchiveOrganization::getArchiveId);
        data.socialPractice = index(socialPracticeRepository.findByArchiveIdIn(ids), ArchiveSocialPractice::getArchiveId);

        // 以下三张扩展表 Repository 未提供批量查询，按当前页 ID 逐条查询（分页上限 50，可接受）
        for (Long id : ids) {
            internshipRepository.findByArchiveId(id).ifPresent(v -> data.internship.put(id, v));
            trainingProjectRepository.findByArchiveId(id).ifPresent(v -> data.training.put(id, v));
            bookReviewRepository.findByArchiveId(id).ifPresent(v -> data.bookReview.put(id, v));
        }
        return data;
    }

    private static <T> Map<Long, T> index(List<T> rows, Function<T, Long> key) {
        return rows.stream().collect(Collectors.toMap(key, Function.identity()));
    }

    // ==================== 过滤与排序 ====================

    private boolean matchesCategory(Archive archive, String category) {
        if (category == null || category.isBlank() || "all".equals(category)) return true;
        return categoryOf(archive.getArchiveType()).equals(category);
    }

    private boolean matchesStatus(Archive archive, String status) {
        if (status == null || status.isBlank() || "all".equals(status)) return true;
        return materialStatusOf(archive).equals(status);
    }

    private boolean matchesTag(Archive archive, String tag) {
        if (tag == null || tag.isBlank()) return true;
        return tagsOf(archive.getArchiveType()).contains(tag);
    }

    private boolean matchesKeyword(Archive archive, String keyword) {
        if (keyword == null || keyword.isBlank()) return true;
        String title = archive.getTitle();
        return title != null && title.toLowerCase(Locale.ROOT).contains(keyword.trim().toLowerCase(Locale.ROOT));
    }

    private Comparator<Archive> comparatorOf(String sort) {
        if ("date_asc".equals(sort)) {
            return (a, b) -> {
                LocalDate da = eventDateOf(a);
                LocalDate db = eventDateOf(b);
                if (da == null && db == null) return Long.compare(a.getId(), b.getId());
                if (da == null) return 1;
                if (db == null) return -1;
                int cmp = da.compareTo(db);
                return cmp != 0 ? cmp : Long.compare(a.getId(), b.getId());
            };
        }
        if ("updated_desc".equals(sort)) {
            return Comparator.comparing(Archive::getUpdatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed()
                    .thenComparing(Archive::getId, Comparator.reverseOrder());
        }
        return (a, b) -> {
            LocalDate da = eventDateOf(a);
            LocalDate db = eventDateOf(b);
            if (da == null && db == null) return Long.compare(b.getId(), a.getId());
            if (da == null) return 1;
            if (db == null) return -1;
            int cmp = db.compareTo(da);
            return cmp != 0 ? cmp : Long.compare(b.getId(), a.getId());
        };
    }

    private static LocalDate eventDateOf(Archive archive) {
        return archive.getObtainedAt();
    }

    // ==================== 分类 / 状态 / 标签 ====================

    /** 兼容种子数据的短编码与 ArchiveTypeEnum 的完整编码 */
    private static String normalizeType(String type) {
        if (type == null) return "";
        return switch (type) {
            case "academic_competition" -> "competition";
            case "academic_research" -> "research";
            case "honor_certificate" -> "certificate";
            case "training_project" -> "training";
            case "innovation_entrepreneurship" -> "innovation";
            default -> type;
        };
    }

    private static String categoryOf(String type) {
        return switch (normalizeType(type)) {
            case "competition" -> CATEGORY_CONTEST;
            case "scholarship", "certificate" -> CATEGORY_HONOR;
            case "research" -> CATEGORY_RESEARCH;
            case "social_practice", "internship", "organization" -> CATEGORY_PRACTICE;
            case "training" -> CATEGORY_COMPLETION;
            default -> CATEGORY_OTHER;
        };
    }

    /**
     * 材料状态由审批状态推导：
     * 已通过视为材料完整（已归档）；已退回即材料不齐；其余在流程中视为待审核。
     */
    private static String materialStatusOf(Archive archive) {
        Integer status = archive.getStatus();
        if (status != null && status == ApplyStatusEnum.APPROVED.getValue()) return "archived";
        if (status != null && status == ApplyStatusEnum.REJECTED.getValue()) return "missing";
        return "pending";
    }

    private static List<String> tagsOf(String type) {
        return switch (normalizeType(type)) {
            case "social_practice" -> List.of("social_practice");
            case "internship" -> List.of("internship");
            case "organization", "training" -> List.of("activity");
            default -> List.of();
        };
    }

    private static Integer versionOf(Archive archive) {
        if (archive.getAuditInfo() == null || archive.getAuditInfo().getCurrentVersion() == null) return 1;
        return archive.getAuditInfo().getCurrentVersion();
    }

    // ==================== 统计 ====================

    private AchievementListResponse.Counts buildCounts(List<Archive> archives) {
        long honor = 0, contest = 0, practice = 0, research = 0, completion = 0, other = 0;
        for (Archive a : archives) {
            switch (categoryOf(a.getArchiveType())) {
                case CATEGORY_HONOR -> honor++;
                case CATEGORY_CONTEST -> contest++;
                case CATEGORY_PRACTICE -> practice++;
                case CATEGORY_RESEARCH -> research++;
                case CATEGORY_COMPLETION -> completion++;
                default -> other++;
            }
        }
        return AchievementListResponse.Counts.builder()
                .all(archives.size())
                .honor(honor)
                .contest(contest)
                .practice(practice)
                .research(research)
                .completion(completion)
                .other(other)
                .build();
    }

    private AchievementListResponse.StatusCounts buildStatusCounts(List<Archive> archives) {
        long archived = 0, missing = 0, pending = 0;
        for (Archive a : archives) {
            switch (materialStatusOf(a)) {
                case "archived" -> archived++;
                case "missing" -> missing++;
                default -> pending++;
            }
        }
        return AchievementListResponse.StatusCounts.builder()
                .all(archives.size())
                .archived(archived)
                .missing(missing)
                .pending(pending)
                .build();
    }

    // ==================== 格式化 ====================

    private static String formatDate(LocalDate date) {
        return date == null ? "" : date.toString();
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? "" : value.format(ISO_DATETIME);
    }

    private static String nvl(String value) {
        return value == null ? "" : value;
    }

    private static void put(Extra extra, String label, String value) {
        if (value != null && !value.isBlank()) {
            extra.extraFields.put(label, value);
        }
    }

    private static AchievementDetailResponse.FieldInfo field(String label, String value) {
        return AchievementDetailResponse.FieldInfo.builder().label(label).value(nvl(value)).build();
    }

    private static void addField(List<AchievementDetailResponse.FieldInfo> fields, String label, String value) {
        if (value != null && !value.isBlank()) {
            fields.add(field(label, value));
        }
    }

    private String composeDownloadName(Long userId, Archive archive, AttachmentRelation attachment) {
        String userNo = userRepository.findById(userId).map(User::getUserNo).orElse("");
        String date = archive.getObtainedAt() == null ? "" : archive.getObtainedAt().toString().replace("-", "");
        String ext = "";
        String original = attachment.getOriginalName();
        int dot = original == null ? -1 : original.lastIndexOf('.');
        if (dot >= 0) ext = original.substring(dot);
        String name = (userNo + "_" + archive.getTitle() + "_" + date).replaceAll("[\\\\/:*?\"<>|]", "_");
        return name + ext;
    }

    // ==================== 内部结构 ====================

    /**
     * 单次请求内的字典编码 → 显示名缓存。
     * 按 dictTypes 顺序查找，字典未命中时回退原始值（兼容自由文本，如自定义参与角色）。
     */
    private final class DictCache {
        private final Map<String, Map<String, String>> cache = new HashMap<>();

        private String label(String code, String... dictTypes) {
            if (code == null || code.isBlank()) return nvl(code);
            for (String type : dictTypes) {
                Map<String, String> map = cache.computeIfAbsent(type, t ->
                        dictionaryRepository.findActiveByDictType(t).stream()
                                .collect(Collectors.toMap(Dictionary::getDictCode, Dictionary::getDictName, (a, b) -> a)));
                String name = map.get(code);
                if (name != null) return name;
            }
            return code;
        }
    }

    private static final class Extra {
        private String level = "";
        private String issuer = "";
        private String description = "";
        private LocalDate eventDate;
        private final Map<String, String> extraFields = new LinkedHashMap<>();
    }

    private static final class ChildData {
        private Map<Long, ArchiveCompetition> competition = Map.of();
        private Map<Long, ArchiveScholarship> scholarship = Map.of();
        private Map<Long, ArchiveInnovation> innovation = Map.of();
        private Map<Long, ArchiveResearch> research = Map.of();
        private Map<Long, ArchiveCertificate> certificate = Map.of();
        private Map<Long, ArchiveInternship> internship = new HashMap<>();
        private Map<Long, ArchiveOrganization> organization = Map.of();
        private Map<Long, ArchiveTrainingProject> training = new HashMap<>();
        private Map<Long, ArchiveSocialPractice> socialPractice = Map.of();
        private Map<Long, ArchiveBookReview> bookReview = new HashMap<>();
    }
}
