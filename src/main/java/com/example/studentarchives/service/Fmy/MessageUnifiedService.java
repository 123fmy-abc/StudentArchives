package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.message.response.MessageListItem;
import com.example.studentarchives.dto.Fmy.message.response.MessageListResponse;
import com.example.studentarchives.entity.foundation.Dictionary;
import com.example.studentarchives.entity.message.UserMessage;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.enums.MessageSenderTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.DictionaryRepository;
import com.example.studentarchives.repository.UserMessageRepository;
import com.example.studentarchives.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 统一消息列表服务（GET /messages/unified，Fmy 新增端点）
 * <p>
 * 修复《学生端接口文档》5.1 的归档语义缺口：既有 {@link MessageService#listMessages}
 * 在未传 {@code isArchived} 时固定按 0（仅未归档）查询，无法一次拉取"全部（含已归档）"，
 * 前端只能通过两态合并拉取兜底。本服务明确语义：
 * <ul>
 *   <li>不传 {@code isArchived} → 查询全部（含已归档/未归档）；</li>
 *   <li>传 {@code 0} / {@code 1} → 单态查询，行为与既有端点一致。</li>
 * </ul>
 * 其余筛选（category/isRead/keyword）与分页、返回结构（{@link MessageListResponse}）与
 * 既有端点完全一致，前端可直接替换调用。
 * <p>
 * 实现说明：使用 {@link EntityManager} 动态拼装 JPQL，未改动既有
 * {@link UserMessageRepository}。软删除过滤由实体 {@code @SQLRestriction} 自动生效；
 * 未读数统计复用既有 Repository 计数方法。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageUnifiedService {

    /** 消息分类枚举值（与字典 message_category 一致） */
    private static final List<String> ALL_CATEGORIES =
            Arrays.asList("system_notice", "audit_remind", "dynamic_remind", "private_message");

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final EntityManager entityManager;
    private final UserMessageRepository userMessageRepository;
    private final DictionaryRepository dictionaryRepository;
    private final UserRepository userRepository;

    /**
     * 获取统一消息列表
     *
     * @param userId     当前登录用户 ID
     * @param category   分类筛选，不传为全部
     * @param isRead     0=未读 1=已读，不传为全部
     * @param isArchived 0=未归档 1=已归档，不传则查询全部（含已归档）
     * @param keyword    搜索关键词（匹配 title 或 content）
     * @param pageParam  分页参数
     * @return 消息列表
     */
    @Transactional(readOnly = true)
    public MessageListResponse listMessages(Long userId, String category, Integer isRead,
                                            Integer isArchived, String keyword, PageParam pageParam) {
        category = normalizeCategory(category);
        if (isRead != null && isRead != 0 && isRead != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "isRead 只能为 0 或 1");
        }
        if (isArchived != null && isArchived != 0 && isArchived != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "isArchived 只能为 0 或 1");
        }

        int page = Math.max(pageParam.getPage(), 1);
        int perPage = Math.max(pageParam.getPerPage(), 1);

        Map<String, Object> params = new HashMap<>();
        String where = buildWhere(userId, category, isRead, isArchived, keyword, params);

        TypedQuery<Long> countQuery = entityManager.createQuery(
                "SELECT COUNT(m) FROM UserMessage m " + where, Long.class);
        applyParams(countQuery, params);
        long total = countQuery.getSingleResult();

        TypedQuery<UserMessage> query = entityManager.createQuery(
                "SELECT m FROM UserMessage m " + where + " ORDER BY m.createdAt DESC", UserMessage.class);
        applyParams(query, params);
        query.setFirstResult((page - 1) * perPage);
        query.setMaxResults(perPage);
        List<UserMessage> rows = query.getResultList();

        Map<String, String> categoryLabelMap = buildCategoryLabelMap();
        Map<Long, String> senderNameMap = buildSenderNameMap(rows);

        List<MessageListItem> items = rows.stream()
                .map(m -> toListItem(m, categoryLabelMap, senderNameMap))
                .collect(Collectors.toList());

        long unread = countUnread(userId, category, isArchived);

        PageResult.Pagination pagination = PageResult.Pagination.builder()
                .page(page)
                .perPage(perPage)
                .total(total)
                .totalPages((int) Math.ceil((double) total / perPage))
                .build();

        return MessageListResponse.builder()
                .total(total)
                .unread(unread)
                .list(items)
                .pagination(pagination)
                .build();
    }

    // ==================== 私有方法 ====================

    /** 动态拼装 WHERE 子句（isArchived 为空表示全部，不生成归档过滤） */
    private String buildWhere(Long userId, String category, Integer isRead, Integer isArchived,
                              String keyword, Map<String, Object> params) {
        StringBuilder sb = new StringBuilder("WHERE m.userId = :userId");
        params.put("userId", userId);
        if (category != null) {
            sb.append(" AND m.category = :category");
            params.put("category", category);
        }
        if (isRead != null) {
            sb.append(" AND m.isRead = :isRead");
            params.put("isRead", isRead);
        }
        if (isArchived != null) {
            sb.append(" AND m.isArchived = :isArchived");
            params.put("isArchived", isArchived);
        }
        if (keyword != null && !keyword.isBlank()) {
            sb.append(" AND (m.title LIKE :kw OR m.content LIKE :kw)");
            params.put("kw", "%" + keyword.trim() + "%");
        }
        return sb.toString();
    }

    private void applyParams(jakarta.persistence.Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }

    /**
     * 统计未读消息数。isArchived 为空（全部）时统计未读且未归档 + 未读且已归档之和，
     * 与"全部"范围的列表口径一致。
     */
    private long countUnread(Long userId, String category, Integer isArchived) {
        if (isArchived != null) {
            return category != null
                    ? userMessageRepository.countByUserIdAndIsReadAndIsArchivedAndCategory(userId, 0, isArchived, category)
                    : userMessageRepository.countByUserIdAndIsReadAndIsArchived(userId, 0, isArchived);
        }
        long unarchived = category != null
                ? userMessageRepository.countByUserIdAndIsReadAndIsArchivedAndCategory(userId, 0, 0, category)
                : userMessageRepository.countByUserIdAndIsReadAndIsArchived(userId, 0, 0);
        long archived = category != null
                ? userMessageRepository.countByUserIdAndIsReadAndIsArchivedAndCategory(userId, 0, 1, category)
                : userMessageRepository.countByUserIdAndIsReadAndIsArchived(userId, 0, 1);
        return unarchived + archived;
    }

    /** 归一化分类参数：null/空串/"all" 视为不过滤（返回 null）；其余必须属于枚举分类。 */
    private String normalizeCategory(String category) {
        if (category == null || category.isEmpty() || "all".equalsIgnoreCase(category)) {
            return null;
        }
        if (!ALL_CATEGORIES.contains(category)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的消息分类");
        }
        return category;
    }

    /** 将消息实体映射为列表项 DTO */
    private MessageListItem toListItem(UserMessage m, Map<String, String> categoryLabelMap,
                                       Map<Long, String> senderNameMap) {
        MessageSenderTypeEnum senderType = MessageSenderTypeEnum.of(m.getSenderType());
        Long senderId = m.getSenderId();
        return MessageListItem.builder()
                .id(m.getId())
                .category(m.getCategory())
                .categoryLabel(categoryLabelMap.getOrDefault(m.getCategory(), m.getCategory()))
                .title(m.getTitle())
                .content(m.getContent())
                .senderType(m.getSenderType())
                .senderTypeLabel(senderType != null ? senderType.getLabel() : null)
                .senderName(senderId != null ? senderNameMap.get(senderId) : null)
                .isRead(m.getIsRead())
                .readAt(toIso(m.getReadAt()))
                .isImportant(m.getIsImportant())
                .isArchived(m.getIsArchived())
                .archivedAt(toIso(m.getArchivedAt()))
                .deadline(toIso(m.getDeadline()))
                .jumpUrl(m.getJumpUrl())
                .sendChannel(m.getSendChannel())
                .relatedType(m.getRelatedType())
                .relatedId(m.getRelatedId())
                .createdAt(toIso(m.getCreatedAt()))
                .build();
    }

    /** 批量加载发送者姓名（sender_id → users.name） */
    private Map<Long, String> buildSenderNameMap(List<UserMessage> messages) {
        Set<Long> senderIds = messages.stream()
                .map(UserMessage::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (senderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userRepository.findByIdIn(senderIds).stream()
                .collect(Collectors.toMap(User::getId, User::getName, (a, b) -> a));
    }

    /** 加载消息分类展示名称（字典 message_category） */
    private Map<String, String> buildCategoryLabelMap() {
        return dictionaryRepository.findActiveByDictType("message_category").stream()
                .collect(Collectors.toMap(Dictionary::getDictCode, Dictionary::getDictName, (a, b) -> a, LinkedHashMap::new));
    }

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
