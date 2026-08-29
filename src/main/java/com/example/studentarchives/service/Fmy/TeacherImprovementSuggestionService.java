package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.dto.Fmy.weakness.request.TeacherImprovementSuggestionRequest;
import com.example.studentarchives.dto.Fmy.weakness.response.TeacherImprovementSuggestionResponse;
import com.example.studentarchives.entity.weakness.ImprovementSuggestion;
import com.example.studentarchives.enums.AISuggestionSourceEnum;
import com.example.studentarchives.repository.ImprovementSuggestionRepository;
import com.example.studentarchives.service.common.AdminAuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 教师端改进建议服务（《教师端接口文档》十、短板识别与改进建议模块）
 * <p>
 * 提供新增改进建议接口：教师对学生提出个性化改进建议，
 * 写入 improvement_suggestions 表（source=2 教师建议、teacher_id=当前教师、is_implemented=0）。
 * 数据范围校验复用 {@link TeacherScopeValidator}，越权返回 20005。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TeacherImprovementSuggestionService {

    /** ISO 8601 带时区格式：2026-07-01T10:00:00+08:00 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final AdminAuthService adminAuthService;
    private final TeacherScopeValidator scopeValidator;
    private final ImprovementSuggestionRepository improvementSuggestionRepository;

    /**
     * 新增改进建议（POST /teacher/students/{studentId}/improvement-suggestions，教师端文档 10.1）
     * <p>
     * 校验目标学生在当前教师授权范围内（20005），随后写入改进建议。
     *
     * @param teacherId 当前登录教师用户 ID
     * @param studentId 目标学生用户 ID
     * @param request   建议内容
     * @return 建议 ID 与创建时间
     */
    @Transactional
    public TeacherImprovementSuggestionResponse addSuggestion(Long teacherId, Long studentId,
                                                              TeacherImprovementSuggestionRequest request) {
        Long schoolId = adminAuthService.getOperatorSchoolId(teacherId);
        scopeValidator.ensureStudentInScope(teacherId, studentId, schoolId);

        ImprovementSuggestion suggestion = new ImprovementSuggestion();
        suggestion.setWeaknessId(request.getWeaknessId());
        suggestion.setSuggestionType(request.getSuggestionType());
        suggestion.setSuggestionContent(request.getContent());
        suggestion.setRelatedGoalId(request.getRelatedGoalId());
        suggestion.setSource(AISuggestionSourceEnum.TEACHER.getValue());
        suggestion.setTeacherId(teacherId);
        suggestion.setIsImplemented(0);
        suggestion = improvementSuggestionRepository.save(suggestion);

        return TeacherImprovementSuggestionResponse.builder()
                .suggestionId(suggestion.getId())
                .createdAt(toIso(suggestion.getCreatedAt()))
                .build();
    }

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }
}
