package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.archive.response.ArchiveAdminDetailResponse;
import com.example.studentarchives.dto.Fmy.archive.response.ArchiveAdminListItem;
import com.example.studentarchives.dto.Fmy.archive.response.ArchiveOverviewResponse;
import com.example.studentarchives.dto.Fmy.archive.response.ArchiveOverviewRow;
import com.example.studentarchives.dto.Fmy.archive.response.ArchiveStudentInfo;
import com.example.studentarchives.dto.Fmy.archive.response.ArchiveTypeCountItem;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.archive.ArchiveBookReview;
import com.example.studentarchives.entity.archive.ArchiveCertificate;
import com.example.studentarchives.entity.archive.ArchiveCompetition;
import com.example.studentarchives.entity.archive.ArchiveInnovation;
import com.example.studentarchives.entity.archive.ArchiveInternship;
import com.example.studentarchives.entity.archive.ArchiveOrganization;
import com.example.studentarchives.entity.archive.ArchiveResearch;
import com.example.studentarchives.entity.archive.ArchiveScholarship;
import com.example.studentarchives.entity.archive.ArchiveSocialPractice;
import com.example.studentarchives.entity.archive.ArchiveTrainingProject;
import com.example.studentarchives.entity.foundation.ArchiveTypeConfig;
import com.example.studentarchives.entity.foundation.Dictionary;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.College;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.org.School;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.enums.ArchiveTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AdminArchiveRepository;
import com.example.studentarchives.repository.AdminCollegeRepository;
import com.example.studentarchives.repository.ArchiveBookReviewRepository;
import com.example.studentarchives.repository.ArchiveCertificateRepository;
import com.example.studentarchives.repository.ArchiveCompetitionRepository;
import com.example.studentarchives.repository.ArchiveInnovationRepository;
import com.example.studentarchives.repository.ArchiveInternshipRepository;
import com.example.studentarchives.repository.ArchiveOrganizationRepository;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.ArchiveResearchRepository;
import com.example.studentarchives.repository.ArchiveScholarshipRepository;
import com.example.studentarchives.repository.ArchiveSocialPracticeRepository;
import com.example.studentarchives.repository.ArchiveTrainingProjectRepository;
import com.example.studentarchives.repository.ArchiveTypeConfigRepository;
import com.example.studentarchives.repository.AttachmentRelationRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.CollegeRepository;
import com.example.studentarchives.repository.DictionaryRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.SchoolRepository;
import com.example.studentarchives.repository.SemesterRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端档案管理服务
 * <p>
 * 对应《管理端接口文档》十五、档案管理模块（15.1 档案列表 / 15.2 档案详情 /
 * 15.3 组织档案汇总），统一权限码 {@code archive:view}。
 * <ul>
 *   <li>15.1 按组织维度（年级/学院/专业/班级）下钻 + 档案类型/状态/学期/关键词分页查询，
 *       组织筛选口径：classId → majorId → collegeId → grade 多条件取交集；管理员全校可见。</li>
 *   <li>15.2 档案基表 + 学生信息 + 该类型扩展表业务字段（字典标签） + 佐证文件。</li>
 *   <li>15.3 按组织维度实时聚合档案总数与状态分布、类型分布（大数据量时建议接
 *       org_archive_summaries / statistics_cache，本实现为实时聚合兜底）。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminArchiveService {

    /** 查看档案权限码（《管理端接口文档》关键权限码） */
    private static final String ARCHIVE_PERMISSION = "archive:view";

    /** 档案状态：0=草稿 1=待审批 2=通过 3=已退回 4=已撤销 */
    private static final int STATUS_DRAFT = 0;
    private static final int STATUS_PENDING = 1;
    private static final int STATUS_APPROVED = 2;
    private static final int STATUS_REJECTED = 3;
    private static final int STATUS_REVOKED = 4;

    /** 组织维度：1=学校 2=学院 3=专业 4=班级 6=年级 */
    private static final int ORG_SCHOOL = 1;
    private static final int ORG_COLLEGE = 2;
    private static final int ORG_MAJOR = 3;
    private static final int ORG_CLASS = 4;
    private static final int ORG_GRADE = 6;

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final AdminAuthService adminAuthService;
    private final AdminArchiveRepository adminArchiveRepository;
    private final ArchiveRepository archiveRepository;
    private final UserRepository userRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final CollegeRepository collegeRepository;
    private final AdminCollegeRepository adminCollegeRepository;
    private final SemesterRepository semesterRepository;
    private final ArchiveTypeConfigRepository archiveTypeConfigRepository;
    private final AttachmentRelationRepository attachmentRelationRepository;
    private final DictionaryRepository dictionaryRepository;
    private final SchoolRepository schoolRepository;
    private final OssFileService ossFileService;
    private final ArchiveCompetitionRepository archiveCompetitionRepository;
    private final ArchiveInnovationRepository archiveInnovationRepository;
    private final ArchiveResearchRepository archiveResearchRepository;
    private final ArchiveScholarshipRepository archiveScholarshipRepository;
    private final ArchiveCertificateRepository archiveCertificateRepository;
    private final ArchiveInternshipRepository archiveInternshipRepository;
    private final ArchiveOrganizationRepository archiveOrganizationRepository;
    private final ArchiveTrainingProjectRepository archiveTrainingProjectRepository;
    private final ArchiveSocialPracticeRepository archiveSocialPracticeRepository;
    private final ArchiveBookReviewRepository archiveBookReviewRepository;

    // ==================== 15.1 学生档案列表 ====================

    /**
     * 获取学生档案列表（GET /admin/archives，文档 15.1）
     * <p>
     * 按组织维度筛选学生集合（classId → majorId → collegeId → grade 取交集），
     * 再按档案类型/状态/学期/关键词（学生姓名/学号/档案标题）分页查询，按 id 倒序。
     *
     * @param userId      当前登录用户 ID
     * @param grade       年级筛选（可选）
     * @param collegeId   学院 ID（可选）
     * @param majorId     专业 ID（可选）
     * @param classId     班级 ID（可选）
     * @param archiveType 档案类型编码（可选）
     * @param status      档案状态（可选，0-4）
     * @param semesterId  学期 ID（可选）
     * @param keyword     关键词（姓名/学号/档案标题，可选）
     * @param pageParam   分页参数
     * @return 分页档案列表
     */
    public PageResult<ArchiveAdminListItem> listArchives(Long userId, String grade, Long collegeId, Long majorId,
                                                         Long classId, String archiveType, Integer status,
                                                         Long semesterId, String keyword, PageParam pageParam) {
        adminAuthService.requireAdminOrPermission(userId, ARCHIVE_PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        List<Long> orgUserIds = resolveStudentIds(schoolId, grade, collegeId, majorId, classId);
        if (orgUserIds.isEmpty()) {
            return PageResult.empty();
        }

        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        List<Long> keywordUserIds = List.of();
        if (kw != null) {
            keywordUserIds = userRepository.findByNameContainingOrUserNoContaining(kw, kw).stream()
                    .map(User::getId).toList();
        }
        List<Long> kwUserIds = keywordUserIds;

        Specification<Archive> spec = (root, query, cb) -> {
            List<Predicate> preds = new ArrayList<>();
            preds.add(root.get("userId").in(orgUserIds));
            if (archiveType != null && !archiveType.isBlank()) {
                preds.add(cb.equal(root.get("archiveType"), archiveType));
            }
            if (status != null) {
                preds.add(cb.equal(root.get("status"), status));
            }
            if (semesterId != null) {
                preds.add(cb.equal(root.get("semesterId"), semesterId));
            }
            if (kw != null) {
                Predicate titleLike = cb.like(root.get("title"), "%" + escapeLike(kw) + "%", '\\');
                Predicate userIn = kwUserIds.isEmpty() ? cb.disjunction() : root.get("userId").in(kwUserIds);
                preds.add(cb.or(titleLike, userIn));
            }
            return cb.and(preds.toArray(new Predicate[0]));
        };

        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(),
                Sort.by(Sort.Direction.DESC, "id"));
        Page<Archive> page = adminArchiveRepository.findAll(spec, pageable);

        EnrichContext ctx = buildEnrichContext(page.getContent());
        List<ArchiveAdminListItem> items = page.getContent().stream()
                .map(a -> toListItem(a, ctx))
                .collect(Collectors.toList());
        return PageResult.of(items, page.getTotalElements(), pageParam);
    }

    // ==================== 15.2 档案详情 ====================

    /**
     * 获取档案详情（GET /admin/archives/{archiveId}，文档 15.2）
     * <p>
     * 返回档案基表字段、学生基础信息与组织归属、该类型扩展表业务字段（含字典标签）
     * 及佐证文件。按学校隔离校验。
     *
     * @param userId    当前登录用户 ID
     * @param archiveId 档案 ID
     * @return 档案详情
     */
    public ArchiveAdminDetailResponse archiveDetail(Long userId, Long archiveId) {
        adminAuthService.requireAdminOrPermission(userId, ARCHIVE_PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        Archive archive = adminArchiveRepository.findById(archiveId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "档案不存在"));
        if (!Objects.equals(archive.getSchoolId(), schoolId)) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "档案不存在");
        }

        User student = userRepository.findById(archive.getUserId()).orElse(null);
        StudentProfile profile = student == null ? null : studentProfileRepository.findByUserId(student.getId()).orElse(null);
        ArchiveStudentInfo studentInfo = buildStudentInfo(student, profile);

        String semesterName = archive.getSemesterId() == null ? null
                : semesterRepository.findById(archive.getSemesterId()).map(Semester::getName).orElse(null);
        String auditorName = null;
        if (archive.getAuditInfo() != null && archive.getAuditInfo().getAuditorId() != null) {
            auditorName = userRepository.findById(archive.getAuditInfo().getAuditorId())
                    .map(User::getName).orElse(null);
        }

        Map<String, Object> details = buildDetail(archive);

        return ArchiveAdminDetailResponse.builder()
                .archiveId(archive.getId())
                .archiveType(archive.getArchiveType())
                .archiveTypeName(resolveTypeName(archive.getArchiveType()))
                .title(archive.getTitle())
                .semesterId(archive.getSemesterId())
                .semesterName(semesterName)
                .obtainedAt(archive.getObtainedAt() == null ? null : DATE.format(archive.getObtainedAt()))
                .status(archive.getStatus())
                .statusLabel(statusLabel(archive.getStatus()))
                .rejectedReason(archive.getAuditInfo() == null ? null : archive.getAuditInfo().getRejectedReason())
                .submittedAt(toIso(archive.getAuditInfo() == null ? null : archive.getAuditInfo().getSubmittedAt()))
                .auditedAt(toIso(archive.getAuditInfo() == null ? null : archive.getAuditInfo().getAuditedAt()))
                .auditorName(auditorName)
                .student(studentInfo)
                .details(details)
                .build();
    }

    // ==================== 15.3 组织档案汇总 ====================

    /**
     * 获取组织档案汇总（GET /admin/archives/overview，文档 15.3）
     * <p>
     * 按组织维度（2=学院 3=专业 4=班级 6=年级）实时聚合各组织的档案总数与状态分布、
     * 类型分布；orgType 不传默认全校单条；orgId 下钻其下一级（如 orgType=2 + orgId=学院
     * 返回该学院下各专业汇总）。
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID（可选，不传取当前学期）
     * @param orgType    汇总维度（可选，2=学院 3=专业 4=班级 6=年级）
     * @param orgId      指定组织 ID（可选，下钻其下一级）
     * @param grade      年级筛选（可选）
     * @return 组织汇总行
     */
    public ArchiveOverviewResponse archiveOverview(Long userId, Long semesterId, Integer orgType, Long orgId, String grade) {
        adminAuthService.requireAdminOrPermission(userId, ARCHIVE_PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        if (semesterId == null) {
            semesterId = semesterRepository.findCurrentBySchoolId(schoolId).map(Semester::getId).orElse(null);
        }
        Long effSemesterId = semesterId;

        OrgIndex index = buildOrgIndex(schoolId);
        List<OrgScope> scopes = resolveScopes(schoolId, orgType, orgId, grade, index);
        List<ArchiveOverviewRow> rows = scopes.stream()
                .map(s -> aggregate(s, effSemesterId, index))
                .collect(Collectors.toList());
        return ArchiveOverviewResponse.builder()
                .orgType(resolveRowOrgType(orgType, orgId))
                .rows(rows)
                .build();
    }

    // ==================== 组织索引 ====================

    /** 全校组织索引：学院/专业/班级映射 + 班级学生集合 */
    private OrgIndex buildOrgIndex(Long schoolId) {
        List<College> colleges = adminCollegeRepository.findBySchoolId(schoolId);
        Map<Long, String> collegeIdToName = colleges.stream()
                .collect(Collectors.toMap(College::getId, College::getName, (a, b) -> a));

        List<Long> collegeIds = colleges.stream().map(College::getId).toList();
        List<Major> majors = collegeIds.isEmpty() ? List.of()
                : majorRepository.findByCollegeIdIn(collegeIds);
        Map<Long, Long> majorIdToCollegeId = majors.stream()
                .collect(Collectors.toMap(Major::getId, Major::getCollegeId, (a, b) -> a));
        Map<Long, String> majorIdToName = majors.stream()
                .collect(Collectors.toMap(Major::getId, Major::getName, (a, b) -> a));

        List<Long> majorIds = majors.stream().map(Major::getId).toList();
        List<Clazz> classes = majorIds.isEmpty() ? List.of()
                : clazzRepository.findByMajorIdIn(majorIds);
        Map<Long, Long> classIdToMajorId = classes.stream()
                .collect(Collectors.toMap(Clazz::getId, Clazz::getMajorId, (a, b) -> a));
        Map<Long, String> classIdToName = classes.stream()
                .collect(Collectors.toMap(Clazz::getId, Clazz::getName, (a, b) -> a));
        Map<Long, String> classIdToGrade = classes.stream()
                .filter(c -> c.getGrade() != null)
                .collect(Collectors.toMap(Clazz::getId, Clazz::getGrade, (a, b) -> a));

        List<Long> classIds = classes.stream().map(Clazz::getId).toList();
        List<StudentProfile> profiles = classIds.isEmpty() ? List.of()
                : studentProfileRepository.findByClassIdIn(classIds);
        Map<Long, List<Long>> userIdsByClassId = new LinkedHashMap<>();
        for (StudentProfile p : profiles) {
            userIdsByClassId.computeIfAbsent(p.getClassId(), k -> new ArrayList<>()).add(p.getUserId());
        }
        for (Map.Entry<Long, List<Long>> e : userIdsByClassId.entrySet()) {
            e.setValue(e.getValue().stream().distinct().toList());
        }

        return new OrgIndex(collegeIdToName, majorIdToCollegeId, majorIdToName,
                classIdToMajorId, classIdToName, classIdToGrade, userIdsByClassId);
    }

    /**
     * 解析各汇总行组织范围。
     * <p>
     * orgType 为当前汇总维度；orgId 提供时返回其下一级组织行（orgType=2+orgId=学院 →
     * 学院下各专业；orgType=3+orgId=专业 → 专业下各班级）。
     */
    private List<OrgScope> resolveScopes(Long schoolId, Integer orgType, Long orgId, String grade, OrgIndex index) {
        List<OrgScope> scopes = new ArrayList<>();
        if (orgType == null) {
            String schoolName = schoolRepository.findById(schoolId).map(School::getName).orElse("全校");
            List<Long> classIds = new ArrayList<>();
            for (Long classId : index.userIdsByClassId().keySet()) {
                if (grade != null && !grade.isBlank() && !grade.equals(index.classIdToGrade().get(classId))) {
                    continue;
                }
                classIds.add(classId);
            }
            scopes.add(new OrgScope(schoolId, schoolName, classIds));
            return scopes;
        }
        if (orgType == ORG_GRADE) {
            Map<String, List<Long>> byGrade = new LinkedHashMap<>();
            for (Map.Entry<Long, String> e : index.classIdToGrade().entrySet()) {
                if (grade != null && !grade.isBlank() && !grade.equals(e.getValue())) {
                    continue;
                }
                byGrade.computeIfAbsent(e.getValue(), k -> new ArrayList<>()).add(e.getKey());
            }
            byGrade.forEach((g, classIds) -> scopes.add(new OrgScope(0L, g, classIds)));
            return scopes;
        }
        if (orgType == ORG_COLLEGE && orgId != null) {
            // 学院下钻 → 该学院下各专业行
            for (Map.Entry<Long, String> e : index.majorIdToName().entrySet()) {
                if (!Objects.equals(index.majorIdToCollegeId().get(e.getKey()), orgId)) {
                    continue;
                }
                scopes.add(new OrgScope(e.getKey(), e.getValue(), classIdsOfMajor(e.getKey(), grade, index)));
            }
            return scopes;
        }
        if (orgType == ORG_MAJOR && orgId != null) {
            // 专业下钻 → 该专业下各班级行
            List<Long> classIds = classIdsOfMajor(orgId, grade, index);
            for (Long classId : classIds) {
                scopes.add(new OrgScope(classId, index.classIdToName().get(classId), List.of(classId)));
            }
            return scopes;
        }
        if (orgType == ORG_CLASS && orgId != null) {
            // 班级指定 → 该班级单行
            String name = index.classIdToName().get(orgId);
            scopes.add(new OrgScope(orgId, name, List.of(orgId)));
            return scopes;
        }

        // 无 orgId：返回该维度的全部组织行
        switch (orgType) {
            case ORG_COLLEGE -> index.collegeIdToName().forEach((cid, name) -> {
                List<Long> classIds = classIdsOfCollege(cid, grade, index);
                scopes.add(new OrgScope(cid, name, classIds));
            });
            case ORG_MAJOR -> index.majorIdToName().forEach((mid, name) -> {
                scopes.add(new OrgScope(mid, name, classIdsOfMajor(mid, grade, index)));
            });
            case ORG_CLASS -> index.classIdToName().forEach((classId, name) -> {
                if (grade == null || grade.isBlank() || grade.equals(index.classIdToGrade().get(classId))) {
                    scopes.add(new OrgScope(classId, name, List.of(classId)));
                }
            });
            default -> {
                // 未识别维度：返回空
            }
        }
        return scopes;
    }

    private List<Long> classIdsOfCollege(Long collegeId, String grade, OrgIndex index) {
        List<Long> result = new ArrayList<>();
        for (Map.Entry<Long, Long> e : index.classIdToMajorId().entrySet()) {
            Long majorId = e.getValue();
            if (majorId == null || !Objects.equals(index.majorIdToCollegeId().get(majorId), collegeId)) {
                continue;
            }
            if (grade != null && !grade.isBlank() && !grade.equals(index.classIdToGrade().get(e.getKey()))) {
                continue;
            }
            result.add(e.getKey());
        }
        return result;
    }

    private List<Long> classIdsOfMajor(Long majorId, String grade, OrgIndex index) {
        List<Long> result = new ArrayList<>();
        for (Map.Entry<Long, Long> e : index.classIdToMajorId().entrySet()) {
            if (!Objects.equals(e.getValue(), majorId)) {
                continue;
            }
            if (grade != null && !grade.isBlank() && !grade.equals(index.classIdToGrade().get(e.getKey()))) {
                continue;
            }
            result.add(e.getKey());
        }
        return result;
    }

    /** 汇总响应中的组织维度（orgId 下钻时行级维度下调一级） */
    private Integer resolveRowOrgType(Integer orgType, Long orgId) {
        if (orgType == null) {
            return ORG_SCHOOL;
        }
        if (orgId != null && orgType == ORG_COLLEGE) {
            return ORG_MAJOR;
        }
        if (orgId != null && orgType == ORG_MAJOR) {
            return ORG_CLASS;
        }
        return orgType;
    }

    /** 单组织行汇总（实时聚合 archives） */
    private ArchiveOverviewRow aggregate(OrgScope scope, Long semesterId, OrgIndex index) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (Long classId : scope.classIds()) {
            List<Long> classUsers = index.userIdsByClassId().getOrDefault(classId, List.of());
            userIds.addAll(classUsers);
        }
        List<Archive> archives = userIds.isEmpty() ? List.of()
                : archiveRepository.findByUserIdIn(userIds);

        int total = 0;
        int submitted = 0;
        int approved = 0;
        int pending = 0;
        int rejected = 0;
        int draft = 0;
        int revoked = 0;
        Map<String, Integer> typeCount = new LinkedHashMap<>();
        for (Archive a : archives) {
            if (semesterId != null && a.getSemesterId() != null && !Objects.equals(a.getSemesterId(), semesterId)) {
                continue;
            }
            total++;
            int st = a.getStatus() == null ? STATUS_DRAFT : a.getStatus();
            switch (st) {
                case STATUS_PENDING -> pending++;
                case STATUS_APPROVED -> approved++;
                case STATUS_REJECTED -> rejected++;
                case STATUS_REVOKED -> revoked++;
                default -> draft++;
            }
            if (a.getAuditInfo() != null && a.getAuditInfo().getSubmittedAt() != null) {
                submitted++;
            }
            typeCount.merge(a.getArchiveType(), 1, Integer::sum);
        }
        List<ArchiveTypeCountItem> distribution = typeCount.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .map(e -> ArchiveTypeCountItem.builder().archiveType(e.getKey()).count(e.getValue()).build())
                .collect(Collectors.toList());

        return ArchiveOverviewRow.builder()
                .orgId(scope.orgId())
                .orgName(scope.orgName())
                .studentCount(userIds.size())
                .totalArchives(total)
                .submittedCount(submitted)
                .approvedCount(approved)
                .pendingCount(pending)
                .rejectedCount(rejected)
                .draftCount(draft)
                .revokedCount(revoked)
                .archiveTypeDistribution(distribution)
                .build();
    }

    // ==================== 列表上下文与映射 ====================

    /** 列表/详情共用的名称解析上下文（批量查一次避免 N+1） */
    private record EnrichContext(
            Map<Long, User> users,
            Map<Long, Long> userIdToClassId,
            Map<Long, Clazz> classes,
            Map<Long, Major> majors,
            Map<Long, College> colleges,
            Map<Long, Semester> semesters,
            Map<String, String> typeNames) {
    }

    private EnrichContext buildEnrichContext(List<Archive> archives) {
        Set<Long> userIds = archives.stream().map(Archive::getUserId).collect(Collectors.toSet());
        Map<Long, User> users = userIds.isEmpty() ? Map.of()
                : userRepository.findByIdIn(userIds).stream().collect(Collectors.toMap(User::getId, u -> u, (a, b) -> a));

        List<StudentProfile> profiles = userIds.isEmpty() ? List.of()
                : studentProfileRepository.findByUserIdIn(userIds);
        Map<Long, Long> userIdToClassId = profiles.stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, StudentProfile::getClassId, (a, b) -> a));

        Set<Long> classIds = new LinkedHashSet<>(userIdToClassId.values());
        Map<Long, Clazz> classes = classIds.isEmpty() ? Map.of()
                : clazzRepository.findByIdIn(classIds).stream().collect(Collectors.toMap(Clazz::getId, c -> c, (a, b) -> a));

        Set<Long> majorIds = classes.values().stream().map(Clazz::getMajorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Major> majors = majorIds.isEmpty() ? Map.of()
                : majorRepository.findByIdIn(majorIds).stream().collect(Collectors.toMap(Major::getId, m -> m, (a, b) -> a));

        Set<Long> collegeIds = majors.values().stream().map(Major::getCollegeId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, College> colleges = collegeIds.isEmpty() ? Map.of()
                : collegeRepository.findByIdIn(collegeIds).stream().collect(Collectors.toMap(College::getId, c -> c, (a, b) -> a));

        Set<Long> semesterIds = archives.stream().map(Archive::getSemesterId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Semester> semesters = semesterIds.isEmpty() ? Map.of()
                : semesterRepository.findAllById(semesterIds).stream().collect(Collectors.toMap(Semester::getId, s -> s, (a, b) -> a));

        return new EnrichContext(users, userIdToClassId, classes, majors, colleges, semesters, buildTypeNameMap());
    }

    private ArchiveAdminListItem toListItem(Archive a, EnrichContext ctx) {
        User user = ctx.users().get(a.getUserId());
        Long classId = ctx.userIdToClassId().get(a.getUserId());
        Clazz clazz = classId == null ? null : ctx.classes().get(classId);
        Major major = clazz == null || clazz.getMajorId() == null ? null : ctx.majors().get(clazz.getMajorId());
        College college = major == null || major.getCollegeId() == null ? null : ctx.colleges().get(major.getCollegeId());
        Semester semester = a.getSemesterId() == null ? null : ctx.semesters().get(a.getSemesterId());

        return ArchiveAdminListItem.builder()
                .archiveId(a.getId())
                .archiveType(a.getArchiveType())
                .archiveTypeName(resolveTypeName(a.getArchiveType()))
                .title(a.getTitle())
                .semesterId(a.getSemesterId())
                .semesterName(semester == null ? null : semester.getName())
                .obtainedAt(a.getObtainedAt() == null ? null : DATE.format(a.getObtainedAt()))
                .status(a.getStatus())
                .statusLabel(statusLabel(a.getStatus()))
                .userId(a.getUserId())
                .studentNo(user == null ? null : user.getUserNo())
                .studentName(user == null ? null : user.getName())
                .className(clazz == null ? null : clazz.getName())
                .majorName(major == null ? null : major.getName())
                .collegeName(college == null ? null : college.getName())
                .grade(clazz == null ? null : clazz.getGrade())
                .submittedAt(toIso(a.getAuditInfo() == null ? null : a.getAuditInfo().getSubmittedAt()))
                .build();
    }

    private ArchiveStudentInfo buildStudentInfo(User student, StudentProfile profile) {
        ArchiveStudentInfo info = new ArchiveStudentInfo();
        if (student == null) {
            return info;
        }
        info.setUserId(student.getId());
        info.setStudentNo(student.getUserNo());
        info.setName(student.getName());
        info.setGender(student.getGender());
        info.setGenderLabel(genderLabel(student.getGender()));

        Clazz clazz = profile == null || profile.getClassId() == null ? null
                : clazzRepository.findById(profile.getClassId()).orElse(null);
        if (clazz != null) {
            info.setClassName(clazz.getName());
            info.setGrade(clazz.getGrade());
            if (clazz.getMajorId() != null) {
                Major major = majorRepository.findById(clazz.getMajorId()).orElse(null);
                if (major != null) {
                    info.setMajorName(major.getName());
                    if (major.getCollegeId() != null) {
                        College college = collegeRepository.findById(major.getCollegeId()).orElse(null);
                        if (college != null) {
                            info.setCollegeName(college.getName());
                        }
                    }
                }
            }
        }
        return info;
    }

    // ==================== 详情构建（与移动端结构一致） ====================

    /** 构建档案类型扩展表字段映射（含字典标签），附加佐证文件 */
    private Map<String, Object> buildDetail(Archive a) {
        Map<String, Object> detail = new LinkedHashMap<>();
        String type = a.getArchiveType();
        if (type == null) {
            return detail;
        }
        if (a.getObtainedAt() != null) {
            detail.put("obtainTime", DATE.format(a.getObtainedAt()));
        }
        if (a.getCourseCode() != null) {
            detail.put("courseCode", a.getCourseCode());
        }
        switch (type) {
            case "academic_competition" -> archiveCompetitionRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("competitionName", ext.getCompetitionName());
                detail.put("competitionType", ext.getCompetitionType());
                detail.put("competitionTypeLabel", resolveLabel("competition_type", ext.getCompetitionType()));
                detail.put("awardLevel", ext.getAwardLevel());
                detail.put("awardLevelLabel", resolveLabel("award_level", ext.getAwardLevel()));
                detail.put("participantRole", ext.getParticipantRole());
            });
            case "scholarship" -> archiveScholarshipRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("scholarshipName", ext.getScholarshipName());
                detail.put("scholarshipCategory", ext.getScholarshipCategory());
                detail.put("scholarshipCategoryLabel", resolveLabel("scholarship_category", ext.getScholarshipCategory()));
                detail.put("awardLevel", ext.getAwardLevel());
                detail.put("awardLevelLabel", resolveLabel("award_level", ext.getAwardLevel()));
            });
            case "innovation_entrepreneurship" -> archiveInnovationRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("companyName", ext.getCompanyName());
                detail.put("industryType", ext.getIndustryType());
                detail.put("industryTypeLabel", resolveLabel("industry_type", ext.getIndustryType()));
                detail.put("projectType", ext.getProjectType());
                detail.put("projectTypeLabel", resolveLabel("project_type", ext.getProjectType()));
                detail.put("participantRole", ext.getParticipantRole());
                if (ext.getRegisteredAt() != null) {
                    detail.put("registeredAt", DATE.format(ext.getRegisteredAt()));
                }
            });
            case "academic_research" -> archiveResearchRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("projectName", ext.getProjectName());
                detail.put("projectLevel", ext.getProjectLevel());
                detail.put("projectLevelLabel", resolveLabel("project_level", ext.getProjectLevel()));
                detail.put("projectType", ext.getProjectType());
                detail.put("projectTypeLabel", resolveLabel("project_type", ext.getProjectType()));
                detail.put("participantRole", ext.getParticipantRole());
                if (ext.getStartDate() != null) {
                    detail.put("startDate", DATE.format(ext.getStartDate()));
                }
                if (ext.getEndDate() != null) {
                    detail.put("endDate", DATE.format(ext.getEndDate()));
                }
            });
            case "honor_certificate" -> archiveCertificateRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("certificateType", ext.getCertificateType());
                detail.put("certificateTypeLabel", resolveLabel("certificate_type", ext.getCertificateType()));
                detail.put("certificateName", ext.getCertificateName());
                detail.put("certificateNo", ext.getCertificateNo());
                detail.put("issuingUnit", ext.getIssuingUnit());
                if (ext.getValidUntil() != null) {
                    detail.put("validUntil", DATE.format(ext.getValidUntil()));
                }
            });
            case "internship" -> archiveInternshipRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("companyName", ext.getCompanyName());
                detail.put("location", ext.getLocation());
                detail.put("position", ext.getPosition());
                if (ext.getStartDate() != null) {
                    detail.put("startDate", DATE.format(ext.getStartDate()));
                }
                if (ext.getEndDate() != null) {
                    detail.put("endDate", DATE.format(ext.getEndDate()));
                }
            });
            case "organization" -> archiveOrganizationRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("orgLevel", ext.getOrgLevel());
                detail.put("orgLevelLabel", resolveLabel("org_level", ext.getOrgLevel()));
                detail.put("department", ext.getDepartment());
                detail.put("positionTitle", ext.getPositionTitle());
                if (ext.getStartDate() != null) {
                    detail.put("startDate", DATE.format(ext.getStartDate()));
                }
                if (ext.getEndDate() != null) {
                    detail.put("endDate", DATE.format(ext.getEndDate()));
                }
            });
            case "training_project" -> archiveTrainingProjectRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("projectName", ext.getProjectName());
                detail.put("projectContent", ext.getProjectContent());
                if (ext.getStartDate() != null) {
                    detail.put("startDate", DATE.format(ext.getStartDate()));
                }
                if (ext.getEndDate() != null) {
                    detail.put("endDate", DATE.format(ext.getEndDate()));
                }
            });
            case "social_practice" -> archiveSocialPracticeRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("activityName", ext.getActivityName());
                detail.put("practiceLocation", ext.getPracticeLocation());
                detail.put("practiceUnit", ext.getPracticeUnit());
                detail.put("participantRole", ext.getParticipantRole());
                if (ext.getStartDate() != null) {
                    detail.put("startDate", DATE.format(ext.getStartDate()));
                }
                if (ext.getEndDate() != null) {
                    detail.put("endDate", DATE.format(ext.getEndDate()));
                }
                if (ext.getVolunteerHours() != null) {
                    detail.put("volunteerHours", ext.getVolunteerHours());
                }
            });
            case "book_review" -> archiveBookReviewRepository.findByArchiveId(a.getId()).ifPresent(ext -> {
                detail.put("bookName", ext.getBookName());
                if (ext.getReadMonth() != null) {
                    detail.put("readMonth", DATE.format(ext.getReadMonth()));
                }
                detail.put("reviewContent", ext.getReviewContent());
            });
            default -> {
            }
        }

        List<Map<String, Object>> proofFiles = attachmentRelationRepository
                .findByBizTypeAndBizId("archive", a.getId()).stream()
                .map(f -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("fileId", f.getId());
                    m.put("fileName", f.getOriginalName());
                    m.put("fileUrl", ossFileService.getFileUrl(f.getFilePath(), f.getOriginalName()));
                    return m;
                })
                .collect(Collectors.toList());
        if (!proofFiles.isEmpty()) {
            detail.put("proofFiles", proofFiles);
        }
        return detail;
    }

    // ==================== 私有工具 ====================

    /** 解析学生集合（classId → majorId → collegeId → grade 取交集，全空则全校） */
    private List<Long> resolveStudentIds(Long schoolId, String grade, Long collegeId, Long majorId, Long classId) {
        List<Long> classIds = resolveClassIds(schoolId, grade, collegeId, majorId, classId);
        if (classIds.isEmpty()) {
            return List.of();
        }
        return studentProfileRepository.findByClassIdIn(classIds).stream()
                .map(StudentProfile::getUserId).distinct().toList();
    }

    private List<Long> resolveClassIds(Long schoolId, String grade, Long collegeId, Long majorId, Long classId) {
        if (classId != null) {
            return List.of(classId);
        }
        if (majorId != null) {
            return clazzRepository.findByMajorId(majorId).stream().map(Clazz::getId).toList();
        }
        if (collegeId != null) {
            List<Long> majorIds = majorRepository.findByCollegeIdIn(List.of(collegeId)).stream()
                    .map(Major::getId).toList();
            if (majorIds.isEmpty()) {
                return List.of();
            }
            return clazzRepository.findByMajorIdIn(majorIds).stream().map(Clazz::getId).toList();
        }
        if (grade != null && !grade.isBlank()) {
            return clazzRepository.findByGrade(grade).stream().map(Clazz::getId).toList();
        }
        // 全校：学校下学院 → 专业 → 班级
        List<Long> collegeIds = adminCollegeRepository.findBySchoolId(schoolId).stream()
                .map(College::getId).toList();
        if (collegeIds.isEmpty()) {
            return List.of();
        }
        List<Long> majorIds = majorRepository.findByCollegeIdIn(collegeIds).stream()
                .map(Major::getId).toList();
        if (majorIds.isEmpty()) {
            return List.of();
        }
        return clazzRepository.findByMajorIdIn(majorIds).stream().map(Clazz::getId).toList();
    }

    /** 档案类型名称：优先 archive_type_configs，回退 ArchiveTypeEnum */
    private Map<String, String> buildTypeNameMap() {
        Map<String, String> map = new HashMap<>();
        for (ArchiveTypeConfig c : archiveTypeConfigRepository.findAllActive()) {
            map.put(c.getArchiveType(), c.getTypeName());
        }
        for (ArchiveTypeEnum e : ArchiveTypeEnum.values()) {
            map.putIfAbsent(e.getValue(), e.getLabel());
        }
        return map;
    }

    private String resolveTypeName(String archiveType) {
        if (archiveType == null) {
            return null;
        }
        String name = buildTypeNameMap().get(archiveType);
        return name != null ? name : archiveType;
    }

    /** 字典标签解析（dict_type → dict_code → dict_name） */
    private String resolveLabel(String dictType, String dictCode) {
        if (dictType == null || dictCode == null) {
            return null;
        }
        return dictionaryRepository.findActiveByDictType(dictType).stream()
                .filter(d -> dictCode.equals(d.getDictCode()))
                .findFirst()
                .map(Dictionary::getDictName)
                .orElse(null);
    }

    private static String statusLabel(Integer status) {
        if (status == null) {
            return "未知";
        }
        return switch (status) {
            case 0 -> "草稿";
            case 1 -> "待审批";
            case 2 -> "通过";
            case 3 -> "已退回";
            case 4 -> "已撤销";
            default -> "未知";
        };
    }

    private static String genderLabel(Integer gender) {
        if (gender == null) {
            return "未知";
        }
        return switch (gender) {
            case 1 -> "男";
            case 2 -> "女";
            default -> "未知";
        };
    }

    private static String escapeLike(String s) {
        return s.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String toIso(LocalDateTime dt) {
        if (dt == null) {
            return null;
        }
        return dt.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE);
    }

    /** 组织索引（校级一次构建，跨行共享） */
    private record OrgIndex(
            Map<Long, String> collegeIdToName,
            Map<Long, Long> majorIdToCollegeId,
            Map<Long, String> majorIdToName,
            Map<Long, Long> classIdToMajorId,
            Map<Long, String> classIdToName,
            Map<Long, String> classIdToGrade,
            Map<Long, List<Long>> userIdsByClassId) {
    }

    /** 汇总行组织范围 */
    private record OrgScope(Long orgId, String orgName, List<Long> classIds) {
    }
}
