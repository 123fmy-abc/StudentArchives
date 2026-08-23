package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.service.Lzw.OrgManageService;
import com.example.studentarchives.service.Lzw.OrgManageService.ClassIdResponse;
import com.example.studentarchives.service.Lzw.OrgManageService.ClassItem;
import com.example.studentarchives.service.Lzw.OrgManageService.ClassSaveRequest;
import com.example.studentarchives.service.Lzw.OrgManageService.CollegeItem;
import com.example.studentarchives.service.Lzw.OrgManageService.MajorCreateRequest;
import com.example.studentarchives.service.Lzw.OrgManageService.MajorIdResponse;
import com.example.studentarchives.service.Lzw.OrgManageService.MajorItem;
import com.example.studentarchives.service.Lzw.OrgManageService.SchoolItem;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端基础组织架构管理模块（Lzw）
 * <p>
 * 对应《管理端接口文档》十一、基础组织架构管理模块（11.1 ~ 11.7）。
 * 权限：要求 admin 角色（越权返回 20005），由 Service 层校验。
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminOrgController {

    private final OrgManageService orgManageService;

    // ==================== 11.1 获取学校列表 ====================

    @GetMapping("/schools")
    public ApiResult<List<SchoolItem>> listSchools(@AuthenticationPrincipal Long operatorId) {
        return ApiResult.success(orgManageService.listSchools(operatorId));
    }

    // ==================== 11.2 获取学院列表 ====================

    @GetMapping("/colleges")
    public ApiResult<List<CollegeItem>> listColleges(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(value = "schoolId", required = false) Long schoolId,
            @RequestParam(value = "status", required = false) Integer status) {
        return ApiResult.success(orgManageService.listColleges(operatorId, schoolId, status));
    }

    // ==================== 11.3 获取专业列表 ====================

    @GetMapping("/majors")
    public ApiResult<List<MajorItem>> listMajors(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(value = "collegeId", required = false) Long collegeId,
            @RequestParam(value = "schoolId", required = false) Long schoolId,
            @RequestParam(value = "status", required = false) Integer status) {
        return ApiResult.success(orgManageService.listMajors(operatorId, collegeId, schoolId, status));
    }

    // ==================== 11.4 获取班级列表 ====================

    @GetMapping("/classes")
    public ApiResult<PageResult<ClassItem>> listClasses(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(value = "majorId", required = false) Long majorId,
            @RequestParam(value = "collegeId", required = false) Long collegeId,
            @RequestParam(value = "schoolId", required = false) Long schoolId,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        return ApiResult.success(orgManageService.listClasses(operatorId, majorId, collegeId, schoolId, grade, status, keyword, pageParam));
    }

    // ==================== 11.5 创建班级 ====================

    @AuditLog(module = "org", action = "create-class", description = "创建班级: #body.className", relatedType = "class")
    @PostMapping("/classes")
    public ApiResult<ClassIdResponse> createClass(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody ClassSaveRequest body) {
        return ApiResult.success("创建成功", orgManageService.createClass(operatorId, body));
    }

    // ==================== 11.6 更新班级 ====================

    @AuditLog(module = "org", action = "update-class", description = "更新班级: #classId", relatedType = "class", relatedId = "#classId")
    @PutMapping("/classes/{classId}")
    public ApiResult<Void> updateClass(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long classId,
            @RequestBody ClassSaveRequest body) {
        orgManageService.updateClass(operatorId, classId, body);
        return ApiResult.success("更新成功", null);
    }

    // ==================== 11.7 创建专业 ====================

    @AuditLog(module = "org", action = "create-major", description = "创建专业: #body.majorName", relatedType = "major")
    @PostMapping("/majors")
    public ApiResult<MajorIdResponse> createMajor(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody MajorCreateRequest body) {
        return ApiResult.success("创建成功", orgManageService.createMajor(operatorId, body));
    }
}
