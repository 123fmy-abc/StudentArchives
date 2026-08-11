package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.form.request.FormTemplateCreateRequest;
import com.example.studentarchives.dto.Fmy.form.request.FormTemplateUpdateRequest;
import com.example.studentarchives.dto.Fmy.form.response.FormTemplateResponse;
import com.example.studentarchives.entity.foundation.FormTemplate;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.FormTemplateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端表单模板服务
 * <p>
 * 明确图片中“表单自定义”语义：管理可复用的业务表单模板（fields/layout_config）。
 * 模板发布时递增版本号，不影响已提交档案（对应 To Do List「已审核通过的数据不得直接删除或覆盖」）。
 * <p>
 * 信息发布请使用 {@link AdminAnnouncementService}，避免与“表单模板发布”混淆。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFormTemplateService {

    private final AdminAuthService adminAuthService;
    private final JsonSchemaValidator jsonSchemaValidator;
    private final FormTemplateRepository formTemplateRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<FormTemplateResponse> list(Long userId, Long schoolId) {
        adminAuthService.requireAdminOrPermission(userId, "form:view", "form:manage");
        return formTemplateRepository.findBySchoolIdAndStatusOrderByIdAsc(schoolId, 1).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FormTemplateResponse detail(Long userId, Long templateId) {
        adminAuthService.requireAdminOrPermission(userId, "form:view", "form:manage");
        FormTemplate template = formTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "表单模板不存在"));
        return toResponse(template);
    }

    @Transactional
    public FormTemplateResponse create(Long userId, FormTemplateCreateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, "form:manage");
        if (formTemplateRepository.existsBySchoolIdAndCode(request.getSchoolId(), request.getCode())) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "表单模板编码已存在");
        }
        validateJsonFields(request.getFields(), request.getLayoutConfig(), request.getApplicableRoles());

        FormTemplate template = new FormTemplate();
        template.setSchoolId(request.getSchoolId());
        template.setTemplateName(request.getTemplateName());
        template.setCode(request.getCode());
        template.setCategory(request.getCategory());
        template.setDescription(request.getDescription());
        template.setFields(toJson(request.getFields(), "fields"));
        template.setLayoutConfig(toJson(request.getLayoutConfig(), "layoutConfig"));
        template.setApplicableRoles(toJson(request.getApplicableRoles(), "applicableRoles"));
        template.setVersion(1);
        template.setStatus(1);
        template.setIsDefault(0);
        template.setCreatedBy(userId);
        template.setUpdatedBy(userId);
        formTemplateRepository.save(template);

        return toResponse(template);
    }

    @Transactional
    public FormTemplateResponse update(Long userId, Long templateId, FormTemplateUpdateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, "form:manage");
        FormTemplate template = formTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "表单模板不存在"));
        validateJsonFields(request.getFields(), request.getLayoutConfig(), request.getApplicableRoles());

        if (request.getTemplateName() != null) {
            template.setTemplateName(request.getTemplateName());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getFields() != null) {
            template.setFields(toJson(request.getFields(), "fields"));
        }
        if (request.getLayoutConfig() != null) {
            template.setLayoutConfig(toJson(request.getLayoutConfig(), "layoutConfig"));
        }
        if (request.getApplicableRoles() != null) {
            template.setApplicableRoles(toJson(request.getApplicableRoles(), "applicableRoles"));
        }
        if (request.getStatus() != null) {
            template.setStatus(request.getStatus());
        }
        template.setUpdatedBy(userId);
        formTemplateRepository.save(template);

        return toResponse(template);
    }

    /**
     * 发布表单模板：递增版本号，供后续申报使用新版本。
     */
    @Transactional
    public FormTemplateResponse publish(Long userId, Long templateId) {
        adminAuthService.requireAdminOrPermission(userId, "form:manage");
        FormTemplate template = formTemplateRepository.findById(templateId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "表单模板不存在"));
        template.setVersion(template.getVersion() == null ? 1 : template.getVersion() + 1);
        template.setUpdatedBy(userId);
        formTemplateRepository.save(template);
        log.info("发布表单模板: templateId={}, version={}, operatorId={}", templateId, template.getVersion(), userId);
        return toResponse(template);
    }

    private void validateJsonFields(JsonNode fields, JsonNode layoutConfig, JsonNode applicableRoles) {
        if (fields != null && !fields.isNull()) {
            jsonSchemaValidator.requireArray(fields.toString(), "form_templates.fields");
        }
        if (layoutConfig != null && !layoutConfig.isNull()) {
            jsonSchemaValidator.requireObject(layoutConfig.toString(), "form_templates.layout_config");
        }
        if (applicableRoles != null && !applicableRoles.isNull()) {
            jsonSchemaValidator.requireArray(applicableRoles.toString(), "form_templates.applicable_roles");
        }
    }

    private String toJson(JsonNode node, String fieldName) {
        if (node == null || node.isNull()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(node);
        } catch (Exception e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, fieldName + " JSON 序列化失败");
        }
    }

    private FormTemplateResponse toResponse(FormTemplate template) {
        return FormTemplateResponse.builder()
                .id(template.getId())
                .schoolId(template.getSchoolId())
                .templateName(template.getTemplateName())
                .code(template.getCode())
                .category(template.getCategory())
                .description(template.getDescription())
                .fields(parse(template.getFields()))
                .layoutConfig(parse(template.getLayoutConfig()))
                .applicableRoles(parse(template.getApplicableRoles()))
                .isDefault(template.getIsDefault())
                .version(template.getVersion())
                .status(template.getStatus())
                .createdAt(template.getCreatedAt() != null ? template.getCreatedAt().toString() : null)
                .updatedAt(template.getUpdatedAt() != null ? template.getUpdatedAt().toString() : null)
                .build();
    }

    private JsonNode parse(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            return null;
        }
    }
}
