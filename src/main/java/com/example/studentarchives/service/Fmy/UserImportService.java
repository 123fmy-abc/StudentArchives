package com.example.studentarchives.service.Fmy;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.OSSObject;
import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.config.Fmy.OssProperties;
import com.example.studentarchives.entity.file.AttachmentRelation;
import com.example.studentarchives.entity.org.Clazz;
import com.example.studentarchives.entity.org.College;
import com.example.studentarchives.entity.org.Major;
import com.example.studentarchives.entity.user.Role;
import com.example.studentarchives.exception.BusinessException;
import com.example.studentarchives.repository.AttachmentRelationRepository;
import com.example.studentarchives.repository.ClazzRepository;
import com.example.studentarchives.repository.CollegeRepository;
import com.example.studentarchives.repository.MajorRepository;
import com.example.studentarchives.repository.RoleRepository;
import com.example.studentarchives.repository.SchoolRepository;
import com.example.studentarchives.service.Lzw.UserManageService;
import com.example.studentarchives.service.Lzw.UserManageService.CreateUserRequest;
import com.example.studentarchives.service.common.AdminAuthService;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 管理端学生账号批量导入服务（Fmy）
 * <p>
 * 对应《管理端接口文档》六、用户管理模块的批量导入补充：POST /admin/users/import
 * 与 GET /admin/users/import-template。文件支持 .xlsx / .csv（模板列：学号/姓名/班级）。
 * <p>
 * 逐行创建学生账号（复用 {@link UserManageService#createUser}，默认密码 123456、角色=student），
 * 每行独立事务，失败行记入 failures 不中断其余行；班级列按「班级名称」在该学校下唯一解析为 classId。
 * <p>
 * 权限：admin 角色或 user:manage 权限码，越权返回 20005。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserImportService {

    /** 学生账号导入权限码（与创建用户一致） */
    private static final String IMPORT_PERMISSION = "user:manage";

    /** 学生角色编码（导入仅支持学生账号） */
    private static final String ROLE_CODE_STUDENT = "student";

    /** 学生账号导入模板列（学号/姓名/班级） */
    private static final List<GradeImportFileParser.TemplateColumn> USER_COLUMNS = List.of(
            new GradeImportFileParser.TemplateColumn("userNo", "学号", true),
            new GradeImportFileParser.TemplateColumn("name", "姓名", true),
            new GradeImportFileParser.TemplateColumn("className", "班级", true)
    );

    private final AdminAuthService adminAuthService;
    private final UserManageService userManageService;
    private final GradeImportFileParser fileParser;
    private final AttachmentRelationRepository attachmentRelationRepository;
    private final SchoolRepository schoolRepository;
    private final RoleRepository roleRepository;
    private final ClazzRepository clazzRepository;
    private final MajorRepository majorRepository;
    private final CollegeRepository collegeRepository;
    private final OSS ossClient;
    private final OssProperties ossProperties;

    /**
     * 下载学生账号批量导入模板（GET /admin/users/import-template）。
     * <p>
     * 返回标准 .xlsx 文件，含表头（学号/姓名/班级）与一行示例。
     */
    public byte[] importTemplate(Long operatorId) {
        adminAuthService.requireAdminOrPermission(operatorId, IMPORT_PERMISSION);
        return fileParser.buildXlsx(USER_COLUMNS, List.of("2026001", "张三", "计算机1班"));
    }

    /**
     * 批量导入学生账号（POST /admin/users/import）。
     * <p>
     * 同步逐行创建，每行独立事务；失败行记入 failures 不中断其余行。班级列按名称在该学校下
     * 唯一解析为 classId（同名多个班级时判为「名称不唯一」）。
     *
     * @param operatorId 当前登录用户 ID
     * @param body       导入请求（schoolId + fileId）
     * @return 逐行导入结果
     */
    public UserImportResponse importUsers(Long operatorId, UserImportRequest body) {
        adminAuthService.requireAdminOrPermission(operatorId, IMPORT_PERMISSION);

        Long schoolId = body.getSchoolId();
        if (schoolId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "schoolId 不能为空");
        }
        Long fileId = body.getFileId();
        if (fileId == null) {
            throw new BusinessException(ResultCode.PARAM_MISSING, "fileId 不能为空");
        }
        schoolRepository.findById(schoolId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "学校不存在"));

        AttachmentRelation file = attachmentRelationRepository.findById(fileId)
                .orElseThrow(() -> new BusinessException(ResultCode.DATA_NOT_EXIST, "导入文件不存在"));
        if (file.getFilePath() == null || file.getFilePath().isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "导入文件未上传到存储");
        }

        String ext = extractExtension(file.getOriginalName());
        if (!"xlsx".equals(ext) && !"csv".equals(ext)) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR, "不支持的文件格式，仅支持 .xlsx / .csv");
        }

        byte[] bytes = readOssBytes(file.getFilePath());
        List<GradeImportFileParser.ParsedRow> rows = fileParser.parse(bytes, ext, USER_COLUMNS, true);

        Long studentRoleId = roleRepository.findByCode(ROLE_CODE_STUDENT)
                .map(Role::getId)
                .orElseThrow(() -> new BusinessException(ResultCode.SYS_ERROR, "学生角色未配置"));

        // 班级名称 → classId 列表（同名多个班级判为不唯一）
        Map<String, List<Long>> classIdsByName = buildClassIdsByName(schoolId);

        List<UserImportFailItem> failures = new ArrayList<>();
        int success = 0;
        for (GradeImportFileParser.ParsedRow row : rows) {
            String userNo = row.getValues().get("userNo");
            String name = row.getValues().get("name");
            String className = row.getValues().get("className");
            try {
                if (userNo == null || userNo.isBlank()) {
                    throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "学号为空");
                }
                if (name == null || name.isBlank()) {
                    throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "姓名为空");
                }
                if (className == null || className.isBlank()) {
                    throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "班级为空");
                }
                List<Long> classIds = classIdsByName.get(className.trim());
                if (classIds == null || classIds.isEmpty()) {
                    throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "班级不存在: " + className.trim());
                }
                if (classIds.size() > 1) {
                    throw new BusinessException(ResultCode.DATA_VALIDATION_FAILED, "班级名称不唯一，请使用更精确的班级名: " + className.trim());
                }

                // 复用创建用户逻辑（默认密码 123456、角色 student、student_profiles.class_id 落库）
                userManageService.createUser(operatorId, CreateUserRequest.builder()
                        .userNo(userNo.trim())
                        .name(name.trim())
                        .schoolId(schoolId)
                        .roleIds(List.of(studentRoleId))
                        .classId(classIds.get(0))
                        .build());
                success++;
            } catch (BusinessException be) {
                failures.add(new UserImportFailItem(row.getRowNumber(), userNo, be.getMessage()));
            }
        }

        return UserImportResponse.builder()
                .totalCount(rows.size())
                .successCount(success)
                .failCount(failures.size())
                .failures(failures)
                .build();
    }

    // ==================== 班级解析辅助 ====================

    /**
     * 解析该学校下「班级名称 → classId 列表」映射。
     * 下钻：schoolId → colleges → majors → classes，同名班级可能对应多个 classId。
     */
    private Map<String, List<Long>> buildClassIdsByName(Long schoolId) {
        List<Long> collegeIds = collegeRepository.findBySchoolId(schoolId).stream()
                .map(College::getId).collect(Collectors.toList());
        if (collegeIds.isEmpty()) {
            return Map.of();
        }
        List<Long> majorIds = majorRepository.findByCollegeIdIn(collegeIds).stream()
                .map(Major::getId).collect(Collectors.toList());
        if (majorIds.isEmpty()) {
            return Map.of();
        }
        return clazzRepository.findByMajorIdIn(majorIds).stream()
                .filter(c -> c.getName() != null)
                .collect(Collectors.groupingBy(Clazz::getName,
                        Collectors.mapping(Clazz::getId, Collectors.toList())));
    }

    // ==================== 文件读取辅助 ====================

    /** 读取 OSS 对象字节内容 */
    private byte[] readOssBytes(String objectKey) {
        OSSObject object = ossClient.getObject(ossProperties.getBucketName(), objectKey);
        try (InputStream in = object.getObjectContent()) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new BusinessException(ResultCode.THIRD_OSS_FAILED, "导入文件读取失败");
        }
    }

    /** 提取小写扩展名（不含点） */
    private String extractExtension(String filename) {
        if (filename == null) {
            return null;
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return null;
        }
        return filename.substring(dot + 1).toLowerCase();
    }

    // ==================== 内嵌 POJO ====================

    /** 批量导入请求 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserImportRequest {
        private Long schoolId;
        private Long fileId;
    }

    /** 批量导入响应 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserImportResponse {
        private Integer totalCount;
        private Integer successCount;
        private Integer failCount;
        private List<UserImportFailItem> failures;
    }

    /** 导入失败明细项 */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserImportFailItem {
        private Integer row;
        private String userNo;
        private String reason;
    }
}
