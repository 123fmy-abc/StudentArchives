package com.example.studentarchives.config.Fmy;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OSS 客户端配置
 * <p>
 * 创建阿里云 OSS 客户端 Bean，供 {@link com.example.studentarchives.service.Fmy.OssFileService} 使用。
 * 团队其他成员只需在 {@code .env} 中配置相同的 OSS 密钥即可共享使用同一 OSS。
 *
 * @author fmy
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class OssConfig {

    private final OssProperties ossProperties;

    @Bean
    public OSS ossClient() {
        log.info("初始化 OSS 客户端: endpoint={}, bucket={}, region={}",
                ossProperties.getEndpoint(), ossProperties.getBucketName(), ossProperties.getRegion());
        // 阿里云 OSS 已停用 V1 签名，必须显式启用 V4 签名（SignVersion.V4 + 地域 ID）
        ClientBuilderConfiguration configuration = new ClientBuilderConfiguration();
        configuration.setSignatureVersion(SignVersion.V4);
        return OSSClientBuilder.create()
                .endpoint(ossProperties.getEndpoint())
                .credentialsProvider(new DefaultCredentialProvider(ossProperties.getAccessKeyId(), ossProperties.getAccessKeySecret()))
                .clientConfiguration(configuration)
                .region(ossProperties.getRegion())
                .build();
    }
}
