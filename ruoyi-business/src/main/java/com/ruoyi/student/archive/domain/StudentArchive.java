package com.ruoyi.student.archive.domain;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.annotation.Excel;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 学员档案对象 student_archive
 * 
 * @author ruoyi
 */
public class StudentArchive extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 学员档案ID */
    private Long id;

    /** 关联系统用户ID */
    private Long userId;

    /** 学员账号 */
    @Excel(name = "学员账号")
    private String studentAccount;

    /** 学员姓名 */
    @Excel(name = "学员姓名")
    private String studentName;

    /** 密码 */
    private String password;

    /** 手机号码 */
    @Excel(name = "手机号码")
    private String phoneNumber;

    /** 性别（字典编码：sys_user_sex） */
    @Excel(name = "性别", readConverterExp = "0=男,1=女,2=未知")
    private String sex;

    /** 学段（字典编码：grade） */
    @Excel(name = "学段")
    private String grade;

    /** 当前年级（字典编码：根据grade联动） */
    @Excel(name = "当前年级")
    private String currentGrade;

    /** 籍贯 */
    @Excel(name = "籍贯")
    private String hometown;

    /** 考试机位号 */
    @Excel(name = "考试机位")
    private String seatNumber;

    /** 适用考卷（List类型，前端和业务层使用） */
    @Excel(name = "适用考卷")
    private List<String> applicablePapers;

    /** 适用试卷ID列表（List类型，前端和业务层使用） */
    @Excel(name = "适用试卷ID列表")
    private List<Integer> applicablePaperIds;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    /** 删除标志（0代表存在 2代表删除） */
    private String delFlag;

    /** 密码最后更新时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "密码更新时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date pwdUpdateDate;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    @NotBlank(message = "学员账号不能为空")
    @Size(min = 2, max = 30, message = "学员账号长度必须介于 2 和 30 之间")
    public String getStudentAccount()
    {
        return studentAccount;
    }

    public void setStudentAccount(String studentAccount)
    {
        this.studentAccount = studentAccount;
    }

    @NotBlank(message = "学员姓名不能为空")
    @Size(min = 2, max = 100, message = "学员姓名长度必须介于 2 和 100 之间")
    public String getStudentName()
    {
        return studentName;
    }

    public void setStudentName(String studentName)
    {
        this.studentName = studentName;
    }

    // 密码字段在更新时可选（为空或"******"表示不修改密码），新增时如果为空后端会使用默认密码
    // 如果提供了密码，则验证长度
    @Size(min = 6, max = 20, message = "密码长度必须介于 6 和 20 之间")
    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    @NotBlank(message = "手机号码不能为空")
    @Size(min = 11, max = 11, message = "手机号码长度必须为11个字符")
    public String getPhoneNumber()
    {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber)
    {
        this.phoneNumber = phoneNumber;
    }

    public String getSex()
    {
        return sex;
    }

    public void setSex(String sex)
    {
        this.sex = sex;
    }

    public String getGrade()
    {
        return grade;
    }

    public void setGrade(String grade)
    {
        this.grade = grade;
    }

    @NotBlank(message = "当前年级不能为空")
    public String getCurrentGrade()
    {
        return currentGrade;
    }

    public void setCurrentGrade(String currentGrade)
    {
        this.currentGrade = currentGrade;
    }

    @Size(max = 100, message = "籍贯长度不能超过100个字符")
    public String getHometown()
    {
        return hometown;
    }

    public void setHometown(String hometown)
    {
        this.hometown = hometown;
    }

    @Size(max = 50, message = "考试机位号长度不能超过50个字符")
    public String getSeatNumber()
    {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber)
    {
        this.seatNumber = seatNumber;
    }

    public List<String> getApplicablePapers()
    {
        return applicablePapers;
    }

    public void setApplicablePapers(List<String> applicablePapers)
    {
        this.applicablePapers = applicablePapers;
    }

    public List<Integer> getApplicablePaperIds()
    {
        return applicablePaperIds;
    }

    public void setApplicablePaperIds(List<Integer> applicablePaperIds)
    {
        this.applicablePaperIds = applicablePaperIds;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }

    public Date getPwdUpdateDate()
    {
        return pwdUpdateDate;
    }

    public void setPwdUpdateDate(Date pwdUpdateDate)
    {
        this.pwdUpdateDate = pwdUpdateDate;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
            .append("id", getId())
            .append("userId", getUserId())
            .append("studentAccount", getStudentAccount())
            .append("password", getPassword())
            .append("phoneNumber", getPhoneNumber())
            .append("sex", getSex())
                .append("grade", getGrade())
                .append("currentGrade", getCurrentGrade())
                .append("hometown", getHometown())
                .append("seatNumber", getSeatNumber())
                .append("applicablePapers", getApplicablePapers())
            .append("applicablePaperIds", getApplicablePaperIds())
            .append("status", getStatus())
            .append("delFlag", getDelFlag())
            .append("pwdUpdateDate", getPwdUpdateDate())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}

