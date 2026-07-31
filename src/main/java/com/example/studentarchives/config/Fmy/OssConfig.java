package com.example.studentarchives.config.Fmy;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
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
        log.info("初始化 OSS 客户端: endpoint={}, bucket={}",
                ossProperties.getEndpoint(), ossProperties.getBucketName());
        return new OSSClientBuilder().build(
                ossProperties.getEndpoint(),
                ossProperties.getAccessKeyId(),
                ossProperties.getAccessKeySecret()
        );
    }
}
