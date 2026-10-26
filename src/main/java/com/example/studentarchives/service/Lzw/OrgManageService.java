package com.example.studentarchives.service.Lzw;

import com.example.studentarchives.common.PageParam;
import com.example.studentarchives.common.PageResult;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.College;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.org.School;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.CollegeRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.SchoolRepository;
import com.example.studentarchives.repository.StudentProfileRepository;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 管理端基础组织架构管理服务（Lzw）
 * <p>
 * 对应《管理端接口文档》十一、基础组织架构管理模块（11.1 ~ 11.7）。
 * 数据来源：schools、colleges、majors、classes。
 * <p>
 * 权限：admin 角色或 org:manage 权限码（越权返回 20005）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgManageService {

    private final SchoolRepository schoolRepository;
    private final CollegeRepository collegeRepository;
    private final MajorRepository majorRepository;
    private final ClazzRepository clazzRepository;
    private final StudentProfileRepository studentProfileRepository;
    private final AdminAuthService adminAuthService;

    // ==================== 11.1 获取学校列表 ====================

    @Transactional(readOnly = true)
    public List<SchoolItem> listSchools(Long operatorId) {
        adminAuthService.requireAdminOrPermission(operatorId, "org:manage");
        List<School> schools = schoolRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        return schools.stream().map(s -> SchoolItem.builder()
                .schoolId(s.getId())
                .schoolName(s.getName())
                .code(s.getCode())
                .status(s.getStatus())
                .build()).toList();
    }

    // ==================== 11.2 获取学院列表 ====================

    @Transactional(readOnly = true)
    public List<CollegeItem> listColleges(Long operatorId, Long schoolId, Integer status) {
        adminAuthService.requireAdminOrPermission(operatorId, "org:manage");
        if (schoolId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "schoolId 不能为空");
        }

        Specification<College> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("schoolId"), schoolId));
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<College> colleges = collegeRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"));
        // 学生人数实时按 student_profiles.class_id → 专业 → 学院 逐级汇总
        Map<Long, Integer> studentCountMap = loadStudentCountsByCollege(colleges);
        return colleges.stream().map(c -> CollegeItem.builder()
                .collegeId(c.getId())
                .collegeName(c.getName())
                .code(c.getCode())
                .schoolId(c.getSchoolId())
                .studentCount(studentCountMap.getOrDefault(c.getId(), 0))
                .status(c.getStatus())
                .build()).toList();
    }

    // ==================== 11.3 获取专业列表 ====================

    @Transactional(readOnly = true)
    public List<MajorItem> listMajors(Long operatorId, Long collegeId, Long schoolId, Integer status) {
        adminAuthService.requireAdminOrPermission(operatorId, "org:manage");

        // 解析学院范围：collegeId 优先，其次按 schoolId 下钻
        List<Long> collegeIds = null;
        if (collegeId != null) {
            if (schoolId != null) {
                College c = collegeRepository.findById(collegeId).orElse(null);
                if (c == null || !schoolId.equals(c.getSchoolId())) {
                    return List.of();
                }
            }
            collegeIds = List.of(collegeId);
        } else if (schoolId != null) {
            collegeIds = collegeRepository.findBySchoolId(schoolId).stream().map(College::getId).toList();
            if (collegeIds.isEmpty()) {
                return List.of();
            }
        }

        final List<Long> filterCollegeIds = collegeIds;
        Specification<Major> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filterCollegeIds != null) {
                predicates.add(root.get("collegeId").in(filterCollegeIds));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        List<Major> majors = majorRepository.findAll(spec, Sort.by(Sort.Direction.ASC, "id"));
        // 学生人数实时按 student_profiles.class_id → 专业 汇总
        Map<Long, Integer> studentCountMap = loadStudentCountsByMajor(majors);
        return majors.stream().map(m -> MajorItem.builder()
                .majorId(m.getId())
                .majorName(m.getName())
                .code(m.getCode())
                .collegeId(m.getCollegeId())
                .studentCount(studentCountMap.getOrDefault(m.getId(), 0))
                .status(m.getStatus())
                .build()).toList();
    }

    // ==================== 11.4 获取班级列表 ====================

    @Transactional(readOnly = true)
    public PageResult<ClassItem> listClasses(Long operatorId, Long majorId, Long collegeId, Long schoolId,
                                             String grade, Integer status, String keyword, PageParam pageParam) {
        adminAuthService.requireAdminOrPermission(operatorId, "org:manage");

        List<Long> majorIds = resolveMajorIds(majorId, collegeId, schoolId);
        if (majorIds != null && majorIds.isEmpty()) {
            return PageResult.of(List.of(), 0, pageParam);
        }

        final List<Long> filterMajorIds = majorIds;
        final String kw = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Specification<Clazz> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (filterMajorIds != null) {
                predicates.add(root.get("majorId").in(filterMajorIds));
            }
            if (grade != null && !grade.isBlank()) {
                predicates.add(cb.equal(root.get("grade"), grade.trim()));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (kw != null) {
                predicates.add(cb.like(cb.lower(root.get("name")), "%" + kw.toLowerCase() + "%"));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort sort = Sort.by(Sort.Direction.ASC, "id");
        Pageable pageable = PageRequest.of(pageParam.getPage() - 1, pageParam.getPerPage(), sort);
        Page<Clazz> page = clazzRepository.findAll(spec, pageable);
        List<Clazz> classes = page.getContent();
        // 学生人数实时按 student_profiles.class_id 统计（clazz.student_count 列建班时仅置 0，不再维护）
        Map<Long, Long> studentCountMap = loadStudentCounts(classes);

        List<ClassItem> items = classes.stream().map(c -> ClassItem.builder()
                .classId(c.getId())
                .className(c.getName())
                .grade(c.getGrade())
                .majorId(c.getMajorId())
                .studentCount(studentCountMap.getOrDefault(c.getId(), 0L).intValue())
                .status(c.getStatus())
                .build()).toList();

        return PageResult.of(items, page.getTotalElements(), pageParam);
    }

    // ==================== 11.5 创建班级 ====================

    @Transactional
    public ClassIdResponse createClass(Long operatorId, ClassSaveRequest body) {
        adminAuthService.requireAdminOrPermission(operatorId, "org:manage");

        Long majorId = body.getMajorId();
        if (majorId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "majorId 不能为空");
        }
        String className = requireNotBlank(body.getClassName(), "className 不能为空");
        String grade = requireNotBlank(body.getGrade(), "grade 不能为空");
        Integer status = body.getStatus() != null ? body.getStatus() : 1;
        validateStatus(status);

        majorRepository.findById(majorId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "专业不存在"));

        Clazz clazz = new Clazz();
        clazz.setMajorId(majorId);
        clazz.setName(className);
        clazz.setGrade(grade);
        clazz.setStudentCount(0);
        clazz.setStatus(status);
        clazzRepository.save(clazz);

        return ClassIdResponse.builder().classId(clazz.getId()).build();
    }

    // ==================== 11.6 更新班级 ====================

    @Transactional
    public void updateClass(Long operatorId, Long classId, ClassSaveRequest body) {
        adminAuthService.requireAdminOrPermission(operatorId, "org:manage");

        Clazz clazz = clazzRepository.findById(classId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "班级不存在"));

        if (body.getMajorId() != null) {
            majorRepository.findById(body.getMajorId())
                    .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "专业不存在"));
            clazz.setMajorId(body.getMajorId());
        }
        if (body.getClassName() != null) {
            clazz.setName(requireNotBlank(body.getClassName(), "className 不能为空"));
        }
        if (body.getGrade() != null) {
            clazz.setGrade(requireNotBlank(body.getGrade(), "grade 不能为空"));
        }
        if (body.getStatus() != null) {
            validateStatus(body.getStatus());
            clazz.setStatus(body.getStatus());
        }

        clazzRepository.save(clazz);
    }

    // ==================== 11.7 创建专业 ====================

    @Transactional
    public MajorIdResponse createMajor(Long operatorId, MajorCreateRequest body) {
        adminAuthService.requireAdminOrPermission(operatorId, "org:manage");

        Long collegeId = body.getCollegeId();
        if (collegeId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "collegeId 不能为空");
        }
        String majorName = requireNotBlank(body.getMajorName(), "majorName 不能为空");
        String majorCode = requireNotBlank(body.getMajorCode(), "majorCode 不能为空");
        Integer status = body.getStatus() != null ? body.getStatus() : 1;
        validateStatus(status);

        collegeRepository.findById(collegeId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学院不存在"));

        majorRepository.findByCollegeIdAndCode(collegeId, majorCode)
                .ifPresent(existing -> {
                    throw new BusinessException(ResultCode.DATA_ALREADY_EXISTS, "该学院下已存在相同专业代码");
                });

        Major major = new Major();
        major.setCollegeId(collegeId);
        major.setName(majorName);
        major.setCode(majorCode);
        major.setStatus(status);
        majorRepository.save(major);

        return MajorIdResponse.builder().majorId(major.getId()).build();
    }

    // ==================== 查询辅助 ====================

    /**
     * 按 majorId / collegeId / schoolId 三级下钻解析班级所属专业 ID 集合。
     * 返回 null 表示不按专业过滤（未指定任何组织维度）。
     */
    private List<Long> resolveMajorIds(Long majorId, Long collegeId, Long schoolId) {
        if (majorId != null) {
            if (collegeId != null) {
                Major m = majorRepository.findById(majorId).orElse(null);
                if (m == null || !collegeId.equals(m.getCollegeId())) {
                    return List.of();
                }
            }
            return List.of(majorId);
        }
        if (collegeId != null) {
            List<Long> ids = majorRepository.findByCollegeIdIn(List.of(collegeId)).stream().map(Major::getId).toList();
            return ids;
        }
        if (schoolId != null) {
            List<Long> collegeIds = collegeRepository.findBySchoolId(schoolId).stream().map(College::getId).toList();
            if (collegeIds.isEmpty()) {
                return List.of();
            }
            List<Long> ids = majorRepository.findByCollegeIdIn(collegeIds).stream().map(Major::getId).toList();
            return ids;
        }
        return null;
    }

    /** 批量统计各班学生人数（student_profiles.class_id → 人数），无学生的班级默认 0 */
    private Map<Long, Long> loadStudentCounts(List<Clazz> classes) {
        List<Long> classIds = classes.stream().map(Clazz::getId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (classIds.isEmpty()) {
            return Map.of();
        }
        return studentProfileRepository.countGroupedByClassIdIn(classIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> (Long) row[1], (a, b) -> a));
    }

    /** 批量统计各专业学生人数：专业 → 班级 → student_profiles.class_id 逐级汇总 */
    private Map<Long, Integer> loadStudentCountsByMajor(List<Major> majors) {
        List<Long> majorIds = majors.stream().map(Major::getId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (majorIds.isEmpty()) {
            return Map.of();
        }
        List<Clazz> classes = clazzRepository.findByMajorIdIn(majorIds);
        Map<Long, Long> classToMajor = classes.stream()
                .collect(Collectors.toMap(Clazz::getId, Clazz::getMajorId, (a, b) -> a));
        Map<Long, Integer> countByMajor = new HashMap<>();
        loadStudentCounts(classes).forEach((classId, count) -> {
            Long majorId = classToMajor.get(classId);
            if (majorId != null) {
                countByMajor.merge(majorId, count.intValue(), Integer::sum);
            }
        });
        return countByMajor;
    }

    /** 批量统计各学院学生人数：学院 → 专业 → 班级 → student_profiles.class_id 逐级汇总 */
    private Map<Long, Integer> loadStudentCountsByCollege(List<College> colleges) {
        List<Long> collegeIds = colleges.stream().map(College::getId)
                .filter(Objects::nonNull).collect(Collectors.toList());
        if (collegeIds.isEmpty()) {
            return Map.of();
        }
        List<Major> majors = majorRepository.findByCollegeIdIn(collegeIds);
        Map<Long, Long> majorToCollege = majors.stream()
                .collect(Collectors.toMap(Major::getId, Major::getCollegeId, (a, b) -> a));
        Map<Long, Integer> countByCollege = new HashMap<>();
        loadStudentCountsByMajor(majors).forEach((majorId, count) -> {
            Long collegeId = majorToCollege.get(majorId);
            if (collegeId != null) {
                countByCollege.merge(collegeId, count, Integer::sum);
            }
        });
        return countByCollege;
    }

    // ==================== 通用辅助 ====================

    private String requireNotBlank(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_MISSING, message);
        }
        return value.trim();
    }

    private void validateStatus(Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "status 只能为 0(禁用) 或 1(启用)");
        }
    }

    // ==================== 内嵌 POJO ====================

    /** 11.1 学校列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class SchoolItem {
        private Long schoolId;
        private String schoolName;
        private String code;
        private Integer status;
    }

    /** 11.2 学院列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CollegeItem {
        private Long collegeId;
        private String collegeName;
        private String code;
        private Long schoolId;
        /** 学生人数（实时按 student_profiles.class_id 逐级汇总） */
        private Integer studentCount;
        private Integer status;
    }

    /** 11.3 专业列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MajorItem {
        private Long majorId;
        private String majorName;
        private String code;
        private Long collegeId;
        /** 学生人数（实时按 student_profiles.class_id 逐级汇总） */
        private Integer studentCount;
        private Integer status;
    }

    /** 11.4 班级列表项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ClassItem {
        private Long classId;
        private String className;
        private String grade;
        private Long majorId;
        private Integer studentCount;
        private Integer status;
    }

    /** 11.5 / 11.6 班级保存请求（部分更新） */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassSaveRequest {
        private Long majorId;
        private String className;
        private String grade;
        private Integer status;
    }

    /** 11.7 创建专业请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MajorCreateRequest {
        private Long collegeId;
        private String majorName;
        private String majorCode;
        private Integer status;
    }

    /** 11.5 创建班级响应 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClassIdResponse {
        private Long classId;
    }

    /** 11.7 创建专业响应 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MajorIdResponse {
        private Long majorId;
    }
}
