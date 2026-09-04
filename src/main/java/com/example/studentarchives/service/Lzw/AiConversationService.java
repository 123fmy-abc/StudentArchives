package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.config.Lzw.DeepSeekProperties;
import com.example.studentarchives.entity.ai.AiConversation;
import com.example.studentarchives.entity.ai.AiGenerationLog;
import com.example.studentarchives.entity.ai.AiMessage;
import com.example.studentarchives.entity.ai.AiMessageFeedback;
import com.example.studentarchives.entity.ai.AiTeacherFeedback;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.career.CareerPlan;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.weakness.ImprovementSuggestion;
import com.example.studentarchives.entity.weakness.WeaknessAnalysis;
import com.example.studentarchives.enums.AIFeedbackActionEnum;
import com.example.studentarchives.enums.AISuggestionSourceEnum;
import com.example.studentarchives.enums.APICallStatusEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AiConversationRepository;
import com.example.studentarchives.repository.AiGenerationLogRepository;
import com.example.studentarchives.repository.AiMessageFeedbackRepository;
import com.example.studentarchives.repository.AiMessageRepository;
import com.example.studentarchives.repository.AiTeacherFeedbackRepository;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.CareerPlanRepository;
import com.example.studentarchives.repository.ImprovementSuggestionRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.repository.WeaknessAnalysisRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 对话模块服务
 * <p>
 * 覆盖 7 个端点：创建会话、会话列表、消息列表、发送消息（含 AI 生成）、重新生成、AI 建议、删除会话。
 * AI 回复由 DeepSeek 大模型生成（见 buildAiReply），调用失败时返回兜底话术。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiConversationService {

    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    /** 会话默认状态：正常 */
    private static final int STATUS_NORMAL = 1;
    /** 消息角色 */
    private static final String ROLE_USER = "user";
    private static final String ROLE_ASSISTANT = "assistant";
    private static final String ROLE_SYSTEM = "system";
    /** 多轮对话带入上下文的历史条数上限 */
    private static final int HISTORY_LIMIT = 10;
    /** AI 助手系统提示词（学生成长档案助手） */
    private static final String SYSTEM_PROMPT =
            "你是「学生成长档案」系统的 AI 助手，为学生提供档案申报、奖项报名、"
            + "职业规划、短板改进等方面的咨询与建议。请用简洁、友好的中文回答；"
            + "若问题超出档案系统范围，请礼貌说明并建议学生联系老师。";
    /** 生成记录关联类型：AI 消息 / 改进建议 */
    private static final String RELATED_AI_MESSAGE = "ai_message";
    private static final String RELATED_SUGGESTION = "improvement_suggestion";
    /** 学生端消息反馈枚举 */
    private static final String FEEDBACK_USEFUL = "useful";
    private static final String FEEDBACK_USELESS = "useless";

    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final AiMessageFeedbackRepository messageFeedbackRepository;
    private final AiGenerationLogRepository generationLogRepository;
    private final AiTeacherFeedbackRepository teacherFeedbackRepository;
    private final ImprovementSuggestionRepository suggestionRepository;
    private final WeaknessAnalysisRepository weaknessAnalysisRepository;
    private final ArchiveRepository archiveRepository;
    private final CareerPlanRepository careerPlanRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final DeepSeekClient deepSeekClient;
    private final DeepSeekProperties deepSeekProperties;

    // ==================== 9.1 创建对话会话 ====================

    @Transactional
    public ConversationCreateResponse createConversation(CreateConversationRequest body, Long userId) {
        User user = loadUser(userId);

        AiConversation conv = new AiConversation();
        conv.setSchoolId(user.getSchoolId() != null ? user.getSchoolId() : 1L);
        conv.setUserId(userId);
        String title = body == null ? null : body.getTitle();
        conv.setTitle((title == null || title.isBlank()) ? "新的对话" : title.trim());
        conv.setContext(body != null && body.getContext() != null ? writeJson(body.getContext()) : null);
        conv.setStatus(STATUS_NORMAL);
        conversationRepository.save(conv);

        ConversationCreateResponse resp = new ConversationCreateResponse();
        resp.setConversationId(conv.getId());
        resp.setTitle(conv.getTitle());
        resp.setStatus(conv.getStatus());
        resp.setCreatedAt(toIso(conv.getCreatedAt()));
        return resp;
    }

    // ==================== 9.2 获取对话会话列表 ====================

    @Transactional(readOnly = true)
    public PageResult<ConversationListItem> listConversations(Long userId, PageParam pageParam) {
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage());
        Page<AiConversation> page = conversationRepository
                .findByUserIdAndStatusOrderByUpdatedAtDesc(userId, STATUS_NORMAL, pageable);

        List<ConversationListItem> list = page.getContent().stream().map(c -> {
            ConversationListItem item = new ConversationListItem();
            item.setId(c.getId());
            item.setTitle(c.getTitle());
            item.setStatus(c.getStatus());
            item.setStatusLabel(statusLabel(c.getStatus()));
            item.setLastMessageTime(toIso(c.getUpdatedAt()));
            item.setCreatedAt(toIso(c.getCreatedAt()));
            return item;
        }).collect(Collectors.toList());

        return PageResult.of(list, page.getTotalElements(), pageParam);
    }

    // ==================== 9.3 获取对话消息列表 ====================

    @Transactional(readOnly = true)
    public ConversationMessagesResponse getMessages(Long conversationId, Long userId) {
        AiConversation conv = loadOwnedConversation(conversationId, userId);
        List<AiMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);

        // 回显当前用户已提交的消息反馈（切回历史对话时）
        List<Long> messageIds = messages.stream().map(AiMessage::getId).collect(Collectors.toList());
        Map<Long, String> feedbackMap = messageIds.isEmpty()
                ? Collections.emptyMap()
                : messageFeedbackRepository.findByUserIdAndMessageIdIn(userId, messageIds).stream()
                        .collect(Collectors.toMap(AiMessageFeedback::getMessageId, AiMessageFeedback::getFeedback));

        List<MessageItem> items = messages.stream().map(m -> {
            MessageItem item = new MessageItem();
            item.setId(m.getId());
            item.setRole(m.getRole());
            item.setContent(m.getContent());
            item.setModelName(m.getModelName());
            item.setTokenUsage(m.getTokenUsage());
            item.setGenerationTimeMs(m.getGenerationTimeMs());
            item.setFeedback(feedbackMap.get(m.getId()));
            item.setCreatedAt(toIso(m.getCreatedAt()));
            return item;
        }).collect(Collectors.toList());

        ConversationMessagesResponse resp = new ConversationMessagesResponse();
        resp.setConversationId(conv.getId());
        resp.setTitle(conv.getTitle());
        resp.setMessages(items);
        return resp;
    }

    // ==================== 9.4 发送消息 ====================

    @Transactional
    public SendMessageResponse sendMessage(Long conversationId, SendMessageRequest body, Long userId) {
        AiConversation conv = loadOwnedConversation(conversationId, userId);
        if (conv.getStatus() != null && conv.getStatus() != STATUS_NORMAL) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "会话已禁用");
        }
        String content = body == null ? null : body.getContent();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "消息内容不能为空");
        }
        LocalDateTime now = LocalDateTime.now();

        // 1. 写入用户消息
        AiMessage userMsg = new AiMessage();
        userMsg.setConversationId(conversationId);
        userMsg.setRole(ROLE_USER);
        userMsg.setContent(content);
        messageRepository.save(userMsg);

        // 2. 调用 AI 生成回复（带上当前会话历史作为上下文）
        List<AiMessage> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        AiReply reply = buildAiReply(history);

        // 3. 写入助手消息
        AiMessage assistantMsg = new AiMessage();
        assistantMsg.setConversationId(conversationId);
        assistantMsg.setRole(ROLE_ASSISTANT);
        assistantMsg.setContent(reply.getContent());
        assistantMsg.setModelName(reply.getModelName());
        assistantMsg.setTokenUsage(reply.getTokenUsage());
        assistantMsg.setGenerationTimeMs(reply.getGenerationTimeMs());
        messageRepository.save(assistantMsg);

        // 4. 更新会话最后对话时间
        conv.setUpdatedAt(now);
        conversationRepository.save(conv);

        // 5. 写入生成日志
        writeGenerationLog(userId, assistantMsg.getId(), content, reply.getContent(), reply, APICallStatusEnum.SUCCESS, null);

        // 6. 组装返回
        SendMessageResponse resp = new SendMessageResponse();
        resp.setMessageId(assistantMsg.getId());
        resp.setRole(ROLE_ASSISTANT);
        resp.setContent(reply.getContent());
        resp.setModelName(reply.getModelName());
        resp.setTokenUsage(reply.getTokenUsage());
        resp.setGenerationTimeMs(reply.getGenerationTimeMs());
        resp.setCreatedAt(toIso(assistantMsg.getCreatedAt()));
        resp.setSuggestedActions(buildSuggestedActions(content));
        return resp;
    }

    // ==================== 9.5 重新生成 AI 消息 ====================

    @Transactional
    public RegenerateResponse regenerate(Long conversationId, Long messageId, Long userId) {
        loadOwnedConversation(conversationId, userId);

        AiMessage original = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "消息不存在"));
        if (!conversationId.equals(original.getConversationId())) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "消息不属于该会话");
        }
        if (!ROLE_ASSISTANT.equals(original.getRole())) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "仅助手消息可重新生成");
        }

        // 定位原调用记录
        AiGenerationLog originalLog = generationLogRepository
                .findFirstByRelatedTypeAndRelatedIdOrderByIdDesc(RELATED_AI_MESSAGE, messageId).orElse(null);

        // 重新调用 AI：带上该消息之前的会话历史（排除被替换的旧回复）
        List<AiMessage> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId)
                .stream().filter(m -> m.getId() < original.getId()).collect(Collectors.toList());
        String prompt = findLastUserContent(conversationId);
        if (prompt == null) {
            prompt = original.getContent();
        }
        AiReply reply = buildAiReply(history);
        LocalDateTime now = LocalDateTime.now();

        AiMessage newMsg = new AiMessage();
        newMsg.setConversationId(conversationId);
        newMsg.setRole(ROLE_ASSISTANT);
        newMsg.setContent(reply.getContent());
        newMsg.setModelName(reply.getModelName());
        newMsg.setTokenUsage(reply.getTokenUsage());
        newMsg.setGenerationTimeMs(reply.getGenerationTimeMs());
        messageRepository.save(newMsg);

        AiConversation conv = conversationRepository.findById(conversationId).orElse(null);
        if (conv != null) {
            conv.setUpdatedAt(now);
            conversationRepository.save(conv);
        }

        // 新调用记录 retry_of 指向原生成记录
        writeGenerationLog(userId, newMsg.getId(), prompt, reply.getContent(), reply, APICallStatusEnum.RETRY,
                originalLog != null ? originalLog.getId() : null);

        RegenerateResponse resp = new RegenerateResponse();
        resp.setMessageId(newMsg.getId());
        resp.setRole(ROLE_ASSISTANT);
        resp.setContent(reply.getContent());
        resp.setModelName(reply.getModelName());
        resp.setModelVersion(reply.getModelVersion());
        resp.setTokenUsage(reply.getTokenUsage());
        resp.setGenerationTimeMs(reply.getGenerationTimeMs());
        resp.setCallStatus(APICallStatusEnum.SUCCESS.getValue());
        resp.setRetry(true);
        resp.setCreatedAt(toIso(newMsg.getCreatedAt()));
        return resp;
    }

    // ==================== 9.6 获取 AI 辅助建议 ====================

    @Transactional(readOnly = true)
    public SuggestionsResponse getSuggestions(String sourceType, Long sourceId, Long userId) {
        if (sourceType == null || sourceType.isBlank() || sourceId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "sourceType 与 sourceId 必填");
        }

        List<Long> weaknessIds = Collections.emptyList();
        SuggestionSourceArchive sourceArchive = null;

        switch (sourceType) {
            case "weakness_analysis" -> {
                WeaknessAnalysis wa = weaknessAnalysisRepository.findById(sourceId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "短板分析不存在"));
                if (!userId.equals(wa.getUserId())) {
                    throw new BusinessException(ResultCode.FORBIDDEN, "无操作权限");
                }
                weaknessIds = Collections.singletonList(wa.getId());
                if ("archive".equals(wa.getRelatedType()) && wa.getRelatedId() != null) {
                    sourceArchive = buildSourceArchive(wa.getRelatedId());
                }
            }
            case "archive" -> {
                Archive a = archiveRepository.findById(sourceId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "档案不存在"));
                if (!userId.equals(a.getUserId())) {
                    throw new BusinessException(ResultCode.FORBIDDEN, "无操作权限");
                }
                sourceArchive = new SuggestionSourceArchive(a.getId(), a.getTitle());
                weaknessIds = weaknessAnalysisRepository
                        .findByUserIdAndRelatedTypeAndRelatedId(userId, "archive", sourceId).stream()
                        .map(WeaknessAnalysis::getId).collect(Collectors.toList());
            }
            case "career_plan" -> {
                CareerPlan cp = careerPlanRepository.findById(sourceId)
                        .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "职业规划不存在"));
                if (!userId.equals(cp.getUserId())) {
                    throw new BusinessException(ResultCode.FORBIDDEN, "无操作权限");
                }
                weaknessIds = weaknessAnalysisRepository
                        .findByUserIdAndRelatedTypeAndRelatedId(userId, "career_plan", sourceId).stream()
                        .map(WeaknessAnalysis::getId).collect(Collectors.toList());
            }
            default -> throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的 sourceType");
        }

        if (weaknessIds.isEmpty()) {
            SuggestionsResponse empty = new SuggestionsResponse();
            empty.setList(Collections.emptyList());
            return empty;
        }

        List<ImprovementSuggestion> suggestions = suggestionRepository.findByWeaknessIdIn(weaknessIds);
        Map<Long, Integer> teacherActionMap = buildTeacherActionMap(suggestions);

        List<SuggestionItem> items = new ArrayList<>();
        for (ImprovementSuggestion s : suggestions) {
            SuggestionItem item = new SuggestionItem();
            item.setSuggestionId(s.getId());
            item.setContent(s.getSuggestionContent());
            item.setSourceArchives(sourceArchive != null
                    ? Collections.singletonList(sourceArchive) : Collections.emptyList());
            boolean aiGenerated = s.getSource() != null && s.getSource() == AISuggestionSourceEnum.AI.getValue();
            item.setAiGenerated(aiGenerated);
            item.setAiWarning(aiGenerated ? "AI辅助生成，请教师复核" : null);
            int action = teacherActionMap.getOrDefault(s.getId(), 0);
            item.setTeacherAction(action);
            item.setTeacherActionLabel(teacherActionLabel(action));
            item.setCreatedAt(toIso(s.getCreatedAt()));
            items.add(item);
        }

        SuggestionsResponse resp = new SuggestionsResponse();
        resp.setList(items);
        return resp;
    }

    // ==================== 9.7 删除对话会话 ====================

    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        loadOwnedConversation(conversationId, userId);
        LocalDateTime now = LocalDateTime.now();
        messageRepository.softDeleteByConversationId(conversationId, now);
        conversationRepository.softDeleteById(conversationId, now);
    }

    // ==================== 9.8 消息反馈 ====================

    @Transactional
    public MessageFeedbackResponse submitFeedback(Long messageId, MessageFeedbackRequest body, Long userId) {
        String feedback = body == null ? null : body.getFeedback();
        if (feedback == null || feedback.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "反馈内容不能为空");
        }
        if (!FEEDBACK_USEFUL.equals(feedback) && !FEEDBACK_USELESS.equals(feedback)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "反馈值必须为 useful 或 useless");
        }

        AiMessage message = messageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "消息不存在"));
        loadOwnedConversation(message.getConversationId(), userId);

        // 幂等：同一条消息重复反馈时覆盖已有记录
        AiMessageFeedback record = messageFeedbackRepository
                .findByMessageIdAndUserId(messageId, userId)
                .orElseGet(AiMessageFeedback::new);
        record.setUserId(userId);
        record.setMessageId(messageId);
        record.setFeedback(feedback);
        messageFeedbackRepository.save(record);

        MessageFeedbackResponse resp = new MessageFeedbackResponse();
        resp.setMessageId(messageId);
        resp.setFeedback(feedback);
        return resp;
    }

    // ==================== 私有工具方法 ====================

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.TOKEN_INVALID, "用户不存在"));
    }

    private AiConversation loadOwnedConversation(Long conversationId, Long userId) {
        AiConversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "会话不存在"));
        if (!userId.equals(conv.getUserId())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无操作权限");
        }
        return conv;
    }

    /**
     * 调用 DeepSeek 生成回复：系统提示词 + 最近历史消息作为上下文；失败时返回兜底话术。
     */
    private AiReply buildAiReply(List<AiMessage> history) {
        List<DeepSeekClient.ChatMessage> messages = new ArrayList<>();
        messages.add(new DeepSeekClient.ChatMessage(ROLE_SYSTEM, SYSTEM_PROMPT));
        int from = Math.max(0, history.size() - HISTORY_LIMIT);
        for (int i = from; i < history.size(); i++) {
            AiMessage m = history.get(i);
            if (m.getRole() == null || m.getContent() == null || m.getContent().isBlank()) {
                continue;
            }
            messages.add(new DeepSeekClient.ChatMessage(m.getRole(), m.getContent()));
        }

        try {
            DeepSeekClient.ChatResult result = deepSeekClient.chat(messages);
            AiReply reply = new AiReply();
            reply.setContent(result.content());
            reply.setModelName(result.model());
            reply.setModelVersion(null);
            reply.setTokenUsage(result.totalTokens());
            reply.setGenerationTimeMs((int) result.generationTimeMs());
            return reply;
        } catch (Exception e) {
            log.error("DeepSeek 调用失败，返回兜底话术", e);
            AiReply reply = new AiReply();
            reply.setContent("抱歉，AI 服务暂时不可用，请稍后重试。");
            reply.setModelName(deepSeekProperties.getModel());
            reply.setModelVersion(null);
            reply.setTokenUsage(0);
            reply.setGenerationTimeMs(0);
            return reply;
        }
    }

    private void writeGenerationLog(Long userId, Long relatedId, String input, String output,
                                    AiReply reply, APICallStatusEnum callStatus, Long retryOf) {
        AiGenerationLog log = new AiGenerationLog();
        log.setUserId(userId);
        log.setGenerationType("ai_conversation");
        log.setRelatedType(RELATED_AI_MESSAGE);
        log.setRelatedId(relatedId);
        log.setInputData(writeJson(Collections.singletonMap("content", input)));
        log.setOutputContent(output);
        log.setModelName(reply.getModelName());
        log.setModelVersion(reply.getModelVersion());
        log.setTokenUsage(reply.getTokenUsage());
        log.setGenerationTimeMs(reply.getGenerationTimeMs());
        log.setCallStatus(callStatus.getValue());
        log.setRetryOf(retryOf);
        log.setIsUsed(1);
        generationLogRepository.save(log);
    }

    private String findLastUserContent(Long conversationId) {
        List<AiMessage> messages = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (ROLE_USER.equals(messages.get(i).getRole())) {
                return messages.get(i).getContent();
            }
        }
        return null;
    }

    private List<SuggestedAction> buildSuggestedActions(String content) {
        List<SuggestedAction> actions = new ArrayList<>();
        if (containsAny(content, "手机号", "电话", "联系方式")) {
            actions.add(new SuggestedAction("修改联系方式", "/profile/contact", "navigate"));
        }
        if (containsAny(content, "导出", "档案")) {
            actions.add(new SuggestedAction("导出个人档案", "/export", "navigate"));
        }
        if (containsAny(content, "密码", "登录", "账号")) {
            actions.add(new SuggestedAction("修改密码", "/profile/security", "navigate"));
        }
        return actions;
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        for (String kw : keywords) {
            if (text.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    private Map<Long, Integer> buildTeacherActionMap(List<ImprovementSuggestion> suggestions) {
        if (suggestions.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> suggestionIds = suggestions.stream().map(ImprovementSuggestion::getId).collect(Collectors.toList());
        List<AiGenerationLog> logs = generationLogRepository
                .findByRelatedTypeAndRelatedIdIn(RELATED_SUGGESTION, suggestionIds);
        if (logs.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> logToSuggestion = new HashMap<>();
        for (AiGenerationLog l : logs) {
            if (l.getRelatedId() != null) {
                logToSuggestion.putIfAbsent(l.getId(), l.getRelatedId());
            }
        }
        List<AiTeacherFeedback> feedbacks = teacherFeedbackRepository
                .findByGenerationLogIdIn(logToSuggestion.keySet());

        Map<Long, Integer> result = new HashMap<>();
        for (AiTeacherFeedback f : feedbacks) {
            Long suggestionId = logToSuggestion.get(f.getGenerationLogId());
            if (suggestionId != null && f.getAction() != null) {
                result.put(suggestionId, f.getAction());
            }
        }
        return result;
    }

    private SuggestionSourceArchive buildSourceArchive(Long archiveId) {
        return archiveRepository.findById(archiveId)
                .map(a -> new SuggestionSourceArchive(a.getId(), a.getTitle()))
                .orElse(null);
    }

    private String statusLabel(Integer status) {
        return status != null && status == STATUS_NORMAL ? "正常" : "禁用";
    }

    private String teacherActionLabel(int action) {
        AIFeedbackActionEnum e = AIFeedbackActionEnum.of(action);
        return e != null ? e.getLabel() : "待处理";
    }

    private String toIso(LocalDateTime dt) {
        return dt != null ? dt.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE) : null;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            log.warn("序列化 JSON 失败", e);
            return null;
        }
    }

    // ==================== 内嵌 POJO：请求体 ====================

    @Data
    public static class CreateConversationRequest {
        private String title;
        private Object context;
    }

    @Data
    public static class SendMessageRequest {
        private String content;
    }

    // ==================== 内嵌 POJO：响应体 ====================

    @Data
    public static class ConversationCreateResponse {
        private Long conversationId;
        private String title;
        private Integer status;
        private String createdAt;
    }

    @Data
    public static class ConversationListItem {
        private Long id;
        private String title;
        private Integer status;
        private String statusLabel;
        private String lastMessageTime;
        private String createdAt;
    }

    @Data
    public static class ConversationMessagesResponse {
        private Long conversationId;
        private String title;
        private List<MessageItem> messages;
    }

    @Data
    public static class MessageItem {
        private Long id;
        private String role;
        private String content;
        private String modelName;
        private Integer tokenUsage;
        private Integer generationTimeMs;
        private String feedback;
        private String createdAt;
    }

    @Data
    public static class SendMessageResponse {
        private Long messageId;
        private String role;
        private String content;
        private String modelName;
        private Integer tokenUsage;
        private Integer generationTimeMs;
        private String createdAt;
        private List<SuggestedAction> suggestedActions;
    }

    @Data
    public static class SuggestedAction {
        private String label;
        private String jumpUrl;
        private String actionType;

        public SuggestedAction() {
        }

        public SuggestedAction(String label, String jumpUrl, String actionType) {
            this.label = label;
            this.jumpUrl = jumpUrl;
            this.actionType = actionType;
        }
    }

    @Data
    public static class RegenerateResponse {
        private Long messageId;
        private String role;
        private String content;
        private String modelName;
        private String modelVersion;
        private Integer tokenUsage;
        private Integer generationTimeMs;
        private Integer callStatus;
        @JsonProperty("isRetry")
        private boolean isRetry;
        private String createdAt;
    }

    @Data
    public static class SuggestionsResponse {
        private List<SuggestionItem> list;
    }

    @Data
    public static class SuggestionItem {
        private Long suggestionId;
        private String content;
        private List<SuggestionSourceArchive> sourceArchives;
        private boolean aiGenerated;
        private String aiWarning;
        private Integer teacherAction;
        private String teacherActionLabel;
        private String createdAt;
    }

    @Data
    public static class SuggestionSourceArchive {
        private Long archiveId;
        private String title;

        public SuggestionSourceArchive() {
        }

        public SuggestionSourceArchive(Long archiveId, String title) {
            this.archiveId = archiveId;
            this.title = title;
        }
    }

    @Data
    public static class MessageFeedbackRequest {
        private String feedback;
        private Long conversationId;
    }

    @Data
    public static class MessageFeedbackResponse {
        private Long messageId;
        private String feedback;
    }

    // ==================== 内嵌 POJO：AI 回复占位 ====================

    @Data
    private static class AiReply {
        private String content;
        private String modelName;
        private String modelVersion;
        private Integer tokenUsage;
        private Integer generationTimeMs;
    }
}