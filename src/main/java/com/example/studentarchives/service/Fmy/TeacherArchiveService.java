package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.archive.response.ArchiveAdminListItem;
import com.example.studentarchives.dto.Fmy.archive.response.ArchiveOverviewResponse;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.service.common.AdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

/**
 * 教师端档案库服务（《教师端接口文档》十六、档案库模块）
 * <p>
 * 教师端此前无档案入口：{@code /admin/archives*} 走 admin 或 {@code archive:view} 权限码
 * （仅 admin 持有），教师调用返回 20005；教师登录后只能拿到统计看板，看不到具体档案。
 * 本服务在 {@code /teacher/archives*} 上复用 {@link AdminArchiveService} 的查询与映射，
 * 以「教师授权学生集合」作为数据边界，不复制查询实现：
 * <ul>
 *   <li>学校由当前登录用户推导（{@code getOperatorSchoolId}），不接受前端传入 schoolId；</li>
 *   <li>授权学生范围由 {@link TeacherScopeValidator#authorizedStudentIds} 按 {@code role_scopes}
 *       展开（学院/专业/班级按组织链下沉到学生；学校级/年级级授权为全校；admin 不限制）；</li>
 *   <li>前端传入的 grade/collegeId/majorId/classId 只作为**缩小**条件，与授权学生集合求交，
 *       无法借组织参数越权看到范围外学生。</li>
 * </ul>
 * 教师无任何生效授权范围（{@code role_scopes} 为空）时返回 20005 无访问权限。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherArchiveService {

    private final AdminAuthService adminAuthService;
    private final TeacherScopeValidator scopeValidator;
    private final AdminArchiveService adminArchiveService;

    /**
     * 教师端档案列表（GET /teacher/archives）。
     * <p>
     * 数据范围 = 教师授权学生集合 ∩ 请求组织筛选结果，二者缺一不可；其余筛选
     * （档案类型/状态/学期/关键词）与分页行为同管理端 15.1。
     *
     * @param userId      当前教师用户 ID
     * @param grade       年级筛选（可选）
     * @param collegeId   学院 ID（可选）
     * @param majorId     专业 ID（可选）
     * @param classId     班级 ID（可选）
     * @param archiveType 档案类型编码（可选）
     * @param status      档案状态（可选，0=草稿 1=待审批 2=通过 3=已退回 4=已撤销）
     * @param semesterId  学期 ID（可选）
     * @param keyword     关键词（可选，匹配学生姓名/学号/档案标题）
     * @param pageParam   分页参数
     * @return 分页档案列表
     */
    @Transactional(readOnly = true)
    public PageResult<ArchiveAdminListItem> listArchives(Long userId, String grade, Long collegeId, Long majorId,
                                                         Long classId, String archiveType, Integer status,
                                                         Long semesterId, String keyword, PageParam pageParam) {
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);
        Set<Long> authorizedUserIds = requireAuthorizedStudents(userId, schoolId);
        return adminArchiveService.listArchivesScoped(schoolId, grade, collegeId, majorId, classId,
                archiveType, status, semesterId, keyword, pageParam, authorizedUserIds);
    }

    /**
     * 教师端组织档案汇总（GET /teacher/archives/overview）。
     * <p>
     * 各组织行的档案计数与状态/类型分布仅统计**授权范围内**的学生，且完全不包含授权学生的
     * 组织行会被隐藏 —— 因此即便前端传入范围外的 {@code orgType}/{@code orgId}，也只会得到
     * 空行集，不会泄露范围外数据，故无需再对 orgType/orgId 做额度的 range 校验。
     *
     * @param userId     当前教师用户 ID
     * @param semesterId 学期 ID（可选，不传取当前学期）
     * @param orgType    汇总维度（可选，2=学院 3=专业 4=班级 6=年级；不传默认全校单条）
     * @param orgId      指定组织 ID（可选，下钻其下一级）
     * @param grade      年级筛选（可选）
     * @return 组织汇总响应
     */
    @Transactional(readOnly = true)
    public ArchiveOverviewResponse archiveOverview(Long userId, Long semesterId, Integer orgType, Long orgId, String grade) {
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);
        Set<Long> authorizedUserIds = requireAuthorizedStudents(userId, schoolId);
        return adminArchiveService.archiveOverviewScoped(schoolId, semesterId, orgType, orgId, grade, authorizedUserIds);
    }

    /**
     * 取教师授权学生集合，无任何生效授权时抛 20005。
     * <p>
     * {@code null} 表示不限制（admin 或学校级/年级级授权），此时不拦截。
     *
     * @return 授权学生 userId 集合；null=不限制（全校）
     */
    private Set<Long> requireAuthorizedStudents(Long userId, Long schoolId) {
        Set<Long> authorized = scopeValidator.authorizedStudentIds(userId, schoolId);
        if (authorized != null && authorized.isEmpty()) {
            // 教师未配置任何生效授权范围：与学生管理/统计看板口径一致，直接拒绝而非返回空列表
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
        return authorized;
    }
}
