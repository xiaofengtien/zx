package com.ruoyi.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 七牛云OSS配置类
 * 
 * @author ruoyi
 */
@Data
@Component
@ConfigurationProperties(prefix = "qiniu.oss")
public class QiniuOssConfig {
    
    /**
     * 访问密钥（Access Key）
     */
    private String accessKey;
    
    /**
     * 密钥（Secret Key）
     */
    private String secretKey;
    
    /**
     * 存储空间名称（Bucket）
     */
    private String bucketName;
    
    /**
     * 访问域名
     */
    private String domain;
    
    /**
     * 文件访问URL前缀
     */
    private String urlPrefix;
    
    /**
     * CDN域名
     */
    private String cdn;
}



