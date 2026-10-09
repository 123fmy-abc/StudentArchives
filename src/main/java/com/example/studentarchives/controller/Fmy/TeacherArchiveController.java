package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.dto.Fmy.archive.response.ArchiveAdminListItem;
import com.example.studentarchives.dto.Fmy.archive.response.ArchiveOverviewResponse;
import com.example.studentarchives.service.Fmy.TeacherArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端档案库控制器
 * <p>
 * 对应《教师端接口文档》十六、档案库模块（教师端 12.1.7 学生档案列表 / 12.1.8 组织档案汇总）。
 * 统一前缀 /teacher/archives，响应 DTO 直接复用管理端
 * {@link ArchiveAdminListItem} / {@link ArchiveOverviewResponse}，前端无需为教师端另写一套解析。
 * <p>
 * 教师登录即可访问（不走 admin / {@code archive:view} 权限码），数据范围严格由 {@code role_scopes}
 * 决定：所有查询结果与教师授权学生集合求交，越权统一返回 20005 无访问权限。
 * 学校由当前登录用户推导，不接受前端传入 schoolId。
 */
@Slf4j
@RestController
@RequestMapping("/teacher/archives")
@RequiredArgsConstructor
public class TeacherArchiveController {

    private final TeacherArchiveService teacherArchiveService;

    // ==================== 12.1.7 学生档案列表 ====================

    /**
     * 获取授权范围内学生档案列表（GET /teacher/archives）
     * <p>
     * 支持按组织维度（年级/学院/专业/班级）、档案类型、状态、学期、关键词
     * （姓名/学号/档案标题）筛选，按档案 ID 倒序分页。组织参数只能收窄范围，不能越过 role_scopes。
     *
     * @param userId      当前登录用户 ID（由 JWT 过滤器注入）
     * @param grade       年级筛选（可选）
     * @param collegeId   学院 ID（可选）
     * @param majorId     专业 ID（可选）
     * @param classId     班级 ID（可选）
     * @param archiveType 档案类型编码（可选）
     * @param status      档案状态（可选，0=草稿 1=待审批 2=通过 3=已退回 4=已撤销）
     * @param semesterId  学期 ID（可选）
     * @param keyword     关键词（可选，匹配学生姓名/学号/档案标题）
     * @param page        页码，默认 1
     * @param perPage     每页条数，默认 20
     * @return 分页档案列表
     */
    @GetMapping
    public ApiResult<PageResult<ArchiveAdminListItem>> listArchives(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "collegeId", required = false) Long collegeId,
            @RequestParam(value = "majorId", required = false) Long majorId,
            @RequestParam(value = "classId", required = false) Long classId,
            @RequestParam(value = "archiveType", required = false) String archiveType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder().page(page).perPage(perPage).build();
        PageResult<ArchiveAdminListItem> result = teacherArchiveService.listArchives(
                userId, grade, collegeId, majorId, classId, archiveType, status, semesterId, keyword, pageParam);
        return ApiResult.success(result);
    }

    // ==================== 12.1.8 组织档案汇总 ====================

    /**
     * 获取授权范围内组织档案汇总（GET /teacher/archives/overview）
     * <p>
     * 按组织维度（2=学院 3=专业 4=班级 6=年级）实时聚合授权范围内学生的档案总数与状态分布、
     * 类型分布；orgType 不传默认全校单条；orgId 下钻其下一级。无授权学生的组织行不返回。
     *
     * @param userId     当前登录用户 ID
     * @param semesterId 学期 ID（可选，不传取当前学期）
     * @param orgType    汇总维度（可选，2=学院 3=专业 4=班级 6=年级）
     * @param orgId      指定组织 ID（可选，下钻其下一级）
     * @param grade      年级筛选（可选）
     * @return 组织汇总响应
     */
    @GetMapping("/overview")
    public ApiResult<ArchiveOverviewResponse> archiveOverview(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "semesterId", required = false) Long semesterId,
            @RequestParam(value = "orgType", required = false) Integer orgType,
            @RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "grade", required = false) String grade) {
        return ApiResult.success(teacherArchiveService.archiveOverview(userId, semesterId, orgType, orgId, grade));
    }
}
