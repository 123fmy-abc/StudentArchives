package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.config.Lzw.LlmProperties;
import com.example.studentarchives.entity.ai.AiConversation;
import com.example.studentarchives.entity.ai.AiGenerationLog;
import com.example.studentarchives.entity.ai.AiMessage;
import com.example.studentarchives.entity.ai.AiMessageFeedback;
import com.example.studentarchives.entity.ai.AiTeacherFeedback;
import com.example.studentarchives.entity.archive.Archive;
import com.example.studentarchives.entity.award.AwardApplication;
import com.example.studentarchives.entity.career.CareerPlan;
import com.example.studentarchives.entity.foundation.AwardTypeConfig;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.weakness.ImprovementSuggestion;
import com.example.studentarchives.entity.weakness.WeaknessAnalysis;
import com.example.studentarchives.enums.AIFeedbackActionEnum;
import com.example.studentarchives.enums.AISuggestionSourceEnum;
import com.example.studentarchives.enums.APICallStatusEnum;
import com.example.studentarchives.enums.ApplyStatusEnum;
import com.example.studentarchives.enums.ArchiveTypeEnum;
import com.example.studentarchives.enums.AwardTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AiConversationRepository;
import com.example.studentarchives.repository.AiGenerationLogRepository;
import com.example.studentarchives.repository.AiMessageFeedbackRepository;
import com.example.studentarchives.repository.AiMessageRepository;
import com.example.studentarchives.repository.AiTeacherFeedbackRepository;
import com.example.studentarchives.repository.ArchiveRepository;
import com.example.studentarchives.repository.AwardApplicationRepository;
import com.example.studentarchives.repository.AwardTypeConfigRepository;
import com.example.studentarchives.repository.CareerPlanRepository;
import com.example.studentarchives.repository.ImprovementSuggestionRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.repository.WeaknessAnalysisRepository;
import com.example.studentarchives.service.common.GuideAssembler;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * AI 对话模块服务
 * <p>
 * 覆盖 8 个端点：创建会话、会话列表、消息列表、发送消息（含 AI 生成）、发送消息（流式 SSE）、
 * 重新生成、AI 建议、删除会话。
 * AI 回复由大模型（智谱 GLM）生成（流式见 {@link #sendMessageStream}，非流式见 buildAiReply），
 * 调用失败时返回兜底话术。
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
            + "系统会以「项目数据上下文」的形式提供该学生的真实档案、奖项申报记录、"
            + "职业规划数据以及奖项评选说明配置，回答相关问题时必须优先依据上下文中的"
            + "真实数据与配置，不要编造具体数据；上下文中没有的信息要如实说明。"
            + "若问题超出档案系统范围，请礼貌说明并建议学生联系老师。";
    /** 注入上下文时各类记录的最大条数，避免提示词过长 */
    private static final int CONTEXT_ITEM_LIMIT = 10;
    /** 评选说明正文的截断长度 */
    private static final int GUIDE_CONTENT_LIMIT = 300;
    /** 泛指奖项类问题时触发评选说明注入的关键词 */
    private static final String[] AWARD_GENERIC_KEYWORDS = {"奖项", "评选", "申报", "评优", "报名"};
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
    private final AwardApplicationRepository awardApplicationRepository;
    private final AwardTypeConfigRepository awardTypeConfigRepository;
    private final GuideAssembler guideAssembler;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final LlmClient llmClient;
    private final LlmProperties llmProperties;
    /** AI 流式回复线程池（Bean 名 aiStreamExecutor，按字段名匹配注入） */
    private final Executor aiStreamExecutor;

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

        // 2. 调用 AI 生成回复（带上项目数据上下文 + 当前会话历史作为上下文）
        List<AiMessage> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
        AiReply reply = buildAiReply(userId, content, history);

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

    // ==================== 9.4.1 发送消息（流式 SSE） ====================

    /**
     * 流式发送消息：立即返回 SSE 连接，AI 生成内容逐段推送，前端可边收边显示。
     * <p>
     * 事件格式（data 均为 JSON）：
     * <ul>
     *   <li>{@code {"type":"start","conversationId":1}}</li>
     *   <li>{@code {"type":"delta","content":"片段"}}</li>
     *   <li>{@code {"type":"done","messageId":2,"modelName":"...","tokenUsage":0,
     *       "generationTimeMs":0,"createdAt":"...","suggestedActions":[...]}}</li>
     *   <li>{@code {"type":"error","message":"..."}}（随后连接关闭）</li>
     * </ul>
     * 校验在请求线程内完成，异常交由全局异常处理；流式阶段异常只推 error 事件并在服务端记日志。
     */
    public SseEmitter sendMessageStream(Long conversationId, SendMessageRequest body, Long userId) {
        AiConversation conv = loadOwnedConversation(conversationId, userId);
        if (conv.getStatus() != null && conv.getStatus() != STATUS_NORMAL) {
            throw new BusinessException(ResultCode.BIZ_STATUS_NOT_OPERABLE, "会话已禁用");
        }
        String content = body == null ? null : body.getContent();
        if (content == null || content.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "消息内容不能为空");
        }

        // 超时略大于大模型调用超时，避免 SSE 先于模型响应断开
        SseEmitter emitter = new SseEmitter((long) llmProperties.getTimeoutMs() + 10_000L);
        aiStreamExecutor.execute(() -> doSendMessageStream(conversationId, content, userId, emitter));
        return emitter;
    }

    /**
     * 流式回复的实际执行体，运行在 aiStreamExecutor 线程池。
     * <p>
     * 刻意不加 @Transactional：流式生成可能持续数十秒，长事务会长时间占用数据库连接。
     */
    private void doSendMessageStream(Long conversationId, String content, Long userId, SseEmitter emitter) {
        try {
            // 1. 写入用户消息
            AiMessage userMsg = new AiMessage();
            userMsg.setConversationId(conversationId);
            userMsg.setRole(ROLE_USER);
            userMsg.setContent(content);
            messageRepository.save(userMsg);

            sendSseEvent(emitter, Map.of("type", "start", "conversationId", conversationId));

            // 2. 组装完整提示词并流式调用大模型
            List<AiMessage> history = messageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
            List<LlmClient.ChatMessage> messages = buildChatMessages(userId, content, history);

            LlmClient.ChatResult result;
            try {
                result = llmClient.streamChat(messages, delta -> {
                    sendSseEvent(emitter, Map.of("type", "delta", "content", delta));
                });
            } catch (Exception e) {
                log.error("大模型流式调用中断，会话 {}", conversationId, e);
                sendSseEventQuietly(emitter, Map.of("type", "error", "message", "AI 服务暂时不可用，请稍后重试。"));
                emitter.complete();
                return;
            }

            // 3. 落库助手消息
            AiMessage assistantMsg = new AiMessage();
            assistantMsg.setConversationId(conversationId);
            assistantMsg.setRole(ROLE_ASSISTANT);
            assistantMsg.setContent(result.content());
            assistantMsg.setModelName(result.model());
            assistantMsg.setTokenUsage(result.totalTokens());
            assistantMsg.setGenerationTimeMs((int) result.generationTimeMs());
            messageRepository.save(assistantMsg);

            // 4. 更新会话最后对话时间
            conversationRepository.findById(conversationId).ifPresent(c -> {
                c.setUpdatedAt(LocalDateTime.now());
                conversationRepository.save(c);
            });

            // 5. 写入生成日志
            AiReply reply = new AiReply();
            reply.setContent(result.content());
            reply.setModelName(result.model());
            reply.setTokenUsage(result.totalTokens());
            reply.setGenerationTimeMs((int) result.generationTimeMs());
            writeGenerationLog(userId, assistantMsg.getId(), content, result.content(), reply,
                    APICallStatusEnum.SUCCESS, null);

            // 6. 推送结束事件
            Map<String, Object> done = new LinkedHashMap<>();
            done.put("type", "done");
            done.put("messageId", assistantMsg.getId());
            done.put("role", ROLE_ASSISTANT);
            done.put("modelName", result.model());
            done.put("tokenUsage", result.totalTokens());
            done.put("generationTimeMs", (int) result.generationTimeMs());
            done.put("createdAt", toIso(assistantMsg.getCreatedAt()));
            done.put("suggestedActions", buildSuggestedActions(content));
            sendSseEvent(emitter, done);
            emitter.complete();
        } catch (Exception e) {
            // 客户端断开、SSE 推送失败或落库异常
            log.error("AI 流式回复执行失败，会话 {}", conversationId, e);
            sendSseEventQuietly(emitter, Map.of("type", "error", "message", "回复生成失败，请稍后重试。"));
            emitter.complete();
        }
    }

    /** 推送一个 SSE 事件；失败时抛异常，由调用方决定是否中止。 */
    private void sendSseEvent(SseEmitter emitter, Object data) {
        try {
            emitter.send(SseEmitter.event().data(data, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            throw new IllegalStateException("SSE 推送失败（客户端可能已断开）", e);
        }
    }

    /** 尽力推送事件，忽略失败（用于错误提示等收尾场景）。 */
    private void sendSseEventQuietly(SseEmitter emitter, Object data) {
        try {
            emitter.send(SseEmitter.event().data(data, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            log.debug("SSE 收尾事件推送失败，客户端可能已断开", e);
        }
    }

    // ==================== 9.5 重新生成 AI 消息 ====================

    @Transactional
    public RegenerateResponse regenerate(Long conversationId, Long messageId, Long userId) {
        AiConversation conv = loadOwnedConversation(conversationId, userId);

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
        AiReply reply = buildAiReply(userId, prompt, history);
        LocalDateTime now = LocalDateTime.now();

        AiMessage newMsg = new AiMessage();
        newMsg.setConversationId(conversationId);
        newMsg.setRole(ROLE_ASSISTANT);
        newMsg.setContent(reply.getContent());
        newMsg.setModelName(reply.getModelName());
        newMsg.setTokenUsage(reply.getTokenUsage());
        newMsg.setGenerationTimeMs(reply.getGenerationTimeMs());
        messageRepository.save(newMsg);

        conv.setUpdatedAt(now);
        conversationRepository.save(conv);

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
            // 未被当前学生采纳过才可再次采纳（列表已按本人 weakness 圈定，归属无需再校验）
            item.setCanAdopt(!careerPlanRepository.existsByUserIdAndSourceAndAiSuggestionId(userId, 2, s.getId()));
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
     * 组装发给大模型的消息序列：系统提示词 + 项目数据上下文 + 最近历史消息。
     */
    private List<LlmClient.ChatMessage> buildChatMessages(Long userId, String question, List<AiMessage> history) {
        List<LlmClient.ChatMessage> messages = new ArrayList<>();
        messages.add(new LlmClient.ChatMessage(ROLE_SYSTEM, SYSTEM_PROMPT));

        // 注入项目数据上下文：学生个人数据摘要 + 命中的奖项评选说明配置
        String projectContext = buildProjectContext(userId, question);
        if (projectContext != null) {
            messages.add(new LlmClient.ChatMessage(ROLE_SYSTEM, projectContext));
        }

        int from = Math.max(0, history.size() - HISTORY_LIMIT);
        for (int i = from; i < history.size(); i++) {
            AiMessage m = history.get(i);
            if (m.getRole() == null || m.getContent() == null || m.getContent().isBlank()) {
                continue;
            }
            messages.add(new LlmClient.ChatMessage(m.getRole(), m.getContent()));
        }
        return messages;
    }

    /**
     * 非流式调用大模型：复用 {@link #buildChatMessages} 组装提示词；失败时返回兜底话术。
     */
    private AiReply buildAiReply(Long userId, String question, List<AiMessage> history) {
        List<LlmClient.ChatMessage> messages = buildChatMessages(userId, question, history);
        try {
            LlmClient.ChatResult result = llmClient.chat(messages);
            AiReply reply = new AiReply();
            reply.setContent(result.content());
            reply.setModelName(result.model());
            reply.setModelVersion(null);
            reply.setTokenUsage(result.totalTokens());
            reply.setGenerationTimeMs((int) result.generationTimeMs());
            return reply;
        } catch (Exception e) {
            log.error("大模型调用失败，返回兜底话术", e);
            AiReply reply = new AiReply();
            reply.setContent("抱歉，AI 服务暂时不可用，请稍后重试。");
            reply.setModelName(llmProperties.getModel());
            reply.setModelVersion(null);
            reply.setTokenUsage(0);
            reply.setGenerationTimeMs(0);
            return reply;
        }
    }

    // ==================== 项目数据上下文构建 ====================

    /**
     * 构建注入给大模型的项目数据上下文。
     * <p>
     * 由两部分组成：① 学生个人数据摘要（档案、奖项申报、职业规划）；
     * ② 按问题关键词命中的奖项评选说明配置。两部分都没有时返回 null，不占用提示词。
     */
    private String buildProjectContext(Long userId, String question) {
        User user = userRepository.findById(userId).orElse(null);
        String studentSummary = buildStudentSummary(user, userId);
        String guideSummary = buildGuideSummary(user, question);
        if (studentSummary.isEmpty() && guideSummary.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【项目数据上下文】以下是系统内与当前学生相关的真实数据与配置，")
          .append("回答时必须优先依据这些信息；若其中没有答案，请明确说明，不要编造具体数据。\n");
        if (!studentSummary.isEmpty()) {
            sb.append(studentSummary).append('\n');
        }
        if (!guideSummary.isEmpty()) {
            sb.append(guideSummary);
        }
        return sb.toString();
    }

    /** 学生个人数据摘要：基本信息 + 档案 + 奖项申报 + 职业规划（各取最近若干条）。 */
    private String buildStudentSummary(User user, Long userId) {
        StringBuilder sb = new StringBuilder();
        sb.append("【学生信息】姓名：").append(user != null && user.getName() != null ? user.getName() : "未知").append('\n');

        List<AwardApplication> awards = awardApplicationRepository.findByUserId(userId);
        if (!awards.isEmpty()) {
            sb.append("【奖项申报记录】\n");
            awards.stream().limit(CONTEXT_ITEM_LIMIT).forEach(a -> sb.append("- ")
                    .append(a.getTitle())
                    .append("（").append(awardTypeLabel(a.getAwardType()))
                    .append("，").append(applyStatusLabel(a.getStatus())).append("）\n"));
        }

        List<Archive> archives = archiveRepository.findByUserId(userId);
        if (!archives.isEmpty()) {
            sb.append("【档案记录】\n");
            archives.stream().limit(CONTEXT_ITEM_LIMIT).forEach(a -> sb.append("- ")
                    .append(a.getTitle())
                    .append("（").append(archiveTypeLabel(a.getArchiveType()))
                    .append("，").append(applyStatusLabel(a.getStatus())).append("）\n"));
        }

        List<CareerPlan> plans = careerPlanRepository.findByUserId(userId);
        if (!plans.isEmpty()) {
            sb.append("【职业规划】\n");
            plans.stream().limit(CONTEXT_ITEM_LIMIT).forEach(p -> {
                sb.append("- ").append(p.getTitle())
                  .append("（进度").append(p.getProgressRate() != null ? p.getProgressRate() : 0).append("%");
                if (p.getContent() != null && !p.getContent().isBlank()) {
                    sb.append("，内容摘要：").append(truncate(p.getContent(), 120));
                }
                sb.append("）\n");
            });
        }
        return sb.toString();
    }

    /** 按问题关键词注入奖项评选说明：命中具体奖项名注入该奖项；泛指奖项问题时注入全部奖项说明。 */
    private String buildGuideSummary(User user, String question) {
        if (question == null || question.isBlank()) {
            return "";
        }
        Long schoolId = user != null && user.getSchoolId() != null ? user.getSchoolId() : 1L;

        StringBuilder sb = new StringBuilder();
        boolean specificHit = false;
        for (AwardTypeEnum type : AwardTypeEnum.values()) {
            if (question.contains(type.getLabel())) {
                appendGuide(sb, schoolId, type);
                specificHit = true;
            }
        }
        if (!specificHit && containsAny(question, AWARD_GENERIC_KEYWORDS)) {
            for (AwardTypeEnum type : AwardTypeEnum.values()) {
                appendGuide(sb, schoolId, type);
            }
        }
        return sb.toString();
    }

    /** 追加单个奖项类型的评选说明配置（取该校已启用配置，未配置时跳过）。 */
    private void appendGuide(StringBuilder sb, Long schoolId, AwardTypeEnum type) {
        AwardTypeConfig config = awardTypeConfigRepository
                .findBySchoolIdAndAwardTypeAndStatus(schoolId, type.getValue(), 1)
                .orElse(null);
        if (config == null) {
            return;
        }
        GuideAssembler.GuideResponse guide = guideAssembler.assemble(
                type.getValue(), type.getLabel(),
                config.getEvaluateDesc(), config.getEvaluateRequirements(),
                config.getEvaluateNotes(), config.getApplyDesc(),
                config.getIcon(), config.getUpdatedAt());

        sb.append("【").append(guide.getTitle()).append("】（来源：系统配置）\n");
        if (guide.getContent() != null && !guide.getContent().isBlank()) {
            sb.append("评选说明：").append(truncate(guide.getContent(), GUIDE_CONTENT_LIMIT)).append('\n');
        }
        if (guide.getRequirements() != null && !guide.getRequirements().isEmpty()) {
            sb.append("申报材料要求：\n");
            for (GuideAssembler.Requirement r : guide.getRequirements()) {
                sb.append("- ").append(r.getLabel() != null ? r.getLabel() : r.getField());
                if (r.isRequired()) {
                    sb.append("（必填）");
                }
                if (r.getDescription() != null && !r.getDescription().isBlank()) {
                    sb.append("：").append(r.getDescription());
                }
                sb.append('\n');
            }
        }
        if (guide.getNotes() != null && !guide.getNotes().isEmpty()) {
            sb.append("注意事项：\n");
            for (String note : guide.getNotes()) {
                sb.append("- ").append(note).append('\n');
            }
        }
        if (guide.getApplyDesc() != null && !guide.getApplyDesc().isBlank()) {
            sb.append("申报说明：").append(truncate(guide.getApplyDesc(), GUIDE_CONTENT_LIMIT)).append('\n');
        }
        sb.append('\n');
    }

    private String awardTypeLabel(String awardType) {
        AwardTypeEnum e = AwardTypeEnum.of(awardType);
        return e != null ? e.getLabel() : (awardType != null ? awardType : "未知类型");
    }

    private String archiveTypeLabel(String archiveType) {
        ArchiveTypeEnum e = ArchiveTypeEnum.of(archiveType);
        return e != null ? e.getLabel() : (archiveType != null ? archiveType : "未知类型");
    }

    private String applyStatusLabel(Integer status) {
        ApplyStatusEnum e = ApplyStatusEnum.of(status);
        return e != null ? e.getLabel() : "未知状态";
    }

    private String truncate(String text, int limit) {
        String trimmed = text.replaceAll("\\s+", " ").trim();
        return trimmed.length() <= limit ? trimmed : trimmed.substring(0, limit) + "…";
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
        /** 是否可被当前学生采纳为计划（未被采纳过且归属本人），供前端预判按钮可用性 */
        private boolean canAdopt;
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