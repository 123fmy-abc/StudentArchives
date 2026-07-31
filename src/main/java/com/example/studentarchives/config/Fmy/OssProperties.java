package com.example.studentarchives.config.Fmy;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OSS 配置属性
 * <p>
 * 通过 .env 文件注入以下环境变量：
 * <ul>
 *   <li>{@code OSS_ENDPOINT} — OSS 地域节点，如 oss-cn-beijing.aliyuncs.com</li>
 *   <li>{@code OSS_ACCESS_KEY_ID} — 阿里云 AccessKey ID</li>
 *   <li>{@code OSS_ACCESS_KEY_SECRET} — 阿里云 AccessKey Secret</li>
 *   <li>{@code OSS_BUCKET_NAME} — 存储空间名称</li>
 * </ul>
 * <p>
 * 团队成员只需在各自 {@code .env} 中填入相同密钥即可共享使用。
 *
 * @author fmy
 */
@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProperties {

    /** OSS 地域节点，如 oss-cn-beijing.aliyuncs.com */
    private String endpoint;

    /** AccessKey ID */
    private String accessKeyId;

    /** AccessKey Secret */
    private String accessKeySecret;

    /** 存储空间名称 */
    private String bucketName;

    /** 文件上传最大大小（字节），默认 10MB */
    private long maxFileSize = 10485760;

    /** 临时文件过期时间（小时），默认 24 小时 */
    private int tempExpireHours = 24;

    /** 临时访问 URL 过期时间（分钟），默认 30 分钟 */
    private int urlExpireMinutes = 30;
}
