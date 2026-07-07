package com.zx.student.archive.domain.dto;

/**
 * 重置密码参数对象
 *
 * @author zx
 */
public class ResetPasswordDTO
{
    /** 学员档案ID */
    private Long archiveId;

    /** 新密码（可选，如果为空则使用默认密码123456） */
    private String newPassword;

    public Long getArchiveId()
    {
        return archiveId;
    }

    public void setArchiveId(Long archiveId)
    {
        this.archiveId = archiveId;
    }

    public String getNewPassword()
    {
        return newPassword;
    }

    public void setNewPassword(String newPassword)
    {
        this.newPassword = newPassword;
    }
}




