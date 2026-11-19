package com.example.studentarchives.support;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.org.Semester;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.SemesterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * 事件时间（获奖/获得时间等）与所选学期的一致性校验。
 * <p>
 * 校验规则：
 * <ul>
 *   <li>事件时间不能晚于当前日期；</li>
 *   <li>正式提交时事件时间必须落在所选学期的起止日期内（以 {@code semesters.start_date / end_date} 为准）；
 *       保存草稿只校验不能晚于当前日期；</li>
 *   <li><b>假期归属规则：</b>事件时间落在所选学期与相邻学期之间的寒暑假空档时，视为假期获得，
 *       按相邻学期放行——即允许落在「前一学期结束之后、所选学期开始之前」或
 *       「所选学期结束之后、下一学期开始之前」的空档内；</li>
 *   <li>任一侧日期未配置、事件时间为空或未选学期时放行（由必填校验负责）。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class SemesterEventTimeValidator {

    private final SemesterRepository semesterRepository;

    /**
     * 校验事件时间与所选学期匹配，不匹配抛出业务异常。
     *
     * @param semesterId 所选学期 ID（草稿或为空时跳过学期区间校验）
     * @param eventTime  事件时间（获奖/获得时间等）
     * @param label      错误提示中的字段名，如「获奖时间」「参赛时间」
     * @param draft      是否草稿保存
     */
    public void requireEventTimeInSemester(Long semesterId, LocalDate eventTime, String label, boolean draft) {
        if (eventTime == null) return;
        if (eventTime.isAfter(LocalDate.now())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, label + "不能大于当前时间");
        }
        if (draft || semesterId == null) return;
        Semester semester = semesterRepository.findById(semesterId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学期不存在"));
        if (isWithinSemester(semester, eventTime)) return;
        if (isInAdjacentVacationGap(semester, eventTime)) return;

        // 不在学期内也不在相邻假期空档：按原始口径报错（先报"早于"再报"晚于"）
        if (semester.getStartDate() != null && eventTime.isBefore(semester.getStartDate())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, label + "不能早于所选学期的开始日期（"
                    + semester.getStartDate() + "）");
        }
        if (semester.getEndDate() != null && eventTime.isAfter(semester.getEndDate())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, label + "不能晚于所选学期的结束日期（"
                    + semester.getEndDate() + "）");
        }
    }

    /** 事件时间是否落在学期起止日期内（任一侧未配置视为该侧不设限） */
    private boolean isWithinSemester(Semester semester, LocalDate eventTime) {
        boolean notBeforeStart = semester.getStartDate() == null || !eventTime.isBefore(semester.getStartDate());
        boolean notAfterEnd = semester.getEndDate() == null || !eventTime.isAfter(semester.getEndDate());
        return notBeforeStart && notAfterEnd;
    }

    /**
     * 事件时间是否落在所选学期紧邻的寒暑假空档内。
     * 空档定义为同校相邻两个学期之间未覆盖的日期段（开学前一学期的结束日期之后、本学期开始日期之前，
     * 或本学期结束日期之后、下一学期开始日期之前）。
     */
    private boolean isInAdjacentVacationGap(Semester semester, LocalDate eventTime) {
        if (semester.getSchoolId() == null) return false;
        List<Semester> siblings = semesterRepository.findBySchoolId(semester.getSchoolId()).stream()
                .filter(s -> s.getStatus() != null && s.getStatus() == 1)
                .filter(s -> s.getStartDate() != null)
                .toList();

        // 假期空档「开学前」：需存在前一学期（start_date 更早的最近一学期），事件时间在其结束之后、本学期开始之前
        Semester prev = siblings.stream()
                .filter(s -> s.getStartDate().isBefore(semester.getStartDate()))
                .filter(s -> s.getEndDate() != null)
                .max(Comparator.comparing(Semester::getStartDate))
                .orElse(null);
        if (prev != null
                && eventTime.isAfter(prev.getEndDate())
                && semester.getStartDate() != null
                && eventTime.isBefore(semester.getStartDate())) {
            return true;
        }

        // 假期空档「放假后」：需存在下一学期（start_date 更晚的最近一学期），事件时间在本学期结束之后、其开始之前
        if (semester.getEndDate() == null) return false;
        Semester next = siblings.stream()
                .filter(s -> s.getStartDate().isAfter(semester.getStartDate()))
                .filter(s -> s.getEndDate() != null)
                .min(Comparator.comparing(Semester::getStartDate))
                .orElse(null);
        return next != null
                && eventTime.isAfter(semester.getEndDate())
                && eventTime.isBefore(next.getStartDate());
    }
}
