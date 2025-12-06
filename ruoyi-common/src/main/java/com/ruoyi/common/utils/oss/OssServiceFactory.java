package com.ruoyi.common.utils.oss;

import com.ruoyi.common.config.OssProviderConfig;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.oss.impl.AliyunOssService;
import com.ruoyi.common.utils.oss.impl.QiniuOssService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * OSS服务工厂类
 * 根据配置文件选择OSS服务提供商
 * 
 * @author ruoyi
 */
@Slf4j
@Component
public class OssServiceFactory {

    @Autowired
    private AliyunOssService aliyunOssService;

    @Autowired
    private QiniuOssService qiniuOssService;

    @Autowired
    private OssProviderConfig ossProviderConfig;

    /**
     * 默认OSS提供商（七牛云）
     */
    private static final String DEFAULT_PROVIDER = "qiniu";

    /**
     * 获取当前使用的OSS服务
     * 
     * @return OSS服务实例
     */
    public IOssService getOssService() {
        String provider = getCurrentProvider();
        
        if ("qiniu".equalsIgnoreCase(provider)) {
            log.debug("使用七牛云OSS服务");
            return qiniuOssService;
        } else if ("aliyun".equalsIgnoreCase(provider)) {
            log.debug("使用阿里云OSS服务");
            return aliyunOssService;
        } else {
            log.warn("未知的OSS提供商: {}，使用默认提供商: {}", provider, DEFAULT_PROVIDER);
            return qiniuOssService;
        }
    }

    /**
     * 获取当前OSS提供商（从配置文件读取）
     * 
     * @return OSS提供商代码（qiniu/aliyun）
     */
    public String getCurrentProvider() {
        try {
            String provider = ossProviderConfig.getProvider();
            
            if (StringUtils.isEmpty(provider)) {
                log.warn("配置文件中未找到OSS提供商，使用默认值: {}", DEFAULT_PROVIDER);
                return DEFAULT_PROVIDER;
            }
            
            // 验证提供商代码是否有效
            if (!"qiniu".equalsIgnoreCase(provider) && !"aliyun".equalsIgnoreCase(provider)) {
                log.warn("无效的OSS提供商代码: {}，使用默认值: {}", provider, DEFAULT_PROVIDER);
                return DEFAULT_PROVIDER;
            }
            
            return provider;
        } catch (Exception e) {
            log.error("获取OSS提供商配置失败，使用默认值: {}", DEFAULT_PROVIDER, e);
            return DEFAULT_PROVIDER;
        }
    }

    /**
     * 初始化时检查配置
     */
    @PostConstruct
    public void init() {
        String provider = getCurrentProvider();
        log.info("OSS服务工厂初始化完成，当前提供商: {}", provider);
    }
}

