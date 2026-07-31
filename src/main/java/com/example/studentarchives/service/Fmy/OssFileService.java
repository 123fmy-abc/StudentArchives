package com.example.studentarchives.service.Fmy;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.ObjectMetadata;
import com.example.studentarchives.config.Fmy.OssProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.UUID;

/**
 * OSS 文件上传服务
 * <p>
 * 提供文件上传、删除、URL 生成、临时文件迁移等功能。
 * <p>
 * <strong>目录结构：</strong>
 * <pre>
 * student-archives/
 *  ├── temp/{yyyy-MM}/{uuid}.{ext}              ← 临时文件
 *  ├── {bizType}/{category}/{yyyy-MM}/{uuid}.{ext}  ← 正式文件
 *  └── avatar/{uuid}.{ext}                       ← 头像（可选）
 * </pre>
 * <p>
 * <strong>团队共享：</strong>
 * 团队成员在各自 {@code .env} 中配置相同的 OSS 密钥即可共享使用。
 *
 * @author fmy
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OssFileService {

    private final OSS ossClient;
    private final OssProperties ossProperties;

    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 上传文件到 OSS（正式目录）
     * <p>
     * 路径格式：{@code {bizType}/{category}/{yyyy-MM}/{uuid}.{ext}}
     *
     * @param file     上传的文件
     * @param bizType  业务类型，如 archive / award / career_plan / announcement
     * @param category 文件分类，如 certificate / photo / proof / other
     * @return OSS 对象路径（相对路径）
     */
    public String uploadFile(MultipartFile file, String bizType, String category) {
        String objectKey = buildObjectKey(bizType, category, file.getOriginalFilename());
        return doUpload(file, objectKey);
    }

    /**
     * 上传头像（直接存正式目录，不走 temp 临时目录）
     * <p>
     * 路径格式：avatar/{uuid}.{ext}
     *
     * @param file 上传的头像文件
     * @return OSS 对象路径
     */
    public String uploadAvatar(MultipartFile file) {
        String ext = extractExtension(file.getOriginalFilename());
        String objectKey = "avatar/" + UUID.randomUUID().toString().replace("-", "")
                + (ext != null ? "." + ext : "");
        return doUpload(file, objectKey);
    }

    /**
     * 上传临时文件（temp 目录）
     * <p>
     * 临时文件可通过 {@link #moveToPermanent} 迁移到正式目录。
     *
     * @param file 上传的文件
     * @return OSS 对象路径（相对路径）
     */
    public String uploadTempFile(MultipartFile file) {
        String objectKey = buildTempObjectKey(file.getOriginalFilename());
        return doUpload(file, objectKey);
    }

    /**
     * 将临时文件迁移到正式目录（拷贝后删除临时文件）
     *
     * @param tempPath 临时文件路径
     * @param bizType  业务类型
     * @param category 文件分类
     * @return 正式文件路径
     */
    public String moveToPermanent(String tempPath, String bizType, String category) {
        String ext = extractExtension(tempPath);
        String destKey = bizType + "/" + category + "/"
                + LocalDate.now().format(MONTH_FORMAT) + "/"
                + UUID.randomUUID().toString().replace("-", "")
                + (ext != null ? "." + ext : "");

        ossClient.copyObject(ossProperties.getBucketName(), tempPath,
                ossProperties.getBucketName(), destKey);
        ossClient.deleteObject(ossProperties.getBucketName(), tempPath);

        log.info("OSS 文件迁移: {} -> {}", tempPath, destKey);
        return destKey;
    }

    /**
     * 删除 OSS 文件
     *
     * @param objectKey 对象路径
     */
    public void deleteFile(String objectKey) {
        if (objectKey == null) return;
        ossClient.deleteObject(ossProperties.getBucketName(), objectKey);
        log.info("OSS 文件删除: {}", objectKey);
    }

    /**
     * 判断 OSS 对象是否存在
     *
     * @param objectKey 对象路径
     * @return 是否存在
     */
    public boolean exists(String objectKey) {
        return ossClient.doesObjectExist(ossProperties.getBucketName(), objectKey);
    }

    /**
     * 获取文件访问 URL（默认临时签名 URL，30 分钟有效）
     * <p>
     * 如果 bucket 开启了公共读，可返回不带签名的 URL。
     *
     * @param objectKey 对象路径
     * @return 临时访问 URL，文件不存在时返回 null
     */
    public String getFileUrl(String objectKey) {
        if (objectKey == null || !exists(objectKey)) return null;
        return generatePresignedUrl(objectKey, ossProperties.getUrlExpireMinutes());
    }

    /**
     * 生成临时签名 URL
     *
     * @param objectKey      对象路径
     * @param expireMinutes  过期时间（分钟）
     * @return 签名 URL
     */
    public String generatePresignedUrl(String objectKey, int expireMinutes) {
        Date expiration = new Date(System.currentTimeMillis() + expireMinutes * 60 * 1000L);
        URL url = ossClient.generatePresignedUrl(ossProperties.getBucketName(), objectKey, expiration);
        return url.toString();
    }

    // ==================== 私有方法 ====================

    /**
     * 构建正式文件 OSS 路径: {bizType}/{category}/{yyyy-MM}/{uuid}.{ext}
     */
    private String buildObjectKey(String bizType, String category, String originalFilename) {
        return bizType + "/" + category + "/"
                + LocalDate.now().format(MONTH_FORMAT) + "/"
                + UUID.randomUUID().toString().replace("-", "")
                + getExtWithDot(originalFilename);
    }

    /**
     * 构建临时文件路径: temp/{yyyy-MM}/{uuid}.{ext}
     */
    private String buildTempObjectKey(String originalFilename) {
        return "temp/" + LocalDate.now().format(MONTH_FORMAT) + "/"
                + UUID.randomUUID().toString().replace("-", "")
                + getExtWithDot(originalFilename);
    }

    /**
     * 执行文件上传
     */
    private String doUpload(MultipartFile file, String objectKey) {
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            metadata.setContentType(file.getContentType());

            ossClient.putObject(ossProperties.getBucketName(), objectKey, inputStream, metadata);
            log.info("OSS 文件上传成功: {}, size={}, type={}",
                    objectKey, file.getSize(), file.getContentType());
            return objectKey;
        } catch (Exception e) {
            log.error("OSS 文件上传失败: bucket={}, objectKey={}, endpoint={}, error={}",
                    ossProperties.getBucketName(), objectKey, ossProperties.getEndpoint(), e.getMessage(), e);
            throw new RuntimeException("OSS上传失败: " + e.getMessage(), e);
        }
    }

    /**
     * 提取扩展名（含点号），无扩展名时返回空字符串
     */
    private static String getExtWithDot(String filename) {
        if (filename == null || filename.isEmpty()) return "";
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex >= 0 && dotIndex < filename.length() - 1)
                ? filename.substring(dotIndex)
                : "";
    }

    /**
     * 提取扩展名（不含点号）
     */
    private static String extractExtension(String filename) {
        if (filename == null || filename.isEmpty()) return null;
        int dotIndex = filename.lastIndexOf('.');
        return (dotIndex >= 0 && dotIndex < filename.length() - 1)
                ? filename.substring(dotIndex + 1)
                : null;
    }
}
