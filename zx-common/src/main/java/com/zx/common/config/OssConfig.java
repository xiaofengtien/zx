package com.zx.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 阿里云OSS配置类
 * 
 * @author zx
 */
@Data
@Component
@ConfigurationProperties(prefix = "aliyun.oss")
public class OssConfig {

    /**
     * OSS endpoint
     */
    private String endpoint;

    /**
     * 访问密钥ID
     */
    private String accessKeyId;

    /**
     * 访问密钥Secret
     */
    private String accessKeySecret;

    /**
     * 存储空间名称
     */
    private String bucketName;

    /**
     * 文件访问URL前缀
     */
    private String urlPrefix;

    /**
     * CDN域名
     */
    private String cdn;
}
