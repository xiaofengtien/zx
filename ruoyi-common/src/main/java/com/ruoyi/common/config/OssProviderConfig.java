package com.ruoyi.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * OSS提供商配置类
 * 
 * @author ruoyi
 */
@Data
@Component
@ConfigurationProperties(prefix = "oss")
public class OssProviderConfig {
    
    /**
     * OSS提供商（qiniu-七牛云，aliyun-阿里云）
     * 默认：qiniu
     */
    private String provider = "qiniu";
}



