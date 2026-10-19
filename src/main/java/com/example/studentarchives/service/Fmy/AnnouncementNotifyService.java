package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.entity.message.Announcement;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.TeacherProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.user.UserRole;
import com.example.studentarchives.enums.AnnouncementReceiverTypeEnum;
import com.example.studentarchives.enums.StatusEnum;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.RoleRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.TeacherProfileRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 公告发布通知服务（Fmy）
 * <p>
 * 收敛「公告发布 → 站内系统通知」的接收人解析与投递逻辑，供
 * {@code MessageProduceAspect}（创建即发布）与 {@code SystemConfigManageService.updateAnnouncement}
 * （草稿改为发布补发）两处复用，避免接收人口径在多处漂移。
 * <p>
 * 接收人口径：学生 + 教职工（教师/管理员）。
 * <ul>
 *   <li>学生：all=全校、class/major/college 按组织链展开到 student_profiles；</li>
 *   <li>管理员：本校 admin 角色用户（全校范围，始终可见）；</li>
 *   <li>教师：college 取该学院教师、major/class 取其所属学院教师、all 取全校教师。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementNotifyService {

    /** 公告已发布状态（对齐 announcements.status） */
    private static final int ANNOUNCEMENT_STATUS_PUBLISHED = 1;

    /** 管理员角色编码 */
    private static final String ROLE_CODE_ADMIN = "admin";

    private final MessageProducer messageProducer;
    private final StudentProfileRepository studentProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    /**
     * 公告已发布时按接收范围粉丝播系统通知；未发布或接收人为空则静默跳过。
     * 消息生产失败仅记日志，不影响业务主流程。
     */
    public void notifyIfPublished(Announcement announcement) {
        try {
            if (announcement == null
                    || announcement.getStatus() == null
                    || announcement.getStatus() != ANNOUNCEMENT_STATUS_PUBLISHED) {
                return;
            }
            List<Long> userIds = resolveRecipients(
                    announcement.getReceiverType(), announcement.getTargetType(),
                    announcement.getTargetId(), announcement.getSchoolId());
            if (userIds.isEmpty()) {
                return;
            }
            MessageProducer.MessageSpec spec = MessageProducer.MessageSpec.systemNotice(
                    announcement.getTitle(), announcement.getContent(),
                    "announcement", announcement.getId(), null, 0);
            messageProducer.sendToUsers(userIds, spec);
        } catch (Exception e) {
            log.warn("公告发布系统通知失败（不影响主流程）: {}", e.getMessage());
        }
    }

    /** 按接收对象（receiver_type）+ 接收范围（target_type）解析接收人，去重 */
    private List<Long> resolveRecipients(String receiverType, String targetType, Long targetId, Long schoolId) {
        if (targetType == null) {
            return Collections.emptyList();
        }
        AnnouncementReceiverTypeEnum type = AnnouncementReceiverTypeEnum.of(receiverType);
        if (type == null) {
            type = AnnouncementReceiverTypeEnum.ALL;
        }
        Set<Long> userIds = new LinkedHashSet<>();
        switch (type) {
            case STUDENT -> userIds.addAll(resolveStudentUserIds(targetType, targetId, schoolId));
            case STAFF -> userIds.addAll(resolveTeacherUserIds(targetType, targetId, schoolId));
            case ADMIN -> userIds.addAll(resolveAdminUserIds(schoolId));
            case ALL -> {
                userIds.addAll(resolveStudentUserIds(targetType, targetId, schoolId));
                userIds.addAll(resolveTeacherUserIds(targetType, targetId, schoolId));
                userIds.addAll(resolveAdminUserIds(schoolId));
            }
        }
        return new ArrayList<>(userIds);
    }

    /**
     * 解析学生用户 ID 列表。
     * all=全校、class=班级、major=专业（→班级→学生）、college=学院（→专业→班级→学生）。
     */
    private List<Long> resolveStudentUserIds(String targetType, Long targetId, Long schoolId) {
        List<StudentProfile> profiles;
        switch (targetType) {
            case "all" -> profiles = studentProfileRepository.findBySchoolId(schoolId);
            case "class" -> {
                if (targetId == null) {
                    return Collections.emptyList();
                }
                profiles = studentProfileRepository.findByClassId(targetId);
            }
            case "major" -> {
                if (targetId == null) {
                    return Collections.emptyList();
                }
                List<Long> classIds = clazzRepository.findByMajorId(targetId).stream()
                        .map(Clazz::getId).toList();
                if (classIds.isEmpty()) {
                    return Collections.emptyList();
                }
                profiles = studentProfileRepository.findByClassIdIn(classIds);
            }
            case "college" -> {
                if (targetId == null) {
                    return Collections.emptyList();
                }
                List<Long> majorIds = majorRepository.findByCollegeIdIn(List.of(targetId)).stream()
                        .map(Major::getId).toList();
                if (majorIds.isEmpty()) {
                    return Collections.emptyList();
                }
                List<Long> classIds = clazzRepository.findByMajorIdIn(majorIds).stream()
                        .map(Clazz::getId).toList();
                if (classIds.isEmpty()) {
                    return Collections.emptyList();
                }
                profiles = studentProfileRepository.findByClassIdIn(classIds);
            }
            default -> {
                return Collections.emptyList();
            }
        }
        return profiles.stream()
                .map(StudentProfile::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    /** 本校 admin 角色用户（全校范围，始终可见） */
    private List<Long> resolveAdminUserIds(Long schoolId) {
        return roleRepository.findByCode(ROLE_CODE_ADMIN)
                .filter(r -> StatusEnum.ENABLED.equalsValue(r.getStatus()))
                .map(r -> userRoleRepository.findByRoleId(r.getId()).stream()
                        .map(UserRole::getUserId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .toList())
                .map(userIds -> filterBySchool(userIds, schoolId))
                .orElseGet(Collections::emptyList);
    }

    /**
     * 教师用户 ID：college 取该学院、major/class 取其所属学院、all 取全校。
     * 教师归属学院取 teacher_profiles.college_id；无法解析学院时退化为全校教师（再按学校过滤）。
     */
    private List<Long> resolveTeacherUserIds(String targetType, Long targetId, Long schoolId) {
        Long collegeId = resolveTargetCollegeId(targetType, targetId);
        List<TeacherProfile> profiles = collegeId != null
                ? teacherProfileRepository.findByCollegeId(collegeId)
                : teacherProfileRepository.findAll();
        List<Long> userIds = profiles.stream()
                .map(TeacherProfile::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return filterBySchool(userIds, schoolId);
    }

    /** 将 targetType/targetId 解析为其所属学院 ID（teacher_profiles 归属粒度） */
    private Long resolveTargetCollegeId(String targetType, Long targetId) {
        if (targetId == null) {
            return null;
        }
        return switch (targetType) {
            case "college" -> targetId;
            case "major" -> majorRepository.findById(targetId).map(Major::getCollegeId).orElse(null);
            case "class" -> {
                Long majorId = clazzRepository.findById(targetId).map(Clazz::getMajorId).orElse(null);
                yield majorId != null
                        ? majorRepository.findById(majorId).map(Major::getCollegeId).orElse(null)
                        : null;
            }
            default -> null;
        };
    }

    /** 按学校过滤用户 ID（教职工须属同校） */
    private List<Long> filterBySchool(List<Long> userIds, Long schoolId) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return userRepository.findByIdIn(userIds).stream()
                .filter(u -> Objects.equals(u.getSchoolId(), schoolId))
                .map(User::getId)
                .toList();
    }
}
