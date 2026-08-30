package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.dto.Fmy.profile.response.CareerPlanDetailResponse;
import com.example.studentarchives.dto.Fmy.profile.response.GrowthTimelineResponse;
import com.example.studentarchives.dto.Fmy.profile.response.ProfileInfoResponse;
import com.example.studentarchives.dto.Fmy.profile.response.TeacherStudentProfileResponse;
import com.example.studentarchives.dto.Fmy.profile.response.TeacherWeaknessItemResponse;
import com.example.studentarchives.entity.weakness.WeaknessAnalysis;
import com.example.studentarchives.enums.AISuggestionSourceEnum;
import com.example.studentarchives.repository.WeaknessAnalysisRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 教师端学生管理服务（《教师端接口文档》十六、学生管理模块）
 * <p>
 * 三个接口均复用学生端既有服务，仅教师侧增加 {@code role_scopes} 范围校验：
 * <ul>
 *   <li>学生档案详情：复用 {@link ProfileService#getProfileInfo}（学生端 GET /profile/info 同一服务）；</li>
 *   <li>成长时间轴：复用 {@link ProfileService#getGrowthTimeline}（学生端 4.2 同一服务，viewType 默认 list）；</li>
 *   <li>职业规划详情：复用 {@link ProfileCareerPlanService#getPlanDetail}（学生端 4.6 同一服务）。</li>
 * </ul>
 * 越权（学生不在教师授权范围内）返回 20005 无访问权限。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherStudentService {

    private final AdminAuthService adminAuthService;
    private final TeacherScopeValidator scopeValidator;
    private final ProfileService profileService;
    private final ProfileCareerPlanService profileCareerPlanService;
    private final WeaknessAnalysisRepository weaknessAnalysisRepository;

    /** LocalDateTime → ISO 8601 带时区格式化器 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /**
     * 获取学生档案详情（GET /teacher/students/{studentId}/profile）
     * <p>
     * 范围校验通过后，以目标学生 userId 调用学生端 {@link ProfileService#getProfileInfo}，
     * 组装档案主体并平铺返回，附加 {@code isInScope=true} 标记。
     *
     * @param teacherId 当前教师用户 ID
     * @param studentId 目标学生用户 ID
     * @return 学生档案详情（含 isInScope 标记）
     */
    @Transactional(readOnly = true)
    public TeacherStudentProfileResponse getStudentProfile(Long teacherId, Long studentId) {
        scopeValidator.ensureStudentInScope(teacherId, studentId, adminAuthService.getOperatorSchoolId(teacherId));
        ProfileInfoResponse profile = profileService.getProfileInfo(studentId);
        return TeacherStudentProfileResponse.builder()
                .isInScope(true)
                .profile(profile)
                .build();
    }

    /**
     * 获取学生成长时间轴（GET /teacher/students/{studentId}/growth-timeline）
     * <p>
     * 范围校验通过后，以目标学生 userId 调用学生端 {@link ProfileService#getGrowthTimeline}
     * （viewType 固定 list，与学生端默认一致）。
     *
     * @param teacherId  当前教师用户 ID
     * @param studentId  目标学生用户 ID
     * @param semesterId 学期 ID 筛选
     * @param eventType  事件类型：1=奖项 2=成绩 3=实践 4=职业规划 5=短板改进 6=能力提升
     * @param status     审核状态筛选
     * @return 成长时间轴响应
     */
    @Transactional(readOnly = true)
    public GrowthTimelineResponse getGrowthTimeline(Long teacherId, Long studentId,
                                                    Long semesterId, Integer eventType, Integer status) {
        scopeValidator.ensureStudentInScope(teacherId, studentId, adminAuthService.getOperatorSchoolId(teacherId));
        return profileService.getGrowthTimeline(studentId, semesterId, eventType, status, "list");
    }

    /**
     * 获取学生职业规划详情（GET /teacher/students/{studentId}/career-plans/{planId}）
     * <p>
     * 范围校验通过后，以目标学生 userId 调用学生端 {@link ProfileCareerPlanService#getPlanDetail}
     * （服务内自带归属校验：plan 不属于该学生返回 30001）。
     *
     * @param teacherId 当前教师用户 ID
     * @param studentId 目标学生用户 ID
     * @param planId    规划 ID
     * @return 规划详情
     */
    @Transactional(readOnly = true)
    public CareerPlanDetailResponse getCareerPlanDetail(Long teacherId, Long studentId, Long planId) {
        scopeValidator.ensureStudentInScope(teacherId, studentId, adminAuthService.getOperatorSchoolId(teacherId));
        return profileCareerPlanService.getPlanDetail(studentId, planId);
    }

    /**
     * 获取学生短板分析列表（GET /teacher/students/{studentId}/weaknesses）
     * <p>
     * 范围校验通过后，复用 {@link WeaknessAnalysisRepository#findByUserIdOrderByCreatedAtDesc}
     * 查询目标学生的短板分析（与学生端 4.1 个人档案短板分析同源），
     * 按创建时间倒序返回，并补充 {@code source} / {@code sourceLabel}。
     *
     * @param teacherId 当前教师用户 ID
     * @param studentId 目标学生用户 ID
     * @return 短板分析列表（文档 12.6.1）
     */
    @Transactional(readOnly = true)
    public List<TeacherWeaknessItemResponse> getWeaknesses(Long teacherId, Long studentId) {
        scopeValidator.ensureStudentInScope(teacherId, studentId, adminAuthService.getOperatorSchoolId(teacherId));
        return weaknessAnalysisRepository.findByUserIdOrderByCreatedAtDesc(studentId)
                .stream()
                .map(this::toWeaknessItem)
                .collect(Collectors.toList());
    }

    /**
     * 短板分析实体 → 教师端响应项
     */
    private TeacherWeaknessItemResponse toWeaknessItem(WeaknessAnalysis weakness) {
        return TeacherWeaknessItemResponse.builder()
                .id(weakness.getId())
                .weaknessType(weakness.getWeaknessType())
                .weaknessDesc(weakness.getWeaknessDesc())
                .severityLevel(weakness.getSeverityLevel())
                .source(weakness.getSource())
                .sourceLabel(AISuggestionSourceEnum.of(weakness.getSource()).getLabel())
                .isRead(weakness.getIsRead())
                .createdAt(toIso(weakness.getCreatedAt()))
                .build();
    }

    /**
     * LocalDateTime → ISO 8601 带时区字符串
     */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
