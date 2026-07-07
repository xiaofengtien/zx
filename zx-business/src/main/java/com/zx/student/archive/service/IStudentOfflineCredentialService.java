package com.zx.student.archive.service;

import com.zx.student.archive.domain.dto.OfflineCredential;

/**
 * 学员离线凭证Service接口
 *
 * @author zx
 */
public interface IStudentOfflineCredentialService
{
    /**
     * 生成离线凭证
     *
     * @param studentAccount 学员账号
     * @param passwordHash 密码哈希
     * @param archiveId 学员档案ID
     * @param userId 系统用户ID
     * @param expireDays 有效期（天数）
     * @return 加密后的离线凭证字符串
     */
    String generateOfflineCredential(String studentAccount, String passwordHash, Long archiveId, Long userId, int expireDays);

    /**
     * 验证并解析离线凭证
     *
     * @param credential 离线凭证字符串
     * @return 离线凭证对象，如果验证失败返回null
     */
    OfflineCredential verifyOfflineCredential(String credential);

    /**
     * 验证离线登录（本地验证密码）
     *
     * @param studentAccount 学员账号
     * @param password 密码
     * @param credential 离线凭证
     * @return true-验证成功，false-验证失败
     */
    boolean verifyOfflineLogin(String studentAccount, String password, OfflineCredential credential);
}




