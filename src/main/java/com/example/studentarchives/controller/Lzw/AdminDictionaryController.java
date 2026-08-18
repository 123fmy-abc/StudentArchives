package com.example.studentarchives.controller.Lzw;

import com.example.studentarchives.annotation.AuditLog;
import com.example.studentarchives.common.ApiResult;
import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.service.Lzw.DictionaryManageService;
import com.example.studentarchives.service.Lzw.DictionaryManageService.DictItemCreateRequest;
import com.example.studentarchives.service.Lzw.DictionaryManageService.DictItemIdResponse;
import com.example.studentarchives.service.Lzw.DictionaryManageService.DictItemListResponse;
import com.example.studentarchives.service.Lzw.DictionaryManageService.DictItemUpdateRequest;
import com.example.studentarchives.service.Lzw.DictionaryManageService.DictTypeItem;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端字典数据管理模块（Lzw）
 * <p>
 * 对应《管理端接口文档》十、字典数据管理模块（10.1 ~ 10.5）。
 * 权限：要求 admin 角色（越权返回 20005），由 Service 层校验。
 */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminDictionaryController {

    private final DictionaryManageService dictionaryManageService;

    // ==================== 10.1 获取字典类型列表 ====================

    @GetMapping("/dict/types")
    public ApiResult<PageResult<DictTypeItem>> listTypes(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        return ApiResult.success(dictionaryManageService.listTypes(operatorId, keyword, status, buildPageParam(page, perPage)));
    }

    // ==================== 10.2 获取字典项列表 ====================

    @GetMapping("/dict/items")
    public ApiResult<DictItemListResponse> listItems(
            @AuthenticationPrincipal Long operatorId,
            @RequestParam(value = "dictType", required = false) String dictType,
            @RequestParam(value = "status", required = false) Integer status,
            @RequestParam(value = "page", required = false, defaultValue = "1") int page,
            @RequestParam(value = "per_page", required = false, defaultValue = "20") int perPage) {
        return ApiResult.success(dictionaryManageService.listItems(operatorId, dictType, status, buildPageParam(page, perPage)));
    }

    // ==================== 10.3 创建字典项 ====================

    @AuditLog(module = "dict", action = "create", description = "创建字典项: #body.dictType/#body.dictValue", relatedType = "dict")
    @PostMapping("/dict/items")
    public ApiResult<DictItemIdResponse> createItem(
            @AuthenticationPrincipal Long operatorId,
            @RequestBody DictItemCreateRequest body) {
        return ApiResult.success("创建成功", dictionaryManageService.createItem(operatorId, body));
    }

    // ==================== 10.4 更新字典项 ====================

    @AuditLog(module = "dict", action = "update", description = "更新字典项: #itemId", relatedType = "dict", relatedId = "#itemId")
    @PutMapping("/dict/items/{itemId}")
    public ApiResult<Void> updateItem(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long itemId,
            @RequestBody DictItemUpdateRequest body) {
        dictionaryManageService.updateItem(operatorId, itemId, body);
        return ApiResult.success("更新成功", null);
    }

    // ==================== 10.5 删除字典项 ====================

    @AuditLog(module = "dict", action = "delete", description = "删除字典项: #itemId", relatedType = "dict", relatedId = "#itemId")
    @DeleteMapping("/dict/items/{itemId}")
    public ApiResult<Void> deleteItem(
            @AuthenticationPrincipal Long operatorId,
            @PathVariable Long itemId) {
        dictionaryManageService.deleteItem(operatorId, itemId);
        return ApiResult.success("删除成功", null);
    }

    private PageParam buildPageParam(int page, int perPage) {
        return PageParam.builder()
                .page(Math.max(page, 1))
                .perPage(Math.min(Math.max(perPage, 1), 100))
                .build();
    }
}
