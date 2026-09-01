package com.example.studentarchives.service.Fmy;

import com.example.studentarchives.common.ResultCode;
import com.example.studentarchives.dto.Fmy.common.response.CoverUploadResponse;
import com.example.studentarchives.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 封面上传服务（POST /common/upload/cover）
 * <p>
 * 与通用文件上传（POST /common/upload）不同，封面上传**不走临时存储流程**：
 * 直接上传至 OSS 正式目录 {@code cover/photo/{yyyy-MM}/{uuid}.{ext}}，返回
 * 可直接回填业务 {@code coverImage} 字段的 URL（30 天签名有效）。
 * 不写 {@code attachment_relations}（file_uploads）表——成长时间轴等业务以裸 URL 存封面。
 * <p>
 * 复用于成长时间轴（4.2）等模块的封面场景：前端先上传拿到 {@code coverUrl}，
 * 再作为 {@code coverImage} 传入新增 / 修改接口。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CoverUploadService {

    /** 封面 OSS 路径业务类型前缀 */
    private static final String BIZ_TYPE_COVER = "cover";

    /** 封面文件分类 */
    private static final String CATEGORY_PHOTO = "photo";

    /** 封面 URL 有效期（分钟）：30 天，对齐头像 / 模板预览图 */
    private static final int COVER_URL_EXPIRE_MINUTES = 30 * 24 * 60;

    /** 封面文件大小上限（5MB） */
    private static final long MAX_COVER_SIZE = 5 * 1024 * 1024L;

    /** 允许的图片扩展名 */
    private static final Set<String> ALLOWED_IMAGE_EXTS = Set.of("jpg", "jpeg", "png", "gif", "webp", "bmp");

    private final OssFileService ossFileService;

    /**
     * 上传封面图片（直接存 OSS 正式目录，不走 temp）
     *
     * @param file   封面文件（仅支持 jpg/jpeg/png/gif/webp/bmp，最大 5MB）
     * @param userId 当前登录用户 ID（写审计，不落 attachment_relations）
     * @return 封面 URL 与 OSS 对象路径
     */
    public CoverUploadResponse uploadCover(MultipartFile file, Long userId) {
        // 1. 校验文件是否为空
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "上传文件不能为空");
        }

        // 2. 校验文件扩展名（仅允许图片格式）
        String originalFilename = file.getOriginalFilename();
        String ext = extractExtension(originalFilename);
        if (ext == null) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR, "文件缺少扩展名");
        }
        if (!ALLOWED_IMAGE_EXTS.contains(ext)) {
            throw new BusinessException(ResultCode.FILE_FORMAT_ERROR,
                    "仅支持 jpg/png/gif/webp/bmp 格式的图片，当前文件格式: " + ext);
        }

        // 3. 校验文件大小（封面限制 5MB）
        if (file.getSize() > MAX_COVER_SIZE) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE,
                    "封面文件过大，最大允许 5MB，当前文件大小: " + (file.getSize() / 1024) + "KB");
        }

        // 4. 上传到 OSS 正式目录 cover/photo/{yyyy-MM}/{uuid}.{ext}
        String objectKey;
        try {
            objectKey = ossFileService.uploadFile(file, BIZ_TYPE_COVER, CATEGORY_PHOTO);
        } catch (Exception e) {
            log.error("OSS 封面上传失败: userId={}, filename={}, size={}, error={}",
                    userId, originalFilename, file.getSize(), e.getMessage(), e);
            throw new BusinessException(ResultCode.FILE_UPLOAD_FAILED,
                    "封面上传失败: " + (e.getMessage() != null ? e.getMessage() : "未知错误"));
        }

        // 5. 生成封面访问 URL（30 天有效，生产环境建议 cover 目录开启公共读后改用不带签名的 URL）
        String coverUrl = ossFileService.generatePresignedUrl(objectKey, COVER_URL_EXPIRE_MINUTES);

        log.info("封面上传成功: userId={}, objectKey={}, size={}", userId, objectKey, file.getSize());
        return CoverUploadResponse.builder()
                .coverUrl(coverUrl)
                .objectKey(objectKey)
                .build();
    }

    /** 提取文件扩展名（不含点号） */
    private static String extractExtension(String filename) {
        if (filename == null || filename.isEmpty()) {
            return null;
        }
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex >= 0 && dotIndex < filename.length() - 1)
                ? filename.substring(dotIndex + 1).toLowerCase()
                : null;
    }
}
