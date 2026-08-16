package com.example.studentarchives.service.Fmy;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.ResponseHeaderOverrides;
import com.example.studentarchives.config.Fmy.OssProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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
     * 上传字节内容到 OSS（正式目录），用于导出生成的 PDF 等非 MultipartFile 场景。
     * <p>
     * 路径格式：{@code {bizType}/{category}/{yyyy-MM}/{uuid}.{ext}}
     *
     * @param data            文件字节内容
     * @param contentType     MIME 类型，如 application/pdf
     * @param bizType         业务类型，如 student_archive
     * @param category        文件分类，如 pdf
     * @param originalFilename 原始文件名（用于提取扩展名）
     * @return OSS 对象路径（相对路径）
     */
    public String uploadBytes(byte[] data, String contentType, String bizType, String category, String originalFilename) {
        String objectKey = buildObjectKey(bizType, category, originalFilename);
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(data.length);
        metadata.setContentType(contentType != null ? contentType : "application/octet-stream");
        metadata.setContentDisposition(buildContentDisposition(originalFilename));
        try (InputStream inputStream = new java.io.ByteArrayInputStream(data)) {
            ossClient.putObject(ossProperties.getBucketName(), objectKey, inputStream, metadata);
            log.info("OSS 字节上传成功: {}, size={}, type={}", objectKey, data.length, contentType);
            return objectKey;
        } catch (Exception e) {
            log.error("OSS 字节上传失败: bucket={}, objectKey={}, error={}",
                    ossProperties.getBucketName(), objectKey, e.getMessage(), e);
            throw new RuntimeException("OSS上传失败: " + e.getMessage(), e);
        }
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
     * 上传导出模板预览图（直接存正式目录，不走 temp 临时目录）
     * <p>
     * 路径格式：export-template-preview/{uuid}.{ext}
     *
     * @param file 上传的预览图文件
     * @return OSS 对象路径
     */
    public String uploadTemplatePreview(MultipartFile file) {
        String ext = extractExtension(file.getOriginalFilename());
        String objectKey = "export-template-preview/" + UUID.randomUUID().toString().replace("-", "")
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
        return generatePresignedUrl(objectKey, ossProperties.getUrlExpireMinutes(), null);
    }

    /**
     * 获取文件访问 URL，并指定下载时显示的文件名（通过 response-content-disposition）。
     *
     * @param objectKey     对象路径
     * @param filename      下载文件名
     * @return 临时访问 URL，文件不存在时返回 null
     */
    public String getFileUrl(String objectKey, String filename) {
        if (objectKey == null || !exists(objectKey)) return null;
        return generatePresignedUrl(objectKey, ossProperties.getUrlExpireMinutes(), filename);
    }

    /**
     * 获取文件在线预览 URL（response-content-disposition 覆盖为 inline）。
     * <p>
     * OSS 对象上传时元数据携带 attachment（见 {@link #uploadBytes}），不覆盖直接访问会
     * 触发浏览器下载；预览需显式覆盖为 inline 才能内嵌渲染 PDF/图片。
     * <p>
     * 注意：OSS 默认域名安全策略对新建 Bucket 强制返回 {@code Content-Disposition: attachment}
     * （见 {@code x-oss-force-download}），此 Bucket 当前使用默认域名，inline 覆盖会被 OSS
     * 归一化为裸 {@code attachment} 且丢弃文件名；因此业务侧在线预览当前应改用
     * {@link #getFileUrl(String, String)} 携带文件名（下载命名正确），待接入自定义域名后再使用
     * 本方法的 inline 语义实现真正的内嵌渲染。
     *
     * @param objectKey 对象路径
     * @return 临时预览 URL，文件不存在时返回 null
     */
    public String getPreviewUrl(String objectKey) {
        if (objectKey == null || !exists(objectKey)) return null;
        return generatePresignedUrl(objectKey, ossProperties.getUrlExpireMinutes(), "inline", null);
    }

    /**
     * 获取文件在线预览 URL，并指定预览/另存为时显示的文件名（通过 response-content-disposition）。
     * <p>
     * 与 {@link #getPreviewUrl(String)} 相比额外携带文件名，浏览器内嵌预览时“另存为”仍使用正确中文名。
     *
     * @param objectKey 对象路径
     * @param filename  预览文件名
     * @return 临时预览 URL，文件不存在时返回 null
     */
    public String getPreviewUrl(String objectKey, String filename) {
        if (objectKey == null || !exists(objectKey)) return null;
        return generatePresignedUrl(objectKey, ossProperties.getUrlExpireMinutes(), "inline", filename);
    }

    /**
     * 生成临时签名 URL
     *
     * @param objectKey      对象路径
     * @param expireMinutes  过期时间（分钟）
     * @return 签名 URL
     */
    public String generatePresignedUrl(String objectKey, int expireMinutes) {
        return generatePresignedUrl(objectKey, expireMinutes, null);
    }

    /**
     * 生成临时签名 URL，可覆盖下载文件名（Content-Disposition: attachment）。
     *
     * @param objectKey      对象路径
     * @param expireMinutes  过期时间（分钟）
     * @param filename       下载文件名（null 则不覆盖）
     * @return 签名 URL
     */
    public String generatePresignedUrl(String objectKey, int expireMinutes, String filename) {
        String disposition = (filename != null && !filename.isBlank()) ? "attachment" : null;
        return generatePresignedUrl(objectKey, expireMinutes, disposition, filename);
    }

    /**
     * 生成临时签名 URL，可覆盖响应 Content-Disposition。
     *
     * @param objectKey      对象路径
     * @param expireMinutes  过期时间（分钟）
     * @param disposition    响应 Content-Disposition：inline（在线预览）/ attachment（下载）/ null（不覆盖）
     * @param filename       下载文件名（attachment 时生效），null 不携带
     * @return 签名 URL
     */
    public String generatePresignedUrl(String objectKey, int expireMinutes, String disposition, String filename) {
        Date expiration = new Date(System.currentTimeMillis() + expireMinutes * 60 * 1000L);
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                ossProperties.getBucketName(), objectKey, com.aliyun.oss.HttpMethod.GET);
        request.setExpiration(expiration);
        if (disposition != null && !disposition.isBlank()) {
            ResponseHeaderOverrides overrides = new ResponseHeaderOverrides();
            boolean inline = "inline".equalsIgnoreCase(disposition);
            // inline 也携带 filename：预览时浏览器保存/另存为仍用正确中文名
            overrides.setContentDisposition(buildContentDisposition(inline ? "inline" : "attachment", filename));
            request.setResponseHeaders(overrides);
        }
        URL url = ossClient.generatePresignedUrl(request);
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

    /**
     * 构建上传到 OSS 对象的 Content-Disposition 元数据（存储态）。
     * <p>
     * 存储态头值保持纯 ASCII：HTTP 上传请求头按 ISO-8859-1 传输，原始 UTF-8 会被服务端
     * 重编码损坏，故 {@code filename} 统一放百分号编码后的 UTF-8（纯 ASCII、RFC 3986），
     * 中文名另走 {@code filename*=UTF-8''} 通道。访问时通常再用 response-content-disposition
     * 覆盖（见 {@link #buildContentDisposition(String, String)}），存储态仅作无覆盖兜底。
     */
    private static String buildContentDisposition(String filename) {
        return buildContentDisposition("attachment", filename);
    }

    /**
     * 构建响应 Content-Disposition 头值（response-content-disposition 覆盖，在线预览/下载通用），
     * 兼容 RFC 6266 + RFC 5987（中文文件名）。
     * <p>
     * 双通道均携带百分号编码后的 UTF-8 名（纯 ASCII，RFC 3986）：
     * - {@code filename="%E8%81%8C..."}：RFC 6266 规定该参数只能放 ASCII，原始 UTF-8 会被
     *   HTTP 头按 ISO-8859-1 传输、客户端按 Latin-1 解读产生乱码（实测：Apifox 下载名变成
     *   {@code è_ä_è§åæä»¶.pdf}）；改放百分号编码后可被 Firefox/Chrome 等直接解码为中文；
     * - {@code filename*=UTF-8''...}：RFC 5987 标准通道，规范客户端（含 Safari）优先采用，
     *   解码得到正确中文名。
     * <p>
     * 编码串中不含原始引号/反斜杠（均被 {@code %22/%5C} 替代），不会破坏 quoted-string 语法。
     *
     * @param mode     inline（在线预览）/ attachment（下载）
     * @param filename 文件名（含扩展名），null 时仅返回 mode
     * @return Content-Disposition 头值
     */
    private static String buildContentDisposition(String mode, String filename) {
        if (filename == null || filename.isBlank()) {
            return mode;
        }
        String encoded;
        try {
            encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
        } catch (UnsupportedEncodingException e) {
            encoded = filename;
        }
        return mode + "; filename=\"" + encoded + "\"; filename*=UTF-8''" + encoded;
    }
}
