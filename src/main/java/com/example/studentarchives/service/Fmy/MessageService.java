package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.message.request.MessageSettingUpdateRequest;
import com.example.studentarchives.dto.Fmy.message.response.MessageArchiveResponse;
import com.example.studentarchives.dto.Fmy.message.response.MessageBatchDeleteResponse;
import com.example.studentarchives.dto.Fmy.message.response.MessageListItem;
import com.example.studentarchives.dto.Fmy.message.response.MessageListResponse;
import com.example.studentarchives.dto.Fmy.message.response.MessageReadAllResponse;
import com.example.studentarchives.dto.Fmy.message.response.MessageSettingItem;
import com.example.studentarchives.entity.foundation.Dictionary;
import com.example.studentarchives.entity.message.NotificationSetting;
import com.example.studentarchives.entity.message.UserMessage;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.enums.MessageSenderTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.DictionaryRepository;
import com.example.studentarchives.repository.NotificationSettingRepository;
import com.example.studentarchives.repository.UserMessageRepository;
import com.example.studentarchives.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 消息中心服务
 * <p>
 * 提供学生端消息中心模块接口（《学生端接口文档》五、消息中心模块）：
 * 消息列表、标记已读、批量标记已读、归档/取消归档、通知设置查询与更新。
 * <p>
 * 数据来源：user_messages、notification_settings、dictionaries（message_category）、users。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageService {

    /** 消息分类枚举值（与字典 message_category 一致） */
    private static final List<String> ALL_CATEGORIES =
            Arrays.asList("system_notice", "audit_remind", "dynamic_remind", "private_message");

    /** 未传 isArchived 时的默认值：仅查询未归档消息 */
    private static final int DEFAULT_IS_ARCHIVED = 0;

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final UserMessageRepository userMessageRepository;
    private final NotificationSettingRepository notificationSettingRepository;
    private final DictionaryRepository dictionaryRepository;
    private final UserRepository userRepository;

    // ==================== 5.1 获取消息列表 ====================

    /**
     * 获取消息列表（GET /messages）
     * <p>
     * 按 userId 过滤，支持 category/isRead/isArchived/keyword 筛选，
     * 按 createdAt 倒序分页返回。
     *
     * @param userId     当前登录用户 ID
     * @param category   分类筛选，不传为全部
     * @param isRead     0=未读 1=已读，不传为全部
     * @param isArchived 是否归档：0=未归档 1=已归档，不传默认 0
     * @param keyword    搜索关键词（匹配 title 或 content）
     * @param pageParam  分页参数（默认每页 10 条）
     * @return 消息列表
     */
    @Transactional(readOnly = true)
    public MessageListResponse listMessages(Long userId, String category, Integer isRead,
                                            Integer isArchived, String keyword, PageParam pageParam) {
        category = normalizeCategory(category);
        if (isRead != null && isRead != 0 && isRead != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "isRead 只能为 0 或 1");
        }
        int archived = isArchived != null ? isArchived : DEFAULT_IS_ARCHIVED;
        if (archived != 0 && archived != 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "isArchived 只能为 0 或 1");
        }

        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<UserMessage> page = userMessageRepository.search(
                userId, category, isRead, archived, keyword, pageable);

        Map<String, String> categoryLabelMap = buildCategoryLabelMap();
        Map<Long, String> senderNameMap = buildSenderNameMap(page.getContent());

        List<MessageListItem> items = page.getContent().stream()
                .map(m -> toListItem(m, categoryLabelMap, senderNameMap))
                .collect(Collectors.toList());

        long unread = category != null
                ? userMessageRepository.countByUserIdAndIsReadAndIsArchivedAndCategory(userId, 0, archived, category)
                : userMessageRepository.countByUserIdAndIsReadAndIsArchived(userId, 0, archived);

        PageResult.Pagination pagination = PageResult.Pagination.builder()
                .page(pageParam.getPage())
                .perPage(pageParam.getPerPage())
                .total(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .build();

        return MessageListResponse.builder()
                .total(page.getTotalElements())
                .unread(unread)
                .list(items)
                .pagination(pagination)
                .build();
    }

    // ==================== 5.2 标记单条消息已读 ====================

    /**
     * 标记消息已读（PUT /messages/{messageId}/read）
     * <p>
     * 更新 user_messages.is_read=1、read_at=CURRENT_TIMESTAMP；已读重复调用幂等。
     *
     * @param userId    当前登录用户 ID
     * @param messageId 消息 ID
     */
    @Transactional
    public void markRead(Long userId, Long messageId) {
        UserMessage message = getOwnedMessage(userId, messageId);
        if (!Integer.valueOf(1).equals(message.getIsRead())) {
            message.setIsRead(1);
            message.setReadAt(LocalDateTime.now());
            userMessageRepository.save(message);
        }
    }

    // ==================== 5.3 批量标记已读 ====================

    /**
     * 批量标记已读（PUT /messages/read-all）
     * <p>
     * 将当前用户未读消息标记为已读；category 可空（不传则标记全部未读）。
     *
     * @param userId   当前登录用户 ID
     * @param category 分类筛选，不传则标记所有未读
     * @return 标记条数
     */
    @Transactional
    public MessageReadAllResponse markAllRead(Long userId, String category) {
        category = normalizeCategory(category);
        int affected = userMessageRepository.markAllRead(userId, category, LocalDateTime.now());
        return MessageReadAllResponse.builder().markedCount(affected).build();
    }

    // ==================== 5.8 按 ID 批量标记已读 ====================

    /**
     * 按 ID 列表批量标记已读（PUT /messages/batch-read）
     * <p>
     * 仅标记当前用户未读消息；已读/非本人/已软删消息静默跳过，幂等。
     *
     * @param userId     当前登录用户 ID
     * @param messageIds 待标记消息 ID 列表
     * @return 本次实际标记条数
     */
    @Transactional
    public MessageReadAllResponse markBatchRead(Long userId, List<Long> messageIds) {
        int affected = userMessageRepository.markReadByIds(userId, messageIds, LocalDateTime.now());
        return MessageReadAllResponse.builder().markedCount(affected).build();
    }

    // ==================== 5.4 归档消息 ====================

    /**
     * 归档消息（PUT /messages/{messageId}/archive）
     * <p>
     * 更新 user_messages.is_archived=1、archived_at=CURRENT_TIMESTAMP，不做物理删除。
     * 重要系统消息（is_important=1）归档前需前端二次确认。
     *
     * @param userId    当前登录用户 ID
     * @param messageId 消息 ID
     * @return 归档结果
     */
    @Transactional
    public MessageArchiveResponse archive(Long userId, Long messageId) {
        UserMessage message = getOwnedMessage(userId, messageId);
        LocalDateTime now = LocalDateTime.now();
        message.setIsArchived(1);
        message.setArchivedAt(now);
        userMessageRepository.save(message);
        return MessageArchiveResponse.builder()
                .messageId(message.getId())
                .isArchived(1)
                .archivedAt(toIso(now))
                .build();
    }

    // ==================== 5.5 取消归档消息 ====================

    /**
     * 取消归档消息（PUT /messages/{messageId}/unarchive）
     * <p>
     * 将 user_messages.is_archived 重置为 0、archived_at 置空。
     *
     * @param userId    当前登录用户 ID
     * @param messageId 消息 ID
     * @return 取消归档结果
     */
    @Transactional
    public MessageArchiveResponse unarchive(Long userId, Long messageId) {
        UserMessage message = getOwnedMessage(userId, messageId);
        message.setIsArchived(0);
        message.setArchivedAt(null);
        userMessageRepository.save(message);
        return MessageArchiveResponse.builder()
                .messageId(message.getId())
                .isArchived(0)
                .archivedAt(null)
                .build();
    }

    // ==================== 5.9 删除单条消息 ====================

    /**
     * 删除单条消息（DELETE /messages/{messageId}）
     * <p>
     * 软删除：置 user_messages.deleted_at=当前时间，配合 @SQLRestriction 自动过滤，
     * 不做物理删除。归属/存在校验与 5.2/5.4 一致。
     *
     * @param userId    当前登录用户 ID
     * @param messageId 消息 ID
     */
    @Transactional
    public void deleteMessage(Long userId, Long messageId) {
        getOwnedMessage(userId, messageId);
        userMessageRepository.softDeleteById(messageId, LocalDateTime.now());
    }

    // ==================== 5.10 按 ID 批量删除消息 ====================

    /**
     * 按 ID 列表批量删除消息（DELETE /messages/batch）
     * <p>
     * 软删除当前用户消息；非本人/已软删消息静默跳过，幂等。
     *
     * @param userId     当前登录用户 ID
     * @param messageIds 待删除消息 ID 列表
     * @return 本次实际软删除条数
     */
    @Transactional
    public MessageBatchDeleteResponse deleteMessages(Long userId, List<Long> messageIds) {
        int affected = userMessageRepository.softDeleteByIds(userId, messageIds, LocalDateTime.now());
        return MessageBatchDeleteResponse.builder().deletedCount(affected).build();
    }

    // ==================== 5.6 获取消息通知设置 ====================

    /**
     * 获取消息通知设置（GET /messages/settings）
     * <p>
     * 返回全部 4 个分类的设置；未配置的分类返回默认值
     * （emailEnabled=1、smsEnabled=0、pushEnabled=1）。
     *
     * @param userId 当前登录用户 ID
     * @return 通知设置列表
     */
    @Transactional(readOnly = true)
    public List<MessageSettingItem> getSettings(Long userId) {
        Map<String, NotificationSetting> settingMap = notificationSettingRepository.findByUserId(userId).stream()
                .collect(Collectors.toMap(NotificationSetting::getCategory, Function.identity(), (a, b) -> a));
        Map<String, String> labelMap = buildCategoryLabelMap();

        return ALL_CATEGORIES.stream()
                .map(category -> {
                    NotificationSetting setting = settingMap.get(category);
                    return MessageSettingItem.builder()
                            .category(category)
                            .categoryLabel(labelMap.getOrDefault(category, category))
                            .emailEnabled(setting != null ? setting.getEmailEnabled() : 1)
                            .smsEnabled(setting != null ? setting.getSmsEnabled() : 0)
                            .pushEnabled(setting != null ? setting.getPushEnabled() : 1)
                            .build();
                })
                .collect(Collectors.toList());
    }

    // ==================== 5.7 更新消息通知设置 ====================

    /**
     * 更新消息通知设置（PUT /messages/settings）
     * <p>
     * 条件唯一索引 (user_id, category)，不存在则新增，存在则更新。
     *
     * @param userId  当前登录用户 ID
     * @param request 更新请求
     */
    @Transactional
    public void updateSetting(Long userId, MessageSettingUpdateRequest request) {
        String category = request.getCategory();
        if (!ALL_CATEGORIES.contains(category)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的通知分类");
        }
        NotificationSetting setting = notificationSettingRepository.findByUserIdAndCategory(userId, category)
                .orElseGet(() -> {
                    NotificationSetting s = new NotificationSetting();
                    s.setUserId(userId);
                    s.setCategory(category);
                    return s;
                });
        setting.setEmailEnabled(request.getEmailEnabled());
        setting.setSmsEnabled(request.getSmsEnabled());
        setting.setPushEnabled(request.getPushEnabled());
        notificationSettingRepository.save(setting);
    }

    // ==================== 私有方法 ====================

    /**
     * 归一化分类参数：null/空串/"all" 视为不过滤（返回 null）；
     * 其余取值必须属于消息分类枚举，否则返回 PARAM_ERROR。
     */
    private String normalizeCategory(String category) {
        if (category == null || category.isEmpty() || "all".equalsIgnoreCase(category)) {
            return null;
        }
        if (!ALL_CATEGORIES.contains(category)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的消息分类");
        }
        return category;
    }

    /** 查询并校验消息归属（越权统一返回 20005 无访问权限） */
    private UserMessage getOwnedMessage(Long userId, Long messageId) {
        UserMessage message = userMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "消息不存在"));
        if (!message.getUserId().equals(userId)) {
            throw new BusinessException(ResultCode.ACCESS_DENIED, "无访问权限");
        }
        return message;
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
                .collect(Collectors.toMap(Dictionary::getDictCode, Dictionary::getDictName, (a, b) -> a));
    }

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
