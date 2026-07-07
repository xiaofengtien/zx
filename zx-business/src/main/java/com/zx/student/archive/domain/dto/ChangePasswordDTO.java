package com.zx.student.archive.domain.dto;

/**
 * 修改密码参数对象
 *
 * @author zx
 */
public class ChangePasswordDTO
{
    /** 学员档案ID */
    private Long archiveId;

    /** 旧密码 */
    private String oldPassword;

    /** 新密码 */
    private String newPassword;

    public Long getArchiveId()
    {
        return archiveId;
    }

    public void setArchiveId(Long archiveId)
    {
        this.archiveId = archiveId;
    }

    public String getOldPassword()
    {
        return oldPassword;
    }

    public void setOldPassword(String oldPassword)
    {
        this.oldPassword = oldPassword;
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




