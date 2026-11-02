package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.common.ValidationConstants;
import com.example.studentarchives.entity.log.LoginLog;
import com.example.studentarchives.entity.foundation.Dictionary;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.College;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.org.School;
import com.example.studentarchives.entity.user.Role;
import com.example.studentarchives.entity.user.RoleScope;
import com.example.studentarchives.entity.user.StudentProfile;
import com.example.studentarchives.entity.user.TeacherProfile;
import com.example.studentarchives.entity.user.User;
import com.example.studentarchives.entity.user.UserContactInfo;
import com.example.studentarchives.entity.user.UserRole;
import com.example.studentarchives.enums.GenderEnum;
import com.example.studentarchives.enums.ScopeTypeEnum;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.CollegeRepository;
import com.example.studentarchives.repository.DictionaryRepository;
import com.example.studentarchives.repository.LoginLogRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.RoleRepository;
import com.example.studentarchives.repository.RoleScopeRepository;
import com.example.studentarchives.repository.SchoolRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
import com.example.studentarchives.repository.TeacherProfileRepository;
import com.example.studentarchives.repository.UserContactInfoRepository;
import com.example.studentarchives.repository.UserRepository;
import com.example.studentarchives.repository.UserRoleRepository;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 管理端用户管理服务（Lzw）
 * <p>
 * 对应《管理端接口文档》六、用户管理模块（6.1 ~ 6.8）。
 * 数据来源：users、user_roles、roles、role_scopes、student_profiles、
 * teacher_profiles、user_contact_infos、classes、majors、colleges、schools、login_logs。
 * <p>
 * 权限：读接口要求 admin 角色或 user:view / user:manage 权限码，
 * 写接口要求 admin 角色或 user:manage 权限码，越权统一返回 20005。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserManageService {

    /** 一次性初始口令字符集（排除易混淆的 0/O/1/I/l 等） */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final String PWD_UPPER = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String PWD_LOWER = "abcdefghijkmnpqrstuvwxyz";
    private static final String PWD_DIGIT = "23456789";
    private static final String PWD_SPECIAL = "!@#$%^&*?";
    private static final int INITIAL_PASSWORD_LENGTH = 12;

    /** 学生角色编码（自动创建 student_profiles 记录） */
    private static final String ROLE_CODE_STUDENT = "student";

    /** 教师角色编码（自动创建 teacher_profiles 记录） */
    private static final String ROLE_CODE_TEACHER = "teacher";

    /** 辅导员角色编码（同一用户可同时持有 teacher + counselor，范围需分别绑定） */
    private static final String ROLE_CODE_COUNSELOR = "counselor";

    /** ISO 8601 带时区输出格式 */
    private static final DateTimeFormatter ISO_WITH_ZONE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoleRepository roleRepository;
    private final RoleScopeRepository roleScopeRepository;
    private final UserContactInfoRepository userContactInfoRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final TeacherProfileRepository teacherProfileRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final CollegeRepository collegeRepository;
    private final DictionaryRepository dictionaryRepository;
    private final SchoolRepository schoolRepository;
    private final LoginLogRepository loginLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminAuthService adminAuthService;

    // ==================== 6.1 获取用户列表 ====================

    @Transactional(readOnly = true)
    public PageResult<UserListItem> listUsers(Long operatorId, UserListQuery query, PageParam pageParam) {
        adminAuthService.requireAdminOrPermission(operatorId, "user:view", "user:manage");

        Set<Long> candidateIds = resolveCandidateUserIds(query);
        if (candidateIds != null && candidateIds.isEmpty()) {
            return PageResult.of(Collections.emptyList(), 0, pageParam);
        }

        Specification<User> spec = buildSpecification(query, candidateIds);
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(), sort);
        Page<User> page = userRepository.findAll(spec, pageable);

        return PageResult.of(toUserListItems(page.getContent()), page.getTotalElements(), pageParam);
    }

    // ==================== 6.2 获取用户详情 ====================

    @Transactional(readOnly = true)
    public UserDetail getDetail(Long operatorId, Long userId) {
        adminAuthService.requireAdminOrPermission(operatorId, "user:view", "user:manage");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在"));
        UserContactInfo contact = userContactInfoRepository.findByUserId(userId).orElse(null);
        School school = user.getSchoolId() != null ? schoolRepository.findById(user.getSchoolId()).orElse(null) : null;

        List<UserRole> userRoles = userRoleRepository.findByUserId(userId);
        List<Role> roles = userRoles.isEmpty()
                ? Collections.emptyList()
                : roleRepository.findByIdIn(userRoles.stream().map(UserRole::getRoleId)
                        .filter(Objects::nonNull).distinct().collect(Collectors.toList()));

        List<RoleScope> scopes = roleScopeRepository.findByUserIdAndStatus(userId, 1);

        String lastLoginAt = loginLogRepository.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .map(LoginLog::getCreatedAt).map(this::toIso).orElse(null);

        return UserDetail.builder()
                .userId(user.getId())
                .userNo(user.getUserNo())
                .name(user.getName())
                .email(contact != null ? contact.getEmail() : null)
                .phone(contact != null ? contact.getPhone() : null)
                .gender(user.getGender())
                .genderLabel(GenderEnum.of(user.getGender()).getLabel())
                .birthDate(user.getBirthDate() != null ? user.getBirthDate().toString() : null)
                .schoolId(user.getSchoolId())
                .schoolName(school != null ? school.getName() : null)
                .roles(roles.stream()
                        .map(r -> RoleItem.builder().roleId(r.getId()).roleName(r.getName()).level(r.getLevel()).build())
                        .collect(Collectors.toList()))
                .status(user.getStatus())
                .statusLabel(accountStatusLabel(user.getStatus()))
                .scopes(toScopeItems(scopes))
                .createdAt(toIso(user.getCreatedAt()))
                .lastLoginAt(lastLoginAt)
                .build();
    }

    // ==================== 6.3 创建用户 ====================

    @Transactional
    public CreateUserResponse createUser(Long operatorId, CreateUserRequest body) {
        adminAuthService.requireAdminOrPermission(operatorId, "user:manage");

        if (body.getUserNo() == null || body.getUserNo().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "学号/工号不能为空");
        }
        if (body.getName() == null || body.getName().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "姓名不能为空");
        }
        if (body.getSchoolId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "学校ID不能为空");
        }
        if (body.getRoleIds() == null || body.getRoleIds().isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "角色列表不能为空");
        }

        String userNo = body.getUserNo().trim();
        userRepository.findByUserNo(userNo)
                .ifPresent(u -> { throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "学号/工号已存在"); });

        schoolRepository.findById(body.getSchoolId())
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学校不存在"));

        List<Long> roleIds = body.getRoleIds().stream().distinct().collect(Collectors.toList());
        List<Role> roles = roleRepository.findByIdIn(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "存在非法的角色ID");
        }
        Set<String> roleCodes = roles.stream().map(Role::getCode).collect(Collectors.toSet());
        boolean isStudent = roleCodes.contains(ROLE_CODE_STUDENT);
        boolean isTeacher = roleCodes.contains(ROLE_CODE_TEACHER);
        if (isStudent && body.getClassId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "学生角色必须指定班级ID");
        }

        User user = new User();
        user.setSchoolId(body.getSchoolId());
        user.setUserNo(userNo);
        user.setName(body.getName().trim());
        user.setGender(body.getGender());
        boolean passwordProvided = body.getPassword() != null && !body.getPassword().isBlank();
        String rawPassword;
        if (passwordProvided) {
            // 管理员显式设置的密码同样要求满足强度策略，避免弱口令
            if (!body.getPassword().matches(ValidationConstants.PASSWORD_PATTERN)) {
                throw new BusinessException(ResultCode.PARAM_ERROR, ValidationConstants.PASSWORD_MESSAGE);
            }
            rawPassword = body.getPassword();
        } else {
            // 不再使用统一默认口令，改为服务端随机生成一次性初始口令
            rawPassword = generateInitialPassword();
        }
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setStatus(1);
        user.setTokenVersion(0);
        user.setRefreshTokenVersion(0);
        // 新建账号一律强制首次登录改密（即使管理员显式设密，也要求用户首次登录后更换）
        user.setMustChangePassword(Boolean.TRUE);
        userRepository.save(user);

        upsertContactInfo(user.getId(), body.getEmail(), body.getPhone());

        for (Long roleId : roleIds) {
            UserRole ur = new UserRole();
            ur.setUserId(user.getId());
            ur.setRoleId(roleId);
            userRoleRepository.save(ur);
        }

        if (isStudent) {
            StudentProfile sp = new StudentProfile();
            sp.setUserId(user.getId());
            sp.setClassId(body.getClassId());
            studentProfileRepository.save(sp);
        }
        if (isTeacher) {
            TeacherProfile tp = new TeacherProfile();
            tp.setUserId(user.getId());
            tp.setCollegeId(body.getCollegeId());
            teacherProfileRepository.save(tp);
        }

        return CreateUserResponse.builder()
                .userId(user.getId())
                .mustChangePassword(Boolean.TRUE)
                // 仅服务端生成时回传一次性明文口令；管理员自设密码时不回传
                .initialPassword(passwordProvided ? null : rawPassword)
                .build();
    }

    // ==================== 6.4 更新用户信息 ====================

    @Transactional
    public void updateUser(Long operatorId, Long userId, UpdateUserRequest body) {
        adminAuthService.requireAdminOrPermission(operatorId, "user:manage");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在"));
        if (body.getName() != null && !body.getName().isBlank()) {
            user.setName(body.getName().trim());
        }
        if (body.getGender() != null) {
            user.setGender(body.getGender());
        }
        userRepository.save(user);

        if (body.getEmail() != null || body.getPhone() != null) {
            upsertContactInfo(userId, body.getEmail(), body.getPhone());
        }
        if (body.getClassId() != null) {
            upsertStudentProfile(userId, body.getClassId());
        }
        if (body.getCollegeId() != null) {
            upsertTeacherProfile(userId, body.getCollegeId());
        }
        // majorId：无对应存储列（学生归属由 classId 决定，教师归属由 collegeId 决定），接受但不落库。
    }

    // ==================== 6.5 启用/禁用用户 ====================

    @Transactional
    public void updateStatus(Long operatorId, Long userId, Integer status) {
        adminAuthService.requireAdminOrPermission(operatorId, "user:manage");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在"));
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能为 0(禁用) 或 1(正常)");
        }
        user.setStatus(status);
        userRepository.save(user);
        // 禁用后使该用户所有 Token 立即失效（access 与 refresh 一并递增，防止旧 refresh 换新 access 复活）
        if (Integer.valueOf(0).equals(status)) {
            userRepository.revokeAllTokens(userId);
        }
    }

    // ==================== 6.6 重置用户密码 ====================

    @Transactional
    public void resetPassword(Long operatorId, Long userId, String newPassword) {
        adminAuthService.requireAdminOrPermission(operatorId, "user:manage");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在"));
        if (newPassword == null || newPassword.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "新密码不能为空");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        // 管理员重置后，用户下次登录须自行修改密码
        user.setMustChangePassword(Boolean.TRUE);
        userRepository.save(user);
        // 重置成功后立即吊销该用户全部旧 access / refresh，踢出所有已登录会话。
        // 否则账号被盗后重置密码，攻击者仍可凭旧 access 访问、凭旧 refresh 续期。
        userRepository.revokeAllTokens(userId);
    }

    // ==================== 6.7 分配用户角色（覆盖式） ====================

    @Transactional
    public void updateRoles(Long operatorId, Long userId, List<Long> roleIds) {
        adminAuthService.requireAdminOrPermission(operatorId, "user:manage");

        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在"));

        List<Long> distinctIds = roleIds == null
                ? Collections.emptyList()
                : roleIds.stream().distinct().collect(Collectors.toList());
        if (!distinctIds.isEmpty()) {
            List<Role> roles = roleRepository.findByIdIn(distinctIds);
            if (roles.size() != distinctIds.size()) {
                throw new BusinessException(ResultCode.PARAM_ERROR, "存在非法的角色ID");
            }
        }

        // 覆盖式：删除旧关联再插入新关联，role_scopes 中该用户的数据范围不受影响
        List<UserRole> old = userRoleRepository.findByUserId(userId);
        if (!old.isEmpty()) {
            userRoleRepository.deleteAll(old);
            // 立即刷库执行 DELETE，避免后续 IDENTITY 自增 INSERT 先于 DELETE 触发，
            // 撞上 user_roles 唯一索引 uk_user_roles(user_id, role_id, is_deleted_null)
            userRoleRepository.flush();
        }
        for (Long roleId : distinctIds) {
            UserRole ur = new UserRole();
            ur.setUserId(userId);
            ur.setRoleId(roleId);
            userRoleRepository.save(ur);
        }
    }

    // ==================== 6.8 配置教师数据范围（覆盖式） ====================

    @Transactional
    public void updateScopes(Long operatorId, Long userId, List<ScopeConfigItem> scopes) {
        adminAuthService.requireAdminOrPermission(operatorId, "user:manage");

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在"));
        List<Long> scopeRoleIds = resolveScopeRoleIds(userId);

        List<ScopeConfigItem> items = scopes == null ? Collections.emptyList() : scopes;
        for (ScopeConfigItem item : items) {
            validateScope(item);
        }

        List<RoleScope> old = roleScopeRepository.findByUserId(userId);
        if (!old.isEmpty()) {
            roleScopeRepository.deleteAll(old);
            // 立即刷库执行 DELETE，避免后续 INSERT 先于 DELETE 撞上
            // role_scopes 唯一索引 uk_role_scopes_assign(user_id, role_id, scope_type, scope_id, semester_id, is_deleted_null)
            roleScopeRepository.flush();
        }
        // 同一份范围同时写入该用户持有的每个「可持有范围」的角色：
        // 兼辅导员的教师需要 teacher + counselor 两套 role_scopes，否则辅导员端范围筛选恒为空。
        // uk_role_scopes_assign 含 role_id，同一 scope 挂多个角色不冲突。
        for (Long roleId : scopeRoleIds) {
            for (ScopeConfigItem item : items) {
                RoleScope rs = new RoleScope();
                rs.setUserId(userId);
                rs.setSchoolId(user.getSchoolId());
                rs.setRoleId(roleId);
                rs.setScopeType(item.getScopeType());
                rs.setScopeId(item.getScopeId());
                rs.setSemesterId(item.getSemesterId());
                rs.setIsPrimary(1);
                rs.setAppointBy(operatorId);
                rs.setValidFrom(LocalDate.now());
                rs.setStatus(1);
                roleScopeRepository.save(rs);
            }
        }
    }

    // ==================== 列表查询辅助 ====================

    /** 解析 roleId / grade / collegeId / classId / keyword 为候选用户 ID 集合（多条件取交集，无此类筛选返回 null） */
    private Set<Long> resolveCandidateUserIds(UserListQuery query) {
        Set<Long> result = null;
        if (query.getRoleId() != null) {
            result = intersect(result, resolveRoleUserIds(query.getRoleId()));
        }
        if (query.getGrade() != null && !query.getGrade().isBlank()) {
            result = intersect(result, resolveGradeUserIds(query.getGrade()));
        }
        if (query.getClassId() != null) {
            result = intersect(result, resolveClassUserIds(query.getClassId()));
        }
        if (query.getCollegeId() != null) {
            result = intersect(result, resolveCollegeUserIds(query.getCollegeId()));
        }
        if (query.getKeyword() != null && !query.getKeyword().isBlank()) {
            result = intersect(result, resolveKeywordUserIds(query.getKeyword()));
        }
        return result;
    }

    private Set<Long> resolveRoleUserIds(Long roleId) {
        return userRoleRepository.findByRoleId(roleId).stream()
                .map(UserRole::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Set<Long> resolveGradeUserIds(String grade) {
        List<Clazz> classes = clazzRepository.findByGrade(grade.trim());
        List<Long> classIds = classes.stream().map(Clazz::getId).filter(Objects::nonNull).collect(Collectors.toList());
        if (classIds.isEmpty()) {
            return Collections.emptySet();
        }
        return studentProfileRepository.findByClassIdIn(classIds).stream()
                .map(StudentProfile::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Set<Long> resolveClassUserIds(Long classId) {
        return studentProfileRepository.findByClassId(classId).stream()
                .map(StudentProfile::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Set<Long> resolveCollegeUserIds(Long collegeId) {
        return teacherProfileRepository.findByCollegeId(collegeId).stream()
                .map(TeacherProfile::getUserId).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private Set<Long> resolveKeywordUserIds(String keyword) {
        String kw = keyword.trim();
        Set<Long> ids = new HashSet<>();
        userRepository.findByNameContainingOrUserNoContaining(kw, kw)
                .forEach(u -> { if (u.getId() != null) ids.add(u.getId()); });
        userContactInfoRepository.findByPhoneContainingOrEmailContaining(kw, kw)
                .forEach(ci -> { if (ci.getUserId() != null) ids.add(ci.getUserId()); });
        return ids;
    }

    private Set<Long> intersect(Set<Long> current, Set<Long> next) {
        if (current == null) {
            return new HashSet<>(next);
        }
        current.retainAll(next);
        return current;
    }

    private Specification<User> buildSpecification(UserListQuery query, Set<Long> candidateIds) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (query.getStatus() != null) {
                predicates.add(cb.equal(root.get("status"), query.getStatus()));
            }
            if (query.getSchoolId() != null) {
                predicates.add(cb.equal(root.get("schoolId"), query.getSchoolId()));
            }
            if (candidateIds != null) {
                predicates.add(root.get("id").in(candidateIds));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /** 批量联查并组装用户列表项 */
    private List<UserListItem> toUserListItems(List<User> users) {
        if (users.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());

        // 角色
        Map<Long, List<Role>> rolesByUser;
        List<UserRole> userRoles = userRoleRepository.findByUserIdIn(userIds);
        if (userRoles.isEmpty()) {
            rolesByUser = Map.of();
        } else {
            Set<Long> roleIds = userRoles.stream().map(UserRole::getRoleId).filter(Objects::nonNull).collect(Collectors.toSet());
            Map<Long, Role> roleMap = roleIds.isEmpty()
                    ? Map.of()
                    : roleRepository.findByIdIn(new ArrayList<>(roleIds)).stream()
                            .collect(Collectors.toMap(Role::getId, r -> r, (a, b) -> a));
            rolesByUser = userRoles.stream()
                    .filter(ur -> roleMap.containsKey(ur.getRoleId()))
                    .collect(Collectors.groupingBy(UserRole::getUserId,
                            Collectors.mapping(ur -> roleMap.get(ur.getRoleId()), Collectors.toList())));
        }

        // 联系方式
        Map<Long, UserContactInfo> contactMap = userContactInfoRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(UserContactInfo::getUserId, c -> c, (a, b) -> a));

        // 学生/教师档案
        Map<Long, StudentProfile> studentMap = studentProfileRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, s -> s, (a, b) -> a));
        Map<Long, TeacherProfile> teacherMap = teacherProfileRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(TeacherProfile::getUserId, t -> t, (a, b) -> a));

        // 组织层级（用于 departmentPath）
        Set<Long> classIds = studentMap.values().stream().map(StudentProfile::getClassId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Clazz> clazzMap = loadClasses(new ArrayList<>(classIds));
        Set<Long> majorIds = clazzMap.values().stream().map(Clazz::getMajorId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Major> majorMap = loadMajors(new ArrayList<>(majorIds));
        Set<Long> collegeIds = new HashSet<>(majorMap.values().stream().map(Major::getCollegeId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        collegeIds.addAll(teacherMap.values().stream().map(TeacherProfile::getCollegeId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, College> collegeMap = loadColleges(new ArrayList<>(collegeIds));

        // 学校
        Set<Long> schoolIds = users.stream().map(User::getSchoolId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, School> schoolMap = schoolIds.isEmpty()
                ? Map.of()
                : schoolRepository.findByIdIn(new ArrayList<>(schoolIds)).stream()
                        .collect(Collectors.toMap(School::getId, s -> s, (a, b) -> a));

        return users.stream().map(u -> {
            List<Role> roles = rolesByUser.getOrDefault(u.getId(), Collections.emptyList());
            UserContactInfo ci = contactMap.get(u.getId());
            return UserListItem.builder()
                    .userId(u.getId())
                    .userNo(u.getUserNo())
                    .name(u.getName())
                    .gender(u.getGender())
                    .genderLabel(GenderEnum.of(u.getGender()).getLabel())
                    .email(ci != null ? ci.getEmail() : null)
                    .phone(ci != null ? ci.getPhone() : null)
                    .schoolId(u.getSchoolId())
                    .schoolName(schoolMap.containsKey(u.getSchoolId()) ? schoolMap.get(u.getSchoolId()).getName() : null)
                    .roles(roles.stream().map(Role::getCode).collect(Collectors.toList()))
                    .roleNames(roles.stream().map(Role::getName).collect(Collectors.toList()))
                    .status(u.getStatus())
                    .statusLabel(accountStatusLabel(u.getStatus()))
                    .departmentPath(buildDepartmentPath(u, studentMap, teacherMap, clazzMap, majorMap, collegeMap))
                    .createdAt(toIso(u.getCreatedAt()))
                    .build();
        }).collect(Collectors.toList());
    }

    // ==================== 详情/数据范围辅助 ====================

    /** 组装教师数据范围列表（scopeName 按 scopeType 关联学院/专业/班级名称） */
    private List<UserScopeItem> toScopeItems(List<RoleScope> scopes) {
        if (scopes.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Integer, List<Long>> idsByType = scopes.stream().collect(Collectors.groupingBy(
                RoleScope::getScopeType,
                Collectors.mapping(RoleScope::getScopeId, Collectors.toList())));
        Map<Long, College> collegeMap = loadColleges(idsByType.getOrDefault(2, Collections.emptyList()));
        Map<Long, Major> majorMap = loadMajors(idsByType.getOrDefault(3, Collections.emptyList()));
        Map<Long, Clazz> clazzMap = loadClasses(idsByType.getOrDefault(4, Collections.emptyList()));

        return scopes.stream().map(s -> UserScopeItem.builder()
                .scopeType(s.getScopeType())
                .scopeTypeLabel(scopeTypeLabel(s.getScopeType()))
                .scopeId(s.getScopeId())
                .scopeName(resolveScopeName(s.getScopeType(), s.getScopeId(), collegeMap, majorMap, clazzMap))
                .build()).collect(Collectors.toList());
    }

    private String resolveScopeName(Integer scopeType, Long scopeId,
                                    Map<Long, College> collegeMap, Map<Long, Major> majorMap, Map<Long, Clazz> clazzMap) {
        if (scopeType == null || scopeId == null) {
            return null;
        }
        return switch (scopeType) {
            case 2 -> collegeMap.containsKey(scopeId) ? collegeMap.get(scopeId).getName() : null;
            case 3 -> majorMap.containsKey(scopeId) ? majorMap.get(scopeId).getName() : null;
            case 4 -> clazzMap.containsKey(scopeId) ? clazzMap.get(scopeId).getName() : null;
            case 6 -> dictionaryRepository.findById(scopeId)
                    .filter(d -> "grade".equals(d.getDictType()))
                    .map(Dictionary::getDictCode).orElse(null);
            default -> null;
        };
    }

    /**
     * 定位配置数据范围时需要写入的角色 ID 列表：teacher 必选，持有 counselor 时追加 counselor。
     * <p>
     * 辅导员也是教师，同一用户可同时持有 teacher + counselor，而两个角色各自需要一套
     * role_scopes（教师端与辅导员端的范围筛选分别按角色读取）。此前只写 teacher 角色，
     * 导致兼辅导员的账号在辅导员端范围筛选恒为空。
     *
     * @param userId 目标用户 ID
     * @return 角色 ID 列表，至少含 teacher 角色
     */
    private List<Long> resolveScopeRoleIds(Long userId) {
        List<Long> roleIds = userRoleRepository.findByUserId(userId).stream()
                .map(UserRole::getRoleId).filter(Objects::nonNull).collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该用户未分配教师角色");
        }
        Map<String, Long> idByCode = roleRepository.findByIdIn(roleIds).stream()
                .filter(r -> r.getCode() != null)
                .collect(Collectors.toMap(Role::getCode, Role::getId, (a, b) -> a));
        List<Long> scopeRoleIds = new ArrayList<>();
        for (String code : new String[]{ROLE_CODE_TEACHER, ROLE_CODE_COUNSELOR}) {
            Long roleId = idByCode.get(code);
            if (roleId != null) {
                scopeRoleIds.add(roleId);
            }
        }
        if (scopeRoleIds.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "该用户未分配教师角色");
        }
        return scopeRoleIds;
    }

    /** 校验数据范围存在且启用（scopeType 支持 2学院/3专业/4班级/6年级） */
    private void validateScope(ScopeConfigItem item) {
        if (item.getScopeType() == null || item.getScopeId() == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "scopeType 与 scopeId 不能为空");
        }
        boolean enabled = switch (item.getScopeType()) {
            case 2 -> collegeRepository.findById(item.getScopeId())
                    .map(c -> Integer.valueOf(1).equals(c.getStatus())).orElse(false);
            case 3 -> majorRepository.findById(item.getScopeId())
                    .map(m -> Integer.valueOf(1).equals(m.getStatus())).orElse(false);
            case 4 -> clazzRepository.findById(item.getScopeId())
                    .map(c -> Integer.valueOf(1).equals(c.getStatus())).orElse(false);
            case 6 -> dictionaryRepository.findById(item.getScopeId())
                    .filter(d -> "grade".equals(d.getDictType()))
                    .map(d -> Integer.valueOf(1).equals(d.getStatus())).orElse(false);
            default -> throw new BusinessException(ResultCode.PARAM_ERROR,
                    "scopeType 只能为 2(学院)/3(专业)/4(班级)/6(年级)");
        };
        if (!enabled) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "数据范围不存在或已停用");
        }
    }

    // ==================== 写操作辅助 ====================

    private void upsertContactInfo(Long userId, String email, String phone) {
        if ((email == null || email.isBlank()) && (phone == null || phone.isBlank())) {
            return;
        }
        UserContactInfo ci = userContactInfoRepository.findByUserId(userId).orElseGet(() -> {
            UserContactInfo n = new UserContactInfo();
            n.setUserId(userId);
            return n;
        });
        if (email != null && !email.isBlank()) {
            ci.setEmail(email.trim());
        }
        if (phone != null && !phone.isBlank()) {
            ci.setPhone(phone.trim());
        }
        userContactInfoRepository.save(ci);
    }

    private void upsertStudentProfile(Long userId, Long classId) {
        StudentProfile sp = studentProfileRepository.findByUserId(userId).orElseGet(() -> {
            StudentProfile n = new StudentProfile();
            n.setUserId(userId);
            return n;
        });
        sp.setClassId(classId);
        studentProfileRepository.save(sp);
    }

    private void upsertTeacherProfile(Long userId, Long collegeId) {
        TeacherProfile tp = teacherProfileRepository.findByUserId(userId).orElseGet(() -> {
            TeacherProfile n = new TeacherProfile();
            n.setUserId(userId);
            return n;
        });
        tp.setCollegeId(collegeId);
        teacherProfileRepository.save(tp);
    }

    // ==================== 通用辅助 ====================

    /**
     * 生成一次性初始口令：12 位，保证包含大写/小写/数字/特殊字符，满足 PASSWORD_PATTERN。
     * 使用 SecureRandom，避免可预测；仅在创建响应里回传一次。
     */
    private String generateInitialPassword() {
        String all = PWD_UPPER + PWD_LOWER + PWD_DIGIT + PWD_SPECIAL;
        List<Character> chars = new ArrayList<>(INITIAL_PASSWORD_LENGTH);
        chars.add(pick(PWD_UPPER));
        chars.add(pick(PWD_LOWER));
        chars.add(pick(PWD_DIGIT));
        chars.add(pick(PWD_SPECIAL));
        for (int i = 4; i < INITIAL_PASSWORD_LENGTH; i++) {
            chars.add(pick(all));
        }
        Collections.shuffle(chars, SECURE_RANDOM);
        StringBuilder sb = new StringBuilder(INITIAL_PASSWORD_LENGTH);
        chars.forEach(sb::append);
        return sb.toString();
    }

    private char pick(String alphabet) {
        return alphabet.charAt(SECURE_RANDOM.nextInt(alphabet.length()));
    }


    private Map<Long, College> loadColleges(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return collegeRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(College::getId, c -> c, (a, b) -> a));
    }

    private Map<Long, Major> loadMajors(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return majorRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Major::getId, m -> m, (a, b) -> a));
    }

    private Map<Long, Clazz> loadClasses(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return clazzRepository.findByIdIn(ids).stream()
                .collect(Collectors.toMap(Clazz::getId, c -> c, (a, b) -> a));
    }

    private String buildDepartmentPath(User user, Map<Long, StudentProfile> studentMap, Map<Long, TeacherProfile> teacherMap,
                                       Map<Long, Clazz> clazzMap, Map<Long, Major> majorMap, Map<Long, College> collegeMap) {
        StudentProfile sp = studentMap.get(user.getId());
        if (sp != null && sp.getClassId() != null) {
            Clazz clazz = clazzMap.get(sp.getClassId());
            if (clazz != null) {
                Major major = clazz.getMajorId() != null ? majorMap.get(clazz.getMajorId()) : null;
                College college = (major != null && major.getCollegeId() != null) ? collegeMap.get(major.getCollegeId()) : null;
                return joinPath(college != null ? college.getName() : null,
                        major != null ? major.getName() : null,
                        clazz.getName());
            }
            return null;
        }
        TeacherProfile tp = teacherMap.get(user.getId());
        if (tp != null && tp.getCollegeId() != null) {
            College college = collegeMap.get(tp.getCollegeId());
            return college != null ? college.getName() : null;
        }
        return null;
    }

    private String joinPath(String... parts) {
        List<String> nonNull = new ArrayList<>();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                nonNull.add(p);
            }
        }
        return nonNull.isEmpty() ? null : String.join("/", nonNull);
    }

    private String accountStatusLabel(Integer status) {
        return Integer.valueOf(1).equals(status) ? "正常" : "禁用";
    }

    private String scopeTypeLabel(Integer scopeType) {
        ScopeTypeEnum e = ScopeTypeEnum.of(scopeType);
        return e != null ? e.getLabel() : "未知";
    }

    /** LocalDateTime → ISO 8601 带时区字符串 */
    private String toIso(LocalDateTime dateTime) {
        return dateTime != null
                ? dateTime.atZone(ZoneId.systemDefault()).format(ISO_WITH_ZONE)
                : null;
    }

    // ==================== 内嵌 POJO ====================

    /** 6.1 查询条件 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserListQuery {
        private Long roleId;
        private Integer status;
        private String grade;
        private Long collegeId;
        private Long classId;
        private String keyword;
        private Long schoolId;
    }

    /** 6.1 列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserListItem {
        private Long userId;
        private String userNo;
        private String name;
        private Integer gender;
        private String genderLabel;
        private String email;
        private String phone;
        private Long schoolId;
        private String schoolName;
        private List<String> roles;
        private List<String> roleNames;
        private Integer status;
        private String statusLabel;
        private String departmentPath;
        private String createdAt;
    }

    /** 6.2 用户详情 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserDetail {
        private Long userId;
        private String userNo;
        private String name;
        private String email;
        private String phone;
        private Integer gender;
        private String genderLabel;
        private String birthDate;
        private Long schoolId;
        private String schoolName;
        private List<RoleItem> roles;
        private Integer status;
        private String statusLabel;
        private List<UserScopeItem> scopes;
        private String createdAt;
        private String lastLoginAt;
    }

    /** 6.2 角色信息 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoleItem {
        private Long roleId;
        private String roleName;
        private Integer level;
    }

    /** 6.2 数据范围信息 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserScopeItem {
        private Integer scopeType;
        private String scopeTypeLabel;
        private Long scopeId;
        private String scopeName;
    }

    /** 6.3 创建用户请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateUserRequest {
        private String userNo;
        private String name;
        private String email;
        private String phone;
        private Integer gender;
        private Long schoolId;
        private List<Long> roleIds;
        private Long classId;
        private Long collegeId;
        private String password;
    }

    /** 6.3 创建用户响应 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateUserResponse {
        private Long userId;
        /** 是否需首次登录强制改密 */
        private Boolean mustChangePassword;
        /** 服务端随机生成的一次性初始口令（仅生成时返回一次，不落日志、不再回显） */
        private String initialPassword;
    }

    /** 6.4 更新用户请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateUserRequest {
        private String name;
        private String email;
        private String phone;
        private Integer gender;
        private Long classId;
        private Long majorId;
        private Long collegeId;
    }

    /** 6.5 启用/禁用请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        private Integer status;
    }

    /** 6.6 重置密码请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ResetPasswordRequest {
        private String newPassword;
    }

    /** 6.7 分配角色请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRolesRequest {
        private List<Long> roleIds;
    }

    /** 6.8 配置数据范围请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateScopesRequest {
        private List<ScopeConfigItem> scopes;
    }

    /** 6.8 数据范围子对象（scopeType 2学院/3专业/4班级） */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScopeConfigItem {
        private Integer scopeType;
        private Long scopeId;
        private Long semesterId;
    }
}