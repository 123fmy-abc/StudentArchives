package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.career.request.TeacherCareerFeedbackRequest;
import com.example.studentarchives.dto.Fmy.career.response.TeacherCareerFeedbackResponse;
import com.example.studentarchives.entity.career.CareerPlan;
import com.example.studentarchives.entity.career.CareerPlanFeedback;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.CareerPlanFeedbackRepository;
import com.example.studentarchives.repository.CareerPlanRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 教师端职业规划服务（《教师端接口文档》八、职业规划反馈模块）
 * <p>
 * 提供提交职业规划反馈接口：教师对学生的职业规划进行点评反馈，
 * 反馈写入 career_plan_feedbacks 表，随后发送消息通知学生。
 * 数据范围校验复用 {@link TeacherScopeValidator}，越权返回 20005。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherCareerPlanService {

    /** ISO 8601 带时区格式：2026-07-01T10:00:00+08:00 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AdminAuthService adminAuthService;
    private final TeacherScopeValidator scopeValidator;
    private final CareerPlanRepository careerPlanRepository;
    private final CareerPlanFeedbackRepository careerPlanFeedbackRepository;
    private final MessageProducer messageProducer;
    private final ObjectMapper objectMapper;

    /**
     * 提交职业规划反馈（POST /teacher/career-plans/{planId}/feedbacks，教师端文档 8.1）
     * <p>
     * 校验：规划存在（30001）；当前教师对该规划所属学生有授权范围（20005）。
     * 反馈写入后通过 {@link MessageProducer#privateMessage} 发送私信通知学生。
     *
     * @param teacherId 当前登录教师用户 ID
     * @param planId    职业规划 ID
     * @param request   反馈请求
     * @return 反馈 ID 与提交时间
     */
    @Transactional
    public TeacherCareerFeedbackResponse submitFeedback(Long teacherId, Long planId,
                                                        TeacherCareerFeedbackRequest request) {
        CareerPlan plan = careerPlanRepository.findById(planId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "职业规划不存在"));
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);
        scopeValidator.ensureStudentInScope(teacherId, plan.getUserId(), schoolId);

        CareerPlanFeedback feedback = new CareerPlanFeedback();
        feedback.setCareerPlanId(planId);
        feedback.setTeacherId(teacherId);
        feedback.setFeedbackContent(request.getFeedbackContent());
        feedback.setSuggestionItems(serializeSuggestionItems(request.getSuggestionItems()));
        feedback = careerPlanFeedbackRepository.save(feedback);

        // 反馈写入后发送消息通知学生
        try {
            messageProducer.privateMessage(
                    plan.getUserId(),
                    teacherId,
                    "职业规划反馈",
                    "你的职业规划《" + plan.getTitle() + "》收到新的教师反馈，请及时查看。",
                    "career_plan",
                    planId,
                    "/career-plans/" + planId);
        } catch (Exception e) {
            log.warn("职业规划反馈消息发送失败 planId={} teacherId={}", planId, teacherId, e);
        }

        return TeacherCareerFeedbackResponse.builder()
                .feedbackId(feedback.getId())
                .createdAt(toIso(feedback.getCreatedAt()))
                .build();
    }

    /** 建议项列表 → JSON 数组字符串（空列表/空值 → null） */
    private String serializeSuggestionItems(List<String> suggestionItems) {
        if (suggestionItems == null || suggestionItems.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(suggestionItems);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "建议项格式错误");
        }
    }

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
