package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.aifaq.request.AdminAiFaqCreateRequest;
import com.example.studentarchives.dto.Fmy.aifaq.request.AdminAiFaqUpdateRequest;
import com.example.studentarchives.dto.Fmy.aifaq.response.AiFaqQuestionResponse;
import com.example.studentarchives.entity.ai.AiFaqQuestion;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AiFaqQuestionRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 管理端 AI 常见问题配置服务
 * <p>
 * 提供 AI 助手常见问题的列表、创建、更新、删除（软删除）。
 * 所有接口需通过 {@link AdminAuthService#requireAdminOrPermission(Long, String...)} 校验
 * admin 角色或 ai:faq:manage 权限码。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAiFaqService {

    private final AiFaqQuestionRepository aiFaqQuestionRepository;
    private final AdminAuthService adminAuthService;

    /**
     * 获取常见问题列表（含禁用）
     */
    @Transactional(readOnly = true)
    public List<AiFaqQuestionResponse> list(Long userId) {
        adminAuthService.requireAdminOrPermission(userId, "ai:faq:manage");
        return aiFaqQuestionRepository.findAllByOrderBySortAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * 创建常见问题
     */
    @Transactional
    public AiFaqQuestionResponse create(Long userId, AdminAiFaqCreateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, "ai:faq:manage");

        AiFaqQuestion faq = new AiFaqQuestion();
        faq.setQuestion(request.getQuestion());
        faq.setSort(request.getSort());
        faq.setStatus(1);
        aiFaqQuestionRepository.save(faq);

        log.info("创建AI常见问题: id={}, operatorId={}", faq.getId(), userId);
        return toResponse(faq);
    }

    /**
     * 更新常见问题
     */
    @Transactional
    public AiFaqQuestionResponse update(Long userId, Long id, AdminAiFaqUpdateRequest request) {
        adminAuthService.requireAdminOrPermission(userId, "ai:faq:manage");
        AiFaqQuestion faq = aiFaqQuestionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "常见问题不存在"));

        if (request.getQuestion() != null) {
            faq.setQuestion(request.getQuestion());
        }
        if (request.getSort() != null) {
            faq.setSort(request.getSort());
        }
        if (request.getStatus() != null) {
            faq.setStatus(request.getStatus());
        }
        aiFaqQuestionRepository.save(faq);

        log.info("更新AI常见问题: id={}, operatorId={}", id, userId);
        return toResponse(faq);
    }

    /**
     * 删除常见问题（软删除）
     */
    @Transactional
    public void delete(Long userId, Long id) {
        adminAuthService.requireAdminOrPermission(userId, "ai:faq:manage");
        AiFaqQuestion faq = aiFaqQuestionRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "常见问题不存在"));

        aiFaqQuestionRepository.softDeleteById(id, LocalDateTime.now());
        log.info("删除AI常见问题: id={}, operatorId={}", id, userId);
    }

    private AiFaqQuestionResponse toResponse(AiFaqQuestion faq) {
        return AiFaqQuestionResponse.builder()
                .id(faq.getId())
                .question(faq.getQuestion())
                .sort(faq.getSort())
                .status(faq.getStatus())
                .statusLabel(Integer.valueOf(1).equals(faq.getStatus()) ? "启用" : "禁用")
                .build();
    }
}
