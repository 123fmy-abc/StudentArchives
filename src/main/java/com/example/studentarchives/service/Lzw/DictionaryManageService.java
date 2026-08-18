package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.foundation.Dictionary;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.DictionaryRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.persistence.criteria.Predicate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * 管理端字典数据管理服务（Lzw）
 * <p>
 * 对应《管理端接口文档》十、字典数据管理模块（10.1 ~ 10.5）。
 * 数据来源：dictionaries。
 * <p>
 * 权限：文档附录标注「管理端可增删改，公共端只读」，且关键权限码表未列出字典相关权限码，
 * 故 10.1~10.5 均要求 admin 角色（越权返回 20005）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DictionaryManageService {

    /** ISO 8601 带时区输出格式 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final DictionaryRepository dictionaryRepository;
    private final AdminAuthService adminAuthService;

    // ==================== 10.1 获取字典类型列表 ====================

    /**
     * 按 {@code dict_type} 分组统计字典类型列表。
     * <p>
     * 字典表无独立的「类型」实体，类型即若干字典项的 {@code dictType} 分组：
     * {@code itemCount} = 分组内未删除项总数；{@code status} = 分组内存在启用项即视为启用（1），
     * 否则禁用（0）；{@code createdAt} = 分组内最早创建时间。
     * keyword 模糊匹配类型编码（dictType）或任一字典项名称（dictName），status 按类型级状态过滤。
     */
    @Transactional(readOnly = true)
    public PageResult<DictTypeItem> listTypes(Long operatorId, String keyword, Integer status, PageParam pageParam) {
        adminAuthService.requireAdmin(operatorId);

        // 加载全部未删除字典项（@SQLRestriction 已过滤 deleted_at），在内存按类型分组
        List<Dictionary> all = dictionaryRepository.findAll();
        Map<String, List<Dictionary>> grouped = all.stream()
                .collect(Collectors.groupingBy(Dictionary::getDictType, TreeMap::new, Collectors.toList()));

        String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim().toLowerCase();
        List<DictTypeItem> typeItems = new ArrayList<>();

        for (Map.Entry<String, List<Dictionary>> entry : grouped.entrySet()) {
            String dictType = entry.getKey();
            List<Dictionary> items = entry.getValue();

            // 类型级状态：任一项启用即视为启用
            int typeStatus = items.stream().anyMatch(d -> Integer.valueOf(1).equals(d.getStatus())) ? 1 : 0;

            // keyword：类型编码 或 任一字典项名称 命中
            if (kw != null) {
                boolean match = dictType.toLowerCase().contains(kw)
                        || items.stream().anyMatch(d -> d.getDictName() != null
                                && d.getDictName().toLowerCase().contains(kw));
                if (!match) {
                    continue;
                }
            }

            // status 过滤
            if (status != null && typeStatus != status) {
                continue;
            }

            LocalDateTime earliest = items.stream()
                    .map(Dictionary::getCreatedAt)
                    .filter(Objects::nonNull)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);

            typeItems.add(DictTypeItem.builder()
                    .dictType(dictType)
                    .itemCount(items.size())
                    .status(typeStatus)
                    .createdAt(toIso(earliest))
                    .build());
        }

        // 内存分页
        int total = typeItems.size();
        int from = Math.min((pageParam.getPage() - 1) * pageParam.getPerPage(), total);
        int to = Math.min(from + pageParam.getPerPage(), total);
        List<DictTypeItem> pageList = from >= total ? List.of() : new ArrayList<>(typeItems.subList(from, to));
        return PageResult.of(pageList, total, pageParam);
    }

    // ==================== 10.2 获取字典项列表 ====================

    @Transactional(readOnly = true)
    public DictItemListResponse listItems(Long operatorId, String dictType, Integer status, PageParam pageParam) {
        adminAuthService.requireAdmin(operatorId);
        String type = requireNotBlank(dictType, "dictType 不能为空");

        Specification<Dictionary> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("dictType"), type));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.ASC, "sort").and(Sort.by(Sort.Direction.ASC, "id"));
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(), sort);
        Page<Dictionary> page = dictionaryRepository.findAll(spec, pageable);

        List<DictItemVO> items = page.getContent().stream().map(d -> DictItemVO.builder()
                .id(d.getId())
                .dictValue(d.getDictCode())
                .label(d.getDictName())
                .sort(d.getSort())
                .status(d.getStatus())
                .createdAt(toIso(d.getCreatedAt()))
                .build()).collect(Collectors.toList());

        PageResult<DictItemVO> pr = PageResult.of(items, page.getTotalElements(), pageParam);
        return DictItemListResponse.builder()
                .dictType(type)
                .list(pr.getList())
                .pagination(pr.getPagination())
                .build();
    }

    // ==================== 10.3 创建字典项 ====================

    /**
     * 创建字典项。若 {@code dictType} 不存在则自动创建新的字典类型
     * （字典类型无独立实体，创建首个字典项即隐式创建该类型）。
     */
    @Transactional
    public DictItemIdResponse createItem(Long operatorId, DictItemCreateRequest body) {
        adminAuthService.requireAdmin(operatorId);

        String dictType = requireNotBlank(body.getDictType(), "dictType 不能为空");
        String dictValue = requireNotBlank(body.getDictValue(), "dictValue 不能为空");
        String label = requireNotBlank(body.getLabel(), "label 不能为空");
        Integer sort = body.getSort() != null ? body.getSort() : 0;
        Integer status = body.getStatus() != null ? body.getStatus() : 1;
        validateStatus(status);

        dictionaryRepository.findByDictTypeAndDictCode(dictType, dictValue)
                .ifPresent(existing -> {
                    throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "该字典类型下已存在相同字典值");
                });

        Dictionary d = new Dictionary();
        d.setDictType(dictType);
        d.setDictCode(dictValue);
        d.setDictName(label);
        d.setSort(sort);
        d.setStatus(status);
        dictionaryRepository.save(d);

        return DictItemIdResponse.builder().id(d.getId()).build();
    }

    // ==================== 10.4 更新字典项 ====================

    @Transactional
    public void updateItem(Long operatorId, Long itemId, DictItemUpdateRequest body) {
        adminAuthService.requireAdmin(operatorId);

        Dictionary d = dictionaryRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "字典项不存在"));

        if (body.getDictValue() != null) {
            String newValue = body.getDictValue().trim();
            if (newValue.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "dictValue 不能为空");
            }
            if (!newValue.equals(d.getDictCode())) {
                dictionaryRepository.findByDictTypeAndDictCode(d.getDictType(), newValue)
                        .filter(existing -> !existing.getId().equals(itemId))
                        .ifPresent(existing -> {
                            throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "该字典类型下已存在相同字典值");
                        });
                d.setDictCode(newValue);
            }
        }

        if (body.getLabel() != null) {
            String label = body.getLabel().trim();
            if (label.isEmpty()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "label 不能为空");
            }
            d.setDictName(label);
        }

        if (body.getSort() != null) {
            d.setSort(body.getSort());
        }

        if (body.getRemark() != null) {
            d.setRemark(body.getRemark());
        }

        if (body.getStatus() != null) {
            validateStatus(body.getStatus());
            d.setStatus(body.getStatus());
        }

        dictionaryRepository.save(d);
    }

    // ==================== 10.5 删除字典项 ====================

    /**
     * 软删除字典项。已被引用（存在子级字典项，即 parent_id 指向本项）的禁止删除。
     */
    @Transactional
    public void deleteItem(Long operatorId, Long itemId) {
        adminAuthService.requireAdmin(operatorId);

        Dictionary d = dictionaryRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "字典项不存在"));

        if (dictionaryRepository.existsByParentId(itemId)) {
            throw new BusinessException(ResultCode.DATA_RELATION_EXISTS, "该字典项存在子级字典项，无法删除");
        }

        int updated = dictionaryRepository.softDeleteById(itemId, LocalDateTime.now());
        if (updated == 0) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "字典项不存在");
        }
    }

    // ==================== 通用辅助 ====================

    private String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, message);
        }
        return value.trim();
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能为 0(禁用) 或 1(启用)");
        }
    }

    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }

    // ==================== 内嵌 POJO ====================

    /** 10.1 字典类型列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DictTypeItem {
        private String dictType;
        private Integer itemCount;
        private Integer status;
        private String createdAt;
    }

    /** 10.2 字典项列表响应 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DictItemListResponse {
        private String dictType;
        private List<DictItemVO> list;
        private PageResult.Pagination pagination;
    }

    /** 10.2 字典项列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class DictItemVO {
        private Long id;
        private String dictValue;
        private String label;
        private Integer sort;
        private Integer status;
        private String createdAt;
    }

    /** 10.3 创建字典项请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DictItemCreateRequest {
        private String dictType;
        private String dictValue;
        private String label;
        private Integer sort;
        private Integer status;
    }

    /** 10.4 更新字典项请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DictItemUpdateRequest {
        private String dictValue;
        private String label;
        private Integer sort;
        private String remark;
        private Integer status;
    }

    /** 10.3 创建字典项响应 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DictItemIdResponse {
        private Long id;
    }
}
