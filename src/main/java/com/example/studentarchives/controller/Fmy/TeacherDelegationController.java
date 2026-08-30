package com.example.studentarchives.controller.Fmy;

import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.dto.Fmy.delegation.request.DelegationCancelRequest;
import com.example.studentarchives.dto.Fmy.delegation.request.DelegationCreateRequest;
import com.example.studentarchives.dto.Fmy.delegation.response.DelegationCancelResponse;
import com.example.studentarchives.dto.Fmy.delegation.response.DelegationCreateResponse;
import com.example.studentarchives.dto.Fmy.delegation.response.DelegationListResponse;
import com.example.studentarchives.service.Fmy.TeacherDelegationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 教师端审批委托 Controller（《教师端接口文档》十一、审批委托模块）
 * <p>
 * 权限码：delegate:manage — 创建/取消/查看自己的审批委托。
 * 路由前缀 /teacher 已在 SecurityConfig 中配置为登录即可访问（无需管理员角色）。
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/teacher/delegations")
public class TeacherDelegationController {

    private final TeacherDelegationService teacherDelegationService;

    /**
     * 获取我的审批委托列表（GET /teacher/delegations，《教师端接口文档》15.1）
     *
     * @param userId    当前登录教师用户 ID
     * @param direction delegator（我委托的）/ delegatee（委托给我的），不传返回全部
     * @param status    状态筛选：0=待生效 1=生效中 2=已过期 3=已取消，不传返回全部
     * @param page      页码，默认 1
     * @param perPage   每页条数，默认 10，最大 100
     */
    @GetMapping
    public ApiResult<DelegationListResponse> listDelegations(
            @AuthenticationPrincipal Long userId,
            @RequestParam(value = "direction", required = false) String direction,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "10") int perPage) {
        PageParam pageParam = PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
        return ApiResult.success(teacherDelegationService.listDelegations(userId, direction, status, pageParam));
    }

    /**
     * 创建审批委托（POST /teacher/delegations，《教师端接口文档》15.2）
     *
     * @param userId  当前登录教师用户 ID（委托人）
     * @param request 委托创建请求体
     */
    @PostMapping
    public ApiResult<DelegationCreateResponse> createDelegation(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody DelegationCreateRequest request) {
        return ApiResult.success("委托创建成功", teacherDelegationService.createDelegation(userId, request));
    }

    /**
     * 取消审批委托（PUT /teacher/delegations/{delegationId}/cancel，《教师端接口文档》15.3）
     *
     * @param userId        当前登录教师用户 ID（委托人）
     * @param delegationId  委托记录 ID
     * @param request       取消请求体（含可选取消原因）
     */
    @PutMapping("/{delegationId:[0-9]+}/cancel")
    public ApiResult<DelegationCancelResponse> cancelDelegation(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long delegationId,
            @Valid @RequestBody DelegationCancelRequest request) {
        return ApiResult.success("委托已取消",
                teacherDelegationService.cancelDelegation(userId, delegationId, request.getCancelReason()));
    }
}
