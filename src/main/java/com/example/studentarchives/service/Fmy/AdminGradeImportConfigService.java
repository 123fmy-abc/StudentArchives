package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.grade.request.GradeImportConfigColumnItem;
import com.example.studentarchives.dto.Fmy.grade.request.GradeImportConfigSaveRequest;
import com.example.studentarchives.dto.Fmy.grade.request.GradeImportConfigUpdateRequest;
import com.example.studentarchives.dto.Fmy.grade.response.GradeImportConfigResponse;
import com.example.studentarchives.entity.grade.GradeImportConfig;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.GradeImportConfigRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 管理端成绩导入配置服务
 * <p>
 * 对应 {@code grade_import_configs} 表的 CRUD 与状态管理。每个学校仅允许一条配置，
 * 删除采用逻辑删除。配置缺失时成绩导入与模板下载将返回业务异常，引导管理员先完成配置。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminGradeImportConfigService {

    /** 复用成绩导入权限码，配置管理属于导入模块的一部分 */
    private static final String PERMISSION = "grade:import";

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("xlsx", "xls", "csv");

    private static final Set<String> REQUIRED_FIELDS = Set.of("studentNo", "courseName", "score");

    // ==================== 系统内置默认模板（学校未配置时兜底，不落库） ====================

    private static final List<String> DEFAULT_ALLOWED_EXTENSIONS = List.of("xlsx", "xls", "csv");

    private static final long DEFAULT_MAX_FILE_SIZE = 10L * 1024 * 1024;

    private static final int DEFAULT_BATCH_SIZE = 500;

    private static final int DEFAULT_HAS_HEADER_ROW = 1;

    private static final int DEFAULT_ALLOW_OVERWRITE = 0;

    private static final List<GradeImportConfigColumnItem> DEFAULT_TEMPLATE_COLUMNS =
            List.of(defaultColumn("studentNo", "学号"),
                    defaultColumn("courseName", "课程名称"),
                    defaultColumn("score", "成绩"));

    private static GradeImportConfigColumnItem defaultColumn(String field, String label) {
        GradeImportConfigColumnItem item = new GradeImportConfigColumnItem();
        item.setField(field);
        item.setLabel(label);
        item.setRequired(true);
        return item;
    }

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AdminAuthService adminAuthService;
    private final GradeImportConfigRepository gradeImportConfigRepository;
    private final GradeImportFileParser fileParser;
    private final ObjectMapper objectMapper;

    // ==================== 查询配置 ====================

    /**
     * 获取当前学校的成绩导入配置。
     * <p>
     * 学校未创建配置时返回系统内置默认模板（{@code configured=false}、{@code source=DEFAULT}），
     * 不再返回 404，便于前端直接展示“当前使用系统默认模板”。
     *
     * @param userId 当前登录用户 ID
     * @return 配置详情（学校配置或默认模板）
     */
    public GradeImportConfigResponse getConfig(Long userId) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);
        return gradeImportConfigRepository.findBySchoolId(schoolId)
                .map(this::toResponse)
                .orElseGet(() -> toDefaultResponse(schoolId));
    }

    /**
     * 获取当前学校生效的导入配置（供 AdminGradeService 导入时使用）。
     * <p>
     * 学校未创建配置时回退系统默认模板；仅当学校配置存在且被禁用时抛出业务异常。
     *
     * @param schoolId 学校 ID
     * @return 生效配置（学校配置或默认模板）
     * @throws BusinessException 学校配置已禁用
     */
    public GradeImportConfig requireEnabledConfig(Long schoolId) {
        GradeImportConfig config = gradeImportConfigRepository.findBySchoolId(schoolId)
                .orElseGet(() -> buildDefaultConfig(schoolId));
        if (!Integer.valueOf(1).equals(config.getStatus())) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST,
                    "成绩导入配置不存在或已禁用，请先配置导入模板");
        }
        return config;
    }

    /**
     * 解析配置的允许扩展名集合（供 AdminGradeService 导入校验使用）。
     */
    public Set<String> resolveAllowedExtensions(Long schoolId) {
        return resolveAllowedExtensions(requireEnabledConfig(schoolId));
    }

    /**
     * 从配置实体解析允许扩展名集合。
     */
    public Set<String> resolveAllowedExtensions(GradeImportConfig config) {
        return new HashSet<>(parseJsonList(config.getAllowedExtensions()));
    }

    /**
     * 从配置实体解析模板列（供 AdminGradeService 导入解析使用）。
     */
    public List<GradeImportFileParser.TemplateColumn> resolveTemplateColumns(GradeImportConfig config) {
        return parseTemplateColumns(config.getTemplateColumns());
    }

    // ==================== 创建配置 ====================

    /**
     * 创建成绩导入配置。
     * <p>
     * 每个学校仅允许一条配置，已存在时返回参数错误。
     *
     * @param userId  当前登录用户 ID
     * @param request 创建请求
     * @return 配置详情
     */
    @Transactional
    public GradeImportConfigResponse createConfig(Long userId, GradeImportConfigSaveRequest request) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        if (gradeImportConfigRepository.findBySchoolId(schoolId).isPresent()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "当前学校已存在成绩导入配置，请使用更新接口");
        }

        validateSaveRequest(request);

        GradeImportConfig config = new GradeImportConfig();
        config.setSchoolId(schoolId);
        config.setAllowedExtensions(writeJson(request.getAllowedExtensions()));
        config.setMaxFileSize(request.getMaxFileSize());
        config.setTemplateColumns(writeJson(request.getTemplateColumns()));
        config.setHasHeaderRow(request.getHasHeaderRow());
        config.setBatchSize(request.getBatchSize());
        config.setAllowOverwrite(request.getAllowOverwrite());
        config.setStatus(request.getStatus());
        config.setCreatedBy(userId);
        config = gradeImportConfigRepository.save(config);

        return toResponse(config);
    }

    // ==================== 更新配置 ====================

    /**
     * 更新成绩导入配置。
     * <p>
     * 全部字段可选，未传表示不修改；status 变更请使用独立状态接口。
     *
     * @param userId  当前登录用户 ID
     * @param id      配置 ID
     * @param request 更新请求
     * @return 配置详情
     */
    @Transactional
    public GradeImportConfigResponse updateConfig(Long userId, Long id, GradeImportConfigUpdateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        if (request.getStatus() != null) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "status 请通过 PATCH /admin/grade-import-configs/{id}/status 接口修改");
        }

        GradeImportConfig config = requireOwnedConfig(id, schoolId);

        if (request.getAllowedExtensions() != null) {
            validateAllowedExtensions(request.getAllowedExtensions());
            config.setAllowedExtensions(writeJson(request.getAllowedExtensions()));
        }
        if (request.getMaxFileSize() != null) {
            config.setMaxFileSize(request.getMaxFileSize());
        }
        if (request.getTemplateColumns() != null) {
            validateTemplateColumns(request.getTemplateColumns());
            config.setTemplateColumns(writeJson(request.getTemplateColumns()));
        }
        if (request.getHasHeaderRow() != null) {
            config.setHasHeaderRow(request.getHasHeaderRow());
        }
        if (request.getBatchSize() != null) {
            config.setBatchSize(request.getBatchSize());
        }
        if (request.getAllowOverwrite() != null) {
            config.setAllowOverwrite(request.getAllowOverwrite());
        }

        config = gradeImportConfigRepository.save(config);
        return toResponse(config);
    }

    // ==================== 删除配置 ====================

    /**
     * 软删除成绩导入配置。
     *
     * @param userId 当前登录用户 ID
     * @param id     配置 ID
     */
    @Transactional
    public void deleteConfig(Long userId, Long id) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        // 先校验配置存在且归属当前学校（@SQLRestriction 已过滤已删除记录）
        requireOwnedConfig(id, schoolId);
        gradeImportConfigRepository.softDeleteById(id, LocalDateTime.now());
    }

    // ==================== 状态管理 ====================

    /**
     * 修改配置启用/禁用状态。
     *
     * @param userId 当前登录用户 ID
     * @param id     配置 ID
     * @param status 0=禁用 1=启用
     * @return 配置详情
     */
    @Transactional
    public GradeImportConfigResponse updateStatus(Long userId, Long id, Integer status) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能为 0 或 1");
        }

        GradeImportConfig config = requireOwnedConfig(id, schoolId);
        config.setStatus(status);
        config = gradeImportConfigRepository.save(config);
        return toResponse(config);
    }

    // ==================== 模板生成（供成绩导入模块调用） ====================

    /**
     * 生成当前学校配置的 .xlsx 模板字节（供 {@link AdminGradeService#importTemplate} 调用）。
     * <p>
     * 配置管理侧不再单独暴露模板下载接口，统一通过 {@code GET /admin/grades/import-template}
     * 下载，避免重复入口。
     *
     * @param userId 当前登录用户 ID
     * @return 标准 .xlsx 模板字节
     */
    public byte[] downloadTemplate(Long userId) {
        adminAuthService.requireAdminOrPermission(userId, PERMISSION);
        Long schoolId = adminAuthService.getOperatorSchoolId(userId);

        // 未配置时使用系统默认模板；学校配置被禁用时抛出业务异常
        GradeImportConfig config = requireEnabledConfig(schoolId);

        List<GradeImportFileParser.TemplateColumn> columns = parseTemplateColumns(config.getTemplateColumns());
        log.info("生成成绩导入模板, schoolId={}, columnCount={}, samples=null", schoolId, columns.size());
        return fileParser.buildXlsx(columns, null);
    }

    // ==================== 内部工具 ====================

    private GradeImportConfig requireOwnedConfig(Long id, Long schoolId) {
        GradeImportConfig config = gradeImportConfigRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "成绩导入配置不存在"));
        if (!Objects.equals(config.getSchoolId(), schoolId)) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "成绩导入配置不存在");
        }
        return config;
    }

    private void validateSaveRequest(GradeImportConfigSaveRequest request) {
        validateAllowedExtensions(request.getAllowedExtensions());
        validateTemplateColumns(request.getTemplateColumns());
        validateHeaderRow(request.getHasHeaderRow());
        if (request.getBatchSize() == null || request.getBatchSize() < 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "batchSize 必须大于等于 1");
        }
        if (request.getMaxFileSize() == null || request.getMaxFileSize() < 0) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "maxFileSize 不能为负数");
        }
        if (request.getAllowOverwrite() == null || (request.getAllowOverwrite() != 0 && request.getAllowOverwrite() != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "allowOverwrite 只能为 0 或 1");
        }
        if (request.getStatus() == null || (request.getStatus() != 0 && request.getStatus() != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能为 0 或 1");
        }
    }

    private void validateAllowedExtensions(List<String> extensions) {
        if (extensions == null || extensions.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "allowedExtensions 不能为空");
        }
        for (String ext : extensions) {
            if (ext == null || ext.isBlank() || !SUPPORTED_EXTENSIONS.contains(ext.toLowerCase())) {
                throw new BusinessException(ResultCode.PARAM_ERROR,
                        "allowedExtensions 仅支持: " + SUPPORTED_EXTENSIONS);
            }
        }
    }

    private void validateTemplateColumns(List<GradeImportConfigColumnItem> columns) {
        if (columns == null || columns.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "templateColumns 不能为空");
        }
        Set<String> fields = new HashSet<>();
        for (GradeImportConfigColumnItem col : columns) {
            if (col.getField() == null || col.getField().isBlank()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "templateColumns 中存在 field 为空");
            }
            if (col.getLabel() == null || col.getLabel().isBlank()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "templateColumns 中存在 label 为空");
            }
            if (!fields.add(col.getField())) {
                throw new BusinessException(ResultCode.PARAM_ERROR,
                        "templateColumns 中存在重复 field: " + col.getField());
            }
        }
        if (!fields.containsAll(REQUIRED_FIELDS)) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "templateColumns 必须包含字段: " + REQUIRED_FIELDS);
        }
    }

    private void validateHeaderRow(Integer hasHeaderRow) {
        if (hasHeaderRow == null || (hasHeaderRow != 0 && hasHeaderRow != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "hasHeaderRow 只能为 0 或 1");
        }
    }

    private List<GradeImportFileParser.TemplateColumn> parseTemplateColumns(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<GradeImportConfigColumnItem> items = objectMapper.readValue(json,
                    new TypeReference<List<GradeImportConfigColumnItem>>() {});
            List<GradeImportFileParser.TemplateColumn> columns = new ArrayList<>();
            for (GradeImportConfigColumnItem item : items) {
                columns.add(new GradeImportFileParser.TemplateColumn(
                        item.getField(), item.getLabel(), Boolean.TRUE.equals(item.getRequired())));
            }
            return columns;
        } catch (Exception e) {
            log.warn("template_columns 解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 构造学校未配置时使用的内存默认配置，不写入数据库。
     */
    private GradeImportConfig buildDefaultConfig(Long schoolId) {
        GradeImportConfig config = new GradeImportConfig();
        config.setSchoolId(schoolId);
        config.setAllowedExtensions(writeJson(DEFAULT_ALLOWED_EXTENSIONS));
        config.setMaxFileSize(DEFAULT_MAX_FILE_SIZE);
        config.setTemplateColumns(writeJson(DEFAULT_TEMPLATE_COLUMNS));
        config.setHasHeaderRow(DEFAULT_HAS_HEADER_ROW);
        config.setBatchSize(DEFAULT_BATCH_SIZE);
        config.setAllowOverwrite(DEFAULT_ALLOW_OVERWRITE);
        config.setStatus(1);
        return config;
    }

    /**
     * 构造系统默认模板的响应，{@code id} 为空表示尚未落库。
     */
    private GradeImportConfigResponse toDefaultResponse(Long schoolId) {
        return GradeImportConfigResponse.builder()
                .id(null)
                .configured(false)
                .source("DEFAULT")
                .schoolId(schoolId)
                .allowedExtensions(new ArrayList<>(DEFAULT_ALLOWED_EXTENSIONS))
                .maxFileSize(DEFAULT_MAX_FILE_SIZE)
                .templateColumns(new ArrayList<>(DEFAULT_TEMPLATE_COLUMNS))
                .hasHeaderRow(DEFAULT_HAS_HEADER_ROW)
                .batchSize(DEFAULT_BATCH_SIZE)
                .allowOverwrite(DEFAULT_ALLOW_OVERWRITE)
                .status(1)
                .build();
    }

    private GradeImportConfigResponse toResponse(GradeImportConfig config) {
        return GradeImportConfigResponse.builder()
                .id(config.getId())
                .configured(true)
                .source("SCHOOL")
                .schoolId(config.getSchoolId())
                .allowedExtensions(parseJsonList(config.getAllowedExtensions()))
                .maxFileSize(config.getMaxFileSize())
                .templateColumns(parseJsonColumns(config.getTemplateColumns()))
                .hasHeaderRow(config.getHasHeaderRow())
                .batchSize(config.getBatchSize())
                .allowOverwrite(config.getAllowOverwrite())
                .status(config.getStatus())
                .createdBy(config.getCreatedBy())
                .createdAt(toIso(config.getCreatedAt()))
                .updatedAt(toIso(config.getUpdatedAt()))
                .build();
    }

    private List<String> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> list = objectMapper.readValue(json, new TypeReference<List<String>>() {});
            return list != null ? list : List.of();
        } catch (Exception e) {
            log.warn("allowed_extensions 解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    private List<GradeImportConfigColumnItem> parseJsonColumns(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<GradeImportConfigColumnItem>>() {});
        } catch (Exception e) {
            log.warn("template_columns 解析失败: {}", e.getMessage());
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.SYS_ERROR, "配置 JSON 序列化失败");
        }
    }

    private static String toIso(LocalDateTime dt) {
        if (dt == null) {
            return null;
        }
        return dt.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE);
    }
}
