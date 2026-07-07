package com.zx.student.archive.domain.dto;

/**
 * 忘记密码参数对象
 *
 * @author zx
 */
public class ForgotPasswordDTO
{
    /** 学员账号 */
    private String studentAccount;

    /** 手机号码 */
    private String phoneNumber;

    /** 验证码 */
    private String code;

    /** 验证码唯一标识 */
    private String uuid;

    /** 新密码 */
    private String newPassword;

    public String getStudentAccount()
    {
        return studentAccount;
    }

    public void setStudentAccount(String studentAccount)
    {
        this.studentAccount = studentAccount;
    }

    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber)
    {
        this.phoneNumber = phoneNumber;
    }

    public String getCode()
    {
        return code;
    }

    public void setCode(String code)
    {
        this.code = code;
    }

    public String getUuid()
    {
        return uuid;
    }

    public void setUuid(String uuid)
    {
        this.uuid = uuid;
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



