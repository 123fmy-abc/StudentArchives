package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.formtemplate.request.FormTemplateCreateRequest;
import com.example.studentarchives.dto.Fmy.formtemplate.request.FormTemplateFieldItem;
import com.example.studentarchives.dto.Fmy.formtemplate.request.FormTemplateUpdateRequest;
import com.example.studentarchives.dto.Fmy.formtemplate.response.FormTemplateCreateResponse;
import com.example.studentarchives.dto.Fmy.formtemplate.response.FormTemplateDefaultResponse;
import com.example.studentarchives.dto.Fmy.formtemplate.response.FormTemplateDeleteResponse;
import com.example.studentarchives.dto.Fmy.formtemplate.response.FormTemplateDetailResponse;
import com.example.studentarchives.dto.Fmy.formtemplate.response.FormTemplateItem;
import com.example.studentarchives.dto.Fmy.formtemplate.response.FormTemplateUpdateResponse;
import com.example.studentarchives.entity.foundation.FormTemplate;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AdminFormTemplateRepository;
import com.example.studentarchives.repository.SchoolRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端表单自定义模板服务
 * <p>
 * 对应《管理端接口文档》十七、表单自定义模板模块（17.1~17.6），统一前缀 /admin/form-templates：
 * <ul>
 *   <li>17.1 GET /admin/form-templates：模板列表（分页，按学校/类别/编码/状态/关键词筛选）；</li>
 *   <li>17.2 GET /admin/form-templates/{templateId}：模板详情（含 fields、layoutConfig 大字段）；</li>
 *   <li>17.3 POST /admin/form-templates：创建模板（version 初始 1，同校 code + category 唯一）；</li>
 *   <li>17.4 PUT /admin/form-templates/{templateId}：更新模板（version 自动 +1，历史申报按字段快照不受影响）；</li>
 *   <li>17.5 DELETE /admin/form-templates/{templateId}：软删除模板（默认模板不允许删除）；</li>
 *   <li>17.6 PUT /admin/form-templates/{templateId}/default：设置默认模板（同校 code + category 下唯一，须启用状态）。</li>
 * </ul>
 * 所有接口需校验 admin 角色或 form:template:manage 权限码，越权返回 20005 无访问权限。
 * 学校范围统一由当前登录用户所属学校推导，不接受前端传入 schoolId（管理端全局约定）。
 * 删除采用逻辑删除（deleted_at 置位）；fields/layoutConfig/applicableRoles 为 JSON 列，
 * 入库前序列化为 JSON 字符串，读出时解析为 JsonNode / List&lt;String&gt;。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFormTemplateService {

    /** 表单自定义模板管理权限码（《管理端接口文档》关键权限码） */
    private static final String PERMISSION = "form:template:manage";

    /** 适用类别中文标签 */
    private static final Map<String, String> CATEGORY_LABELS = Map.of(
            "archive", "档案",
            "award", "奖项",
            "career_plan", "职业规划");

    /** 字段组件类型白名单（《管理端接口文档》17.2 字段配置说明） */
    private static final Set<String> ALLOWED_FIELD_TYPES = Set.of(
            "input", "textarea", "number", "select", "radio",
            "checkbox", "date", "upload", "switch");

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AdminAuthService adminAuthService;
    private final AdminFormTemplateRepository adminFormTemplateRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    // ==================== 17.1 获取表单模板列表 ====================

    /**
     * 获取表单模板列表（GET /admin/form-templates，文档 17.1）
     * <p>
     * 按当前登录用户所属学校、适用类别、模板编码、启用状态、名称关键词筛选，
     * 按更新时间倒序分页；列表项不含 fields、layoutConfig 大字段（需调详情接口）。
     *
     * @param userId    当前登录用户 ID
     * @param category  适用类别（可选）：archive/award/career_plan
     * @param code      模板编码（可选）
     * @param status    0=禁用 1=启用（可选，不传返回全部）
     * @param keyword   模板名称模糊搜索（可选）
     * @param pageParam 分页参数
     * @return 分页的模板列表
     */
    public PageResult<FormTemplateItem> listTemplates(Long userId, String category, String code,
                                                      Integer status, String keyword, PageParam pageParam) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);
        List<FormTemplate> all = adminFormTemplateRepository.findAll().stream()
                .filter(t -> Objects.equals(t.getSchoolId(), schoolId))
                .filter(t -> category == null || category.isBlank() || Objects.equals(t.getCategory(), category))
                .filter(t -> code == null || code.isBlank() || Objects.equals(t.getCode(), code))
                .filter(t -> status == null || Objects.equals(t.getStatus(), status))
                .filter(t -> keyword == null || keyword.isBlank()
                        || (t.getTemplateName() != null && t.getTemplateName().contains(keyword)))
                .sorted(Comparator.comparing(FormTemplate::getUpdatedAt,
                        Comparator.nullsFirst(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        long total = all.size();
        int offset = pageParam.getOffset();
        List<FormTemplate> pageItems = offset >= all.size()
                ? List.of()
                : all.subList(offset, Math.min(offset + pageParam.getPerPage(), all.size()));

        // 批量加载创建人姓名
        Map<Long, String> creatorName = userRepository.findByIdIn(
                        pageItems.stream().map(FormTemplate::getCreatedBy).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));

        List<FormTemplateItem> list = pageItems.stream()
                .map(t -> toListItem(t, creatorName.get(t.getCreatedBy())))
                .collect(Collectors.toList());
        return PageResult.of(list, total, pageParam);
    }

    // ==================== 17.2 获取表单模板详情 ====================

    /**
     * 获取表单模板详情（GET /admin/form-templates/{templateId}，文档 17.2）
     * <p>
     * 返回模板完整配置：fields 字段配置、layoutConfig 布局配置解析为 JsonNode，
     * 并携带适用角色与版本信息。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @return 模板完整配置
     */
    public FormTemplateDetailResponse getTemplateDetail(Long userId, Long templateId) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        FormTemplate template = requireTemplate(templateId);
        String creatorName = userRepository.findById(template.getCreatedBy())
                .map(User::getName).orElse(null);
        return toDetailResponse(template, creatorName);
    }

    // ==================== 17.3 创建表单模板 ====================

    /**
     * 创建表单模板（POST /admin/form-templates，文档 17.3）
     * <p>
     * 创建学校维度的表单自定义模板，version 初始 1，is_default 默认 0，status 默认 1。
     * 同一学校、同一 code + category 在软删除条件下唯一（重复返回 10002 参数错误）。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建请求
     * @return 创建结果
     */
    public FormTemplateCreateResponse createTemplate(Long userId, FormTemplateCreateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学校不存在"));
        validateCategory(request.getCategory());
        validateFields(request.getFields());
        checkCodeUnique(schoolId, request.getCategory(), request.getCode(), null);

        FormTemplate template = new FormTemplate();
        template.setSchoolId(schoolId);
        template.setTemplateName(request.getTemplateName());
        template.setCode(request.getCode());
        template.setCategory(request.getCategory());
        template.setDescription(request.getDescription());
        template.setFields(writeJson(request.getFields()));
        template.setLayoutConfig(writeJson(request.getLayoutConfig()));
        template.setApplicableRoles(writeJson(request.getApplicableRoles()));
        template.setIsDefault(request.getIsDefault() != null ? request.getIsDefault() : 0);
        template.setVersion(1);
        template.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        template.setCreatedBy(userId);
        template.setUpdatedBy(userId);
        template = adminFormTemplateRepository.save(template);

        return FormTemplateCreateResponse.builder()
                .id(template.getId())
                .schoolId(template.getSchoolId())
                .templateName(template.getTemplateName())
                .code(template.getCode())
                .category(template.getCategory())
                .version(template.getVersion())
                .isDefault(template.getIsDefault())
                .status(template.getStatus())
                .createdAt(toIso(template.getCreatedAt()))
                .updatedAt(toIso(template.getUpdatedAt()))
                .build();
    }

    // ==================== 17.4 更新表单模板 ====================

    /**
     * 更新表单模板（PUT /admin/form-templates/{templateId}，文档 17.4）
     * <p>
     * 全部字段可选，未传（null）表示不修改；更新成功时 version 自动 +1。
     * code / category / schoolId 不支持修改；status 直接在本接口变更。
     * 已提交表单按当时字段快照存储，历史申报不受影响。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @param request    更新请求
     * @return 更新结果
     */
    public FormTemplateUpdateResponse updateTemplate(Long userId, Long templateId, FormTemplateUpdateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        FormTemplate template = requireTemplate(templateId);

        if (request.getTemplateName() != null) {
            template.setTemplateName(request.getTemplateName());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getFields() != null) {
            validateFields(request.getFields());
            template.setFields(writeJson(request.getFields()));
        }
        if (request.getLayoutConfig() != null) {
            template.setLayoutConfig(writeJson(request.getLayoutConfig()));
        }
        if (request.getApplicableRoles() != null) {
            template.setApplicableRoles(writeJson(request.getApplicableRoles()));
        }
        if (request.getStatus() != null) {
            if (request.getStatus() != 0 && request.getStatus() != 1) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "status 仅支持 0=禁用 1=启用");
            }
            template.setStatus(request.getStatus());
        }
        // 版本自动 +1，便于追溯学生端/教师端填写时使用的模板版本
        template.setVersion((template.getVersion() != null ? template.getVersion() : 1) + 1);
        template.setUpdatedBy(userId);
        template = adminFormTemplateRepository.save(template);

        return FormTemplateUpdateResponse.builder()
                .id(template.getId())
                .schoolId(template.getSchoolId())
                .templateName(template.getTemplateName())
                .code(template.getCode())
                .category(template.getCategory())
                .version(template.getVersion())
                .isDefault(template.getIsDefault())
                .status(template.getStatus())
                .updatedAt(toIso(template.getUpdatedAt()))
                .build();
    }

    // ==================== 17.5 删除表单模板 ====================

    /**
     * 删除表单模板（DELETE /admin/form-templates/{templateId}，文档 17.5）
     * <p>
     * 软删除（deleted_at 置位）；当前处于默认状态（is_default=1）的模板不允许删除，
     * 需先通过 17.6 取消默认，返回 4 操作失败。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @return 删除结果
     */
    @Transactional
    public FormTemplateDeleteResponse deleteTemplate(Long userId, Long templateId) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        FormTemplate template = requireTemplate(templateId);
        if (Objects.equals(template.getIsDefault(), 1)) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "默认模板需先取消默认再删除");
        }
        LocalDateTime deletedAt = LocalDateTime.now();
        int updated = adminFormTemplateRepository.softDelete(templateId, deletedAt);
        if (updated == 0) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "模板不存在");
        }
        return FormTemplateDeleteResponse.builder()
                .id(templateId)
                .deletedAt(toIso(deletedAt))
                .build();
    }

    // ==================== 17.6 设置默认表单模板 ====================

    /**
     * 设置默认表单模板（PUT /admin/form-templates/{templateId}/default，文档 17.6）
     * <p>
     * 将指定模板设为某学校、某 code + category 下的默认模板，同时取消同校同 code + category 下
     * 其他模板的默认状态（is_default=0）。被设置模板必须处于启用状态（status=1），否则返回 4 操作失败。
     *
     * @param userId     当前登录用户 ID
     * @param templateId 模板 ID
     * @return 设置结果
     */
    @Transactional
    public FormTemplateDefaultResponse setDefaultTemplate(Long userId, Long templateId) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        FormTemplate template = requireTemplate(templateId);
        if (!Objects.equals(template.getStatus(), 1)) {
            throw new BusinessException(ResultCode.OPERATION_FAILED, "禁用状态模板不能设为默认");
        }
        // 取消同学校、同 code + category 下其他模板的默认状态
        List<FormTemplate> sameSchoolCategoryCode = adminFormTemplateRepository
                .findBySchoolIdAndCategoryAndCode(template.getSchoolId(), template.getCategory(), template.getCode());
        boolean changed = false;
        for (FormTemplate other : sameSchoolCategoryCode) {
            if (!Objects.equals(other.getId(), templateId) && Objects.equals(other.getIsDefault(), 1)) {
                other.setIsDefault(0);
                adminFormTemplateRepository.save(other);
                changed = true;
            }
        }
        template.setIsDefault(1);
        template = adminFormTemplateRepository.save(template);
        log.info("设置默认表单模板 templateId={}, schoolId={}, code={}, category={}{}",
                templateId, template.getSchoolId(), template.getCode(), template.getCategory(),
                changed ? "（已取消其他模板默认）" : "");
        return FormTemplateDefaultResponse.builder()
                .id(template.getId())
                .schoolId(template.getSchoolId())
                .code(template.getCode())
                .category(template.getCategory())
                .templateName(template.getTemplateName())
                .isDefault(template.getIsDefault())
                .updatedAt(toIso(template.getUpdatedAt()))
                .build();
    }

    // ==================== 私有辅助方法 ====================

    /** 按 ID 加载模板，不存在返回 30001 */
    private FormTemplate requireTemplate(Long templateId) {
        return adminFormTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "模板不存在"));
    }

    /** 校验适用类别合法（archive/award/career_plan），非法返回 10001 参数错误 */
    private void validateCategory(String category) {
        if (category == null || !CATEGORY_LABELS.containsKey(category)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "category 仅支持 archive/award/career_plan，当前取值: " + category);
        }
    }

    /**
     * 校验字段配置数组：非空、字段 type 在组件类型白名单内、字段 key 唯一。
     * 失败返回 10001 参数错误（文档 17.2 字段配置说明）。
     */
    private void validateFields(List<FormTemplateFieldItem> fields) {
        if (fields == null || fields.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "fields 至少 1 个字段");
        }
        Set<String> keys = new HashSet<>();
        for (FormTemplateFieldItem field : fields) {
            if (field.getType() == null || !ALLOWED_FIELD_TYPES.contains(field.getType())) {
                throw new BusinessException(ResultCode.PARAM_ERROR,
                        "字段 type 仅支持 input/textarea/number/select/radio/checkbox/date/upload/switch，"
                                + "当前取值: " + field.getType());
            }
            if (field.getKey() != null && !keys.add(field.getKey())) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "字段 key 重复: " + field.getKey());
            }
        }
    }

    /**
     * 校验模板编码同校同类别唯一（excludeId 为当前更新模板 ID，创建时传 null）。
     * 唯一约束 UNIQUE(school_id, code, category, is_deleted_null)，重复返回 10002 参数错误。
     */
    private void checkCodeUnique(Long schoolId, String category, String code, Long excludeId) {
        boolean duplicate = adminFormTemplateRepository
                .findBySchoolIdAndCategoryAndCode(schoolId, category, code)
                .stream().anyMatch(t -> excludeId == null || !Objects.equals(t.getId(), excludeId));
        if (duplicate) {
            throw new BusinessException(10002, "模板编码已存在");
        }
    }

    /** 模板 → 列表项 DTO */
    private FormTemplateItem toListItem(FormTemplate t, String creatorName) {
        return FormTemplateItem.builder()
                .id(t.getId())
                .schoolId(t.getSchoolId())
                .templateName(t.getTemplateName())
                .code(t.getCode())
                .category(t.getCategory())
                .categoryLabel(categoryLabel(t.getCategory()))
                .description(t.getDescription())
                .applicableRoles(parseRoles(t.getApplicableRoles()))
                .isDefault(t.getIsDefault())
                .version(t.getVersion())
                .status(t.getStatus())
                .statusLabel(statusLabel(t.getStatus()))
                .createdBy(t.getCreatedBy())
                .createdByName(creatorName)
                .createdAt(toIso(t.getCreatedAt()))
                .updatedAt(toIso(t.getUpdatedAt()))
                .build();
    }

    /** 模板 → 详情 DTO（fields/layoutConfig JSON 列解析为 JsonNode） */
    private FormTemplateDetailResponse toDetailResponse(FormTemplate t, String creatorName) {
        return FormTemplateDetailResponse.builder()
                .id(t.getId())
                .schoolId(t.getSchoolId())
                .templateName(t.getTemplateName())
                .code(t.getCode())
                .category(t.getCategory())
                .categoryLabel(categoryLabel(t.getCategory()))
                .description(t.getDescription())
                .fields(parseJson(t.getFields()))
                .layoutConfig(parseJson(t.getLayoutConfig()))
                .applicableRoles(parseRoles(t.getApplicableRoles()))
                .isDefault(t.getIsDefault())
                .version(t.getVersion())
                .status(t.getStatus())
                .statusLabel(statusLabel(t.getStatus()))
                .createdBy(t.getCreatedBy())
                .createdByName(creatorName)
                .createdAt(toIso(t.getCreatedAt()))
                .updatedAt(toIso(t.getUpdatedAt()))
                .build();
    }

    // ==================== 标签映射 ====================

    private String categoryLabel(String category) {
        return category != null ? CATEGORY_LABELS.get(category) : null;
    }

    /** 状态：0=禁用 1=启用 */
    private String statusLabel(Integer status) {
        if (status == null) {
            return null;
        }
        return status == 1 ? "启用" : "禁用";
    }

    // ==================== JSON 工具 ====================

    /** 序列化对象到 JSON 字符串（null 原样返回，供 JSON 列写入） */
    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("序列化 JSON 失败", e);
            return null;
        }
    }

    /** 解析 JSON 字符串为 JsonNode（空白返回 null，供详情接口返回 JSON 列） */
    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (JsonProcessingException e) {
            log.warn("JSON 解析失败: {}", json, e);
            return null;
        }
    }

    /** 解析适用角色 JSON 数组为 List&lt;String&gt;（非数组/空白返回 null） */
    private List<String> parseRoles(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            JsonNode node = objectMapper.readTree(json);
            if (!node.isArray()) {
                return null;
            }
            List<String> roles = new ArrayList<>();
            node.forEach(n -> {
                if (n.isTextual()) {
                    roles.add(n.asText());
                }
            });
            return roles;
        } catch (JsonProcessingException e) {
            log.warn("JSON 解析失败: {}", json, e);
            return null;
        }
    }

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
