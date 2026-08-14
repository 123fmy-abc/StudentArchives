package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.abilitydimension.request.AbilityDimensionCreateRequest;
import com.example.studentarchives.dto.Fmy.abilitydimension.request.AbilityDimensionUpdateRequest;
import com.example.studentarchives.dto.Fmy.abilitydimension.response.AbilityDimensionResponse;
import com.example.studentarchives.entity.foundation.AbilityDimension;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AbilityDimensionRepository;
import com.example.studentarchives.repository.EvaluationIndicatorRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端能力维度配置服务
 * <p>
 * 提供能力维度的列表、创建、更新、删除（软删除）。
 * 所有接口需通过 {@link AdminAuthService#requireAdminOrPermission(Long, String...)} 校验
 * admin 角色或 indicator:manage 权限码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAbilityDimensionService {

    private final AbilityDimensionRepository abilityDimensionRepository;
    private final EvaluationIndicatorRepository evaluationIndicatorRepository;
    private final AdminAuthService adminAuthService;

    /**
     * 获取能力维度列表（含禁用）
     */
    @Transactional(readOnly = true)
    public List<AbilityDimensionResponse> list(Long userId) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        return abilityDimensionRepository.findAllByOrderBySortAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建能力维度
     */
    @Transactional
    public AbilityDimensionResponse create(Long userId, AbilityDimensionCreateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        validateDimensionCodeUnique(request.getDimensionCode(), null);

        AbilityDimension dimension = new AbilityDimension();
        dimension.setDimensionName(request.getDimensionName());
        dimension.setDimensionCode(request.getDimensionCode());
        dimension.setDescription(request.getDescription());
        dimension.setSort(request.getSort());
        dimension.setStatus(1);
        abilityDimensionRepository.save(dimension);

        log.info("创建能力维度: id={}, code={}, operatorId={}",
                dimension.getId(), dimension.getDimensionCode(), userId);
        return toResponse(dimension);
    }

    /**
     * 更新能力维度
     */
    @Transactional
    public AbilityDimensionResponse update(Long userId, Long id, AbilityDimensionUpdateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        AbilityDimension dimension = abilityDimensionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "能力维度不存在"));

        if (request.getDimensionName() != null) {
            dimension.setDimensionName(request.getDimensionName());
        }
        if (request.getDimensionCode() != null) {
            validateDimensionCodeUnique(request.getDimensionCode(), id);
            dimension.setDimensionCode(request.getDimensionCode());
        }
        if (request.getDescription() != null) {
            dimension.setDescription(request.getDescription());
        }
        if (request.getSort() != null) {
            dimension.setSort(request.getSort());
        }
        if (request.getStatus() != null) {
            dimension.setStatus(request.getStatus());
        }
        abilityDimensionRepository.save(dimension);

        log.info("更新能力维度: id={}, operatorId={}", id, userId);
        return toResponse(dimension);
    }

    /**
     * 删除能力维度（软删除）
     * <p>
     * 若存在未删除的指标引用该维度编码，返回 30006 数据关联存在。
     */
    @Transactional
    public void delete(Long userId, Long id) {
        adminAuthService.requireAdminOrPermission(userId, "indicator:manage");
        AbilityDimension dimension = abilityDimensionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "能力维度不存在"));

        if (evaluationIndicatorRepository.existsByDimensionCode(dimension.getDimensionCode())) {
            throw new BusinessException(ResultCode.DATA_RELATION_EXISTS,
                    "该能力维度已被指标引用，无法删除，建议禁用");
        }

        abilityDimensionRepository.softDeleteById(id, LocalDateTime.now());
        log.info("删除能力维度: id={}, code={}, operatorId={}", id, dimension.getDimensionCode(), userId);
    }

    private void validateDimensionCodeUnique(String dimensionCode, Long excludeId) {
        AbilityDimension existing = abilityDimensionRepository.findByDimensionCode(dimensionCode).orElse(null);
        if (existing != null && !Objects.equals(existing.getId(), excludeId)) {
            throw new BusinessException(ResultCode.DATA_DUPLICATE, "维度编码已存在");
        }
    }

    private AbilityDimensionResponse toResponse(AbilityDimension dimension) {
        return AbilityDimensionResponse.builder()
                .id(dimension.getId())
                .dimensionName(dimension.getDimensionName())
                .dimensionCode(dimension.getDimensionCode())
                .description(dimension.getDescription())
                .sort(dimension.getSort())
                .status(dimension.getStatus())
                .statusLabel(Integer.valueOf(1).equals(dimension.getStatus()) ? "启用" : "禁用")
                .build();
    }
}
