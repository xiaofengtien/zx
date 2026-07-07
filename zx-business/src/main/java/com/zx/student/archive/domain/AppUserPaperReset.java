package com.zx.student.archive.domain;

import com.zx.common.annotation.Excel;
import com.zx.common.core.domain.BaseEntity;
import java.util.Date;

/**
 * 用户试卷练习次数重置对象 app_user_paper_reset
 */
public class AppUserPaperReset extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    private Long id;

    /** 用户ID */
    @Excel(name = "用户ID")
    private Long userId;

    /** 试卷ID（为空表示重置该用户所有试卷） */
    @Excel(name = "试卷ID")
    private Long paperId;

    /** 重置时间 */
    @Excel(name = "重置时间", dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date resetTime;

    /** 操作人 */
    @Excel(name = "操作人")
    private String resetBy;

    /** 学员档案ID（用于查询userId） */
    private Long archiveId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getArchiveId() { return archiveId; }
    public void setArchiveId(Long archiveId) { this.archiveId = archiveId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getPaperId() { return paperId; }
    public void setPaperId(Long paperId) { this.paperId = paperId; }

    public Date getResetTime() { return resetTime; }
    public void setResetTime(Date resetTime) { this.resetTime = resetTime; }

    public String getResetBy() { return resetBy; }
    public void setResetBy(String resetBy) { this.resetBy = resetBy; }
}
