package com.zx.student.archive.domain.dto;

import java.io.Serializable;
import java.util.Date;

/**
 * 离线凭证对象
 * 用于桌面应用离线登录
 *
 * @author zx
 */
public class OfflineCredential implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 学员账号 */
    private String studentAccount;

    /** 加密的密码哈希 */
    private String passwordHash;

    /** 凭证有效期（时间戳） */
    private Long expireTime;

    /** 学员档案ID */
    private Long archiveId;

    /** 系统用户ID */
    private Long userId;

    /** 凭证生成时间 */
    private Date createTime;

    public String getStudentAccount()
    {
        return studentAccount;
    }

    public void setStudentAccount(String studentAccount)
    {
        this.studentAccount = studentAccount;
    }

    public String getPasswordHash()
    {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash)
    {
        this.passwordHash = passwordHash;
    }

    public Long getExpireTime()
    {
        return expireTime;
    }

    public void setExpireTime(Long expireTime)
    {
        this.expireTime = expireTime;
    }

    public Long getArchiveId()
    {
        return archiveId;
    }

    public void setArchiveId(Long archiveId)
    {
        this.archiveId = archiveId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    /**
     * 检查凭证是否过期
     *
     * @return true-已过期，false-未过期
     */
    public boolean isExpired()
    {
        if (expireTime == null)
        {
            return true;
        }
        return System.currentTimeMillis() > expireTime;
    }
}




