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
import java.util.List;

/**
 * 管理端基础组织架构管理服务（Lzw）
 * <p>
 * 对应《管理端接口文档》十一、基础组织架构管理模块（11.1 ~ 11.7）。
 * 数据来源：schools、colleges、majors、classes。
 * <p>
 * 权限：文档未为组织架构列出权限码，且附录接口关系表未收录本模块，故全部要求 admin 角色（越权返回 20005）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrgManageService {

    private final SchoolRepository schoolRepository;
    private final CollegeRepository collegeRepository;
    private final MajorRepository majorRepository;
    private final ClazzRepository clazzRepository;
    private final AdminAuthService adminAuthService;

    // ==================== 11.1 获取学校列表 ====================

    @Transactional(readOnly = true)
    public List<SchoolItem> listSchools(Long operatorId) {
        adminAuthService.requireAdmin(operatorId);
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
        adminAuthService.requireAdmin(operatorId);
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
        return colleges.stream().map(c -> CollegeItem.builder()
                .collegeId(c.getId())
                .collegeName(c.getName())
                .code(c.getCode())
                .schoolId(c.getSchoolId())
                .status(c.getStatus())
                .build()).toList();
    }

    // ==================== 11.3 获取专业列表 ====================

    @Transactional(readOnly = true)
    public List<MajorItem> listMajors(Long operatorId, Long collegeId, Long schoolId, Integer status) {
        adminAuthService.requireAdmin(operatorId);

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
        return majors.stream().map(m -> MajorItem.builder()
                .majorId(m.getId())
                .majorName(m.getName())
                .code(m.getCode())
                .collegeId(m.getCollegeId())
                .status(m.getStatus())
                .build()).toList();
    }

    // ==================== 11.4 获取班级列表 ====================

    @Transactional(readOnly = true)
    public PageResult<ClassItem> listClasses(Long operatorId, Long majorId, Long collegeId, Long schoolId,
                                             String grade, Integer status, String keyword, PageParam pageParam) {
        adminAuthService.requireAdmin(operatorId);

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

        List<ClassItem> items = page.getContent().stream().map(c -> ClassItem.builder()
                .classId(c.getId())
                .className(c.getName())
                .grade(c.getGrade())
                .majorId(c.getMajorId())
                .studentCount(c.getStudentCount())
                .status(c.getStatus())
                .build()).toList();

        return PageResult.of(items, page.getTotalElements(), pageParam);
    }

    // ==================== 11.5 创建班级 ====================

    @Transactional
    public ClassIdResponse createClass(Long operatorId, ClassSaveRequest body) {
        adminAuthService.requireAdmin(operatorId);

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
        adminAuthService.requireAdmin(operatorId);

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
        adminAuthService.requireAdmin(operatorId);

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
