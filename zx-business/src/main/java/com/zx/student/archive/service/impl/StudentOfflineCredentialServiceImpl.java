package com.zx.student.archive.service.impl;

import java.util.Date;
import com.alibaba.fastjson2.JSON;
import com.zx.common.utils.StringUtils;
import com.zx.common.utils.SecurityUtils;
import com.zx.student.archive.domain.dto.OfflineCredential;
import com.zx.student.archive.service.IStudentOfflineCredentialService;
import org.springframework.stereotype.Service;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * 学员离线凭证Service实现
 *
 * @author zx
 */
@Service
public class StudentOfflineCredentialServiceImpl implements IStudentOfflineCredentialService
{
    /**
     * 加密密钥（实际项目中应该从配置文件读取）
     * TODO: 请将密钥配置到application.yml中
     * AES-128需要16字节密钥，AES-256需要32字节密钥
     */
    private static final String SECRET_KEY = "ZxStudent2024"; // 16字节密钥

    /**
     * 默认有效期（天）
     */
    private static final int DEFAULT_EXPIRE_DAYS = 30;

    @Override
    public String generateOfflineCredential(String studentAccount, String passwordHash, Long archiveId, Long userId, int expireDays)
    {
        if (StringUtils.isEmpty(studentAccount) || StringUtils.isEmpty(passwordHash))
        {
            throw new IllegalArgumentException("学员账号和密码哈希不能为空");
        }

        OfflineCredential credential = new OfflineCredential();
        credential.setStudentAccount(studentAccount);
        credential.setPasswordHash(passwordHash);
        credential.setArchiveId(archiveId);
        credential.setUserId(userId);
        credential.setCreateTime(new Date());
        
        // 计算过期时间（当前时间 + 有效期天数）
        long expireTime = System.currentTimeMillis() + (expireDays > 0 ? expireDays : DEFAULT_EXPIRE_DAYS) * 24L * 60L * 60L * 1000L;
        credential.setExpireTime(expireTime);

        // 将凭证对象序列化为JSON
        String json = JSON.toJSONString(credential);
        
        // 加密JSON字符串
        try
        {
            return encrypt(json);
        }
        catch (Exception e)
        {
            throw new RuntimeException("生成离线凭证失败", e);
        }
    }

    @Override
    public OfflineCredential verifyOfflineCredential(String credential)
    {
        if (StringUtils.isEmpty(credential))
        {
            return null;
        }

        try
        {
            // 解密凭证
            String json = decrypt(credential);
            
            // 反序列化为对象
            OfflineCredential offlineCredential = JSON.parseObject(json, OfflineCredential.class);
            
            // 检查是否过期
            if (offlineCredential.isExpired())
            {
                return null;
            }

            return offlineCredential;
        }
        catch (Exception e)
        {
            return null;
        }
    }

    @Override
    public boolean verifyOfflineLogin(String studentAccount, String password, OfflineCredential credential)
    {
        if (credential == null || StringUtils.isEmpty(studentAccount) || StringUtils.isEmpty(password))
        {
            return false;
        }

        // 验证账号是否匹配
        if (!studentAccount.equals(credential.getStudentAccount()))
        {
            return false;
        }

        // 验证密码（使用凭证中的密码哈希）
        return SecurityUtils.matchesPassword(password, credential.getPasswordHash());
    }

    /**
     * 加密字符串（使用AES算法）
     */
    private String encrypt(String plainText) throws Exception
    {
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }

    /**
     * 解密字符串（使用AES算法）
     */
    private String decrypt(String encryptedText) throws Exception
    {
        SecretKeySpec secretKey = new SecretKeySpec(SECRET_KEY.getBytes(StandardCharsets.UTF_8), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}

