package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.entity.message.UserMessage;
import com.example.studentarchives.enums.MessageSenderTypeEnum;
import com.example.studentarchives.repository.UserMessageRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 * 消息生产服务（消息中心"生产端"）
 * <p>
 * 统一封装 {@link UserMessage} 的创建逻辑，收敛字段默认值（senderType=1 系统、
 * isRead=0 未读、isArchived=0 未归档、sendChannel=push、isImportant 可配），
 * 并按照《学生端接口文档》5.1 的四类消息分类（system_notice / audit_remind /
 * dynamic_remind / private_message）提供语义化发送方法，供各业务事件调用。
 * <p>
 * 相比 {@link ProfileExportService} / {@link ResumeExportService} 中内联手写
 * UserMessage 的做法，本类是唯一推荐的消息写入入口，支持单发与批量粉丝播。
 * <p>
 * 事务说明：发送方法默认 {@link Propagation#REQUIRED}——若在业务事务内调用则与业务
 * 变更同事务提交（原子）；若在业务事务提交后调用则自行开启事务，两种场景消息均可靠落库。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageProducer {

    /** 消息分类（与字典 message_category 一致，见《学生端接口文档》5.1） */
    public static final String CATEGORY_SYSTEM_NOTICE = "system_notice";
    public static final String CATEGORY_AUDIT_REMIND = "audit_remind";
    public static final String CATEGORY_DYNAMIC_REMIND = "dynamic_remind";
    public static final String CATEGORY_PRIVATE_MESSAGE = "private_message";

    private final UserMessageRepository userMessageRepository;

    /**
     * 发送单条消息
     *
     * @param userId 接收者用户 ID（user_messages.user_id）
     * @param spec   消息内容与元信息
     * @return 持久化后的消息实体
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public UserMessage sendToUser(Long userId, MessageSpec spec) {
        if (userId == null || spec == null || spec.getTitle() == null) {
            log.warn("消息参数缺失，跳过发送 userId={} spec={}", userId, spec);
            return null;
        }
        UserMessage message = buildMessage(userId, spec);
        return userMessageRepository.save(message);
    }

    /**
     * 批量发送消息（公告等粉丝播场景）
     *
     * @param userIds 接收者用户 ID 集合（自动去重）
     * @param spec    消息内容与元信息
     * @return 实际落库消息数
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public int sendToUsers(Collection<Long> userIds, MessageSpec spec) {
        List<Long> distinct = userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinct.isEmpty() || spec == null || spec.getTitle() == null) {
            return 0;
        }
        List<UserMessage> messages = distinct.stream()
                .map(uid -> buildMessage(uid, spec))
                .toList();
        userMessageRepository.saveAll(messages);
        return messages.size();
    }

    /**
     * 审核提醒（audit_remind）：申报提交、审批通过/驳回、退回等审核类事件。
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public UserMessage auditRemind(Long userId, String title, String content,
                                   String relatedType, Long relatedId, String jumpUrl, Integer isImportant) {
        return sendToUser(userId, MessageSpec.auditRemind(title, content, relatedType, relatedId, jumpUrl, isImportant));
    }

    /**
     * 系统通知（system_notice）：公告发布、系统维护等站内公告类事件。
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public UserMessage systemNotice(Long userId, String title, String content,
                                    String relatedType, Long relatedId, String jumpUrl, Integer isImportant) {
        return sendToUser(userId, MessageSpec.systemNotice(title, content, relatedType, relatedId, jumpUrl, isImportant));
    }

    /**
     * 动态提醒（dynamic_remind）：成长时间轴更新、数据完整度提示等动态类事件。
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public UserMessage dynamicRemind(Long userId, String title, String content,
                                     String relatedType, Long relatedId, String jumpUrl, Integer isImportant) {
        return sendToUser(userId, MessageSpec.dynamicRemind(title, content, relatedType, relatedId, jumpUrl, isImportant));
    }

    /**
     * 私信（private_message）：人工/教师定向私信，senderType=2 人工。
     *
     * @param senderId 发送者用户 ID（users.id）
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public UserMessage privateMessage(Long userId, Long senderId, String title, String content,
                                      String relatedType, Long relatedId, String jumpUrl) {
        return sendToUser(userId, MessageSpec.builder()
                .category(CATEGORY_PRIVATE_MESSAGE)
                .senderType(MessageSenderTypeEnum.MANUAL.getValue())
                .senderId(senderId)
                .title(title)
                .content(content)
                .relatedType(relatedType)
                .relatedId(relatedId)
                .jumpUrl(jumpUrl)
                .build());
    }

    /** 按消息规格构建 UserMessage 实体（统一收敛默认值） */
    private UserMessage buildMessage(Long userId, MessageSpec spec) {
        UserMessage message = new UserMessage();
        message.setUserId(userId);
        message.setSenderType(spec.getSenderType() != null
                ? spec.getSenderType() : MessageSenderTypeEnum.SYSTEM.getValue());
        message.setSenderId(spec.getSenderId());
        message.setCategory(spec.getCategory());
        message.setTitle(spec.getTitle());
        message.setContent(spec.getContent());
        message.setRelatedType(spec.getRelatedType());
        message.setRelatedId(spec.getRelatedId());
        message.setSendChannel(spec.getSendChannel() != null ? spec.getSendChannel() : "push");
        message.setIsRead(0);
        message.setIsArchived(0);
        message.setIsImportant(spec.getIsImportant() != null ? spec.getIsImportant() : 0);
        message.setDeadline(spec.getDeadline());
        message.setJumpUrl(spec.getJumpUrl());
        return message;
    }

    /**
     * 消息发送规格（内部命令对象，非对外接口 DTO）
     */
    @Getter
    @Builder
    public static class MessageSpec {

        /** 消息分类（必填，四选一） */
        private String category;

        /** 消息标题（必填） */
        private String title;

        /** 消息正文 */
        private String content;

        /** 发送者类型：1=系统 2=人工 3=自动触发，默认 1 */
        private Integer senderType;

        /** 发送者用户 ID（senderType=2 人工私信时使用） */
        private Long senderId;

        /** 关联业务类型（如 archive / award_application / announcement） */
        private String relatedType;

        /** 关联业务 ID */
        private Long relatedId;

        /** 发送渠道，默认 push */
        private String sendChannel;

        /** 是否重要：1=重要（归档前需二次确认），默认 0 */
        private Integer isImportant;

        /** 截止时间（可选，如申报截止提醒） */
        private LocalDateTime deadline;

        /** 跳转地址（可选） */
        private String jumpUrl;

        /** 审核提醒规格工厂 */
        public static MessageSpec auditRemind(String title, String content,
                                              String relatedType, Long relatedId, String jumpUrl, Integer isImportant) {
            return MessageSpec.builder()
                    .category(CATEGORY_AUDIT_REMIND)
                    .senderType(MessageSenderTypeEnum.SYSTEM.getValue())
                    .title(title)
                    .content(content)
                    .relatedType(relatedType)
                    .relatedId(relatedId)
                    .jumpUrl(jumpUrl)
                    .isImportant(isImportant)
                    .build();
        }

        /** 系统通知规格工厂 */
        public static MessageSpec systemNotice(String title, String content,
                                               String relatedType, Long relatedId, String jumpUrl, Integer isImportant) {
            return MessageSpec.builder()
                    .category(CATEGORY_SYSTEM_NOTICE)
                    .senderType(MessageSenderTypeEnum.SYSTEM.getValue())
                    .title(title)
                    .content(content)
                    .relatedType(relatedType)
                    .relatedId(relatedId)
                    .jumpUrl(jumpUrl)
                    .isImportant(isImportant)
                    .build();
        }

        /** 动态提醒规格工厂 */
        public static MessageSpec dynamicRemind(String title, String content,
                                                String relatedType, Long relatedId, String jumpUrl, Integer isImportant) {
            return MessageSpec.builder()
                    .category(CATEGORY_DYNAMIC_REMIND)
                    .senderType(MessageSenderTypeEnum.SYSTEM.getValue())
                    .title(title)
                    .content(content)
                    .relatedType(relatedType)
                    .relatedId(relatedId)
                    .jumpUrl(jumpUrl)
                    .isImportant(isImportant)
                    .build();
        }
    }
}
