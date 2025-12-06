package com.ruoyi.student.archive.domain.paper;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 用户试卷信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_user_paper_info")
public class AppUserPaperInfo extends BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Integer id;

    /**
     * 业务类型
     * 业务类型 1内容管理-音频专辑 2内容管理-视频专辑 3图书管理-图书资源 4图书管理-题库 5图书管理-音频专辑 6图书管理-视频专辑
     */
    private Integer businessType;

    /**
     * 业务id
     */
    private Integer businessId;

    /**
     * 试卷ID（关联paper表）
     */
    private Integer paperId;

    /**
     * 试卷名称
     */
    private String paperName;

    /**
     * 用户id(学员档案ID)
     */
    private Integer appUserId;

    /**
     * 开始答题时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;

    /**
     * 提交时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date submitTime;

    /**
     * 用时（秒）
     */
    private Integer usedTime;

    /**
     * 总分
     */
    private BigDecimal totalScore;

    /**
     * 得分
     */
    private BigDecimal userScore;

    /**
     * 已练习次数
     */
    private Integer practiceCount;

    /**
     * 最后练习时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date lastPracticeTime;

    /**
     * 各卷状态（JSON对象，如：{"A":"completed","B":"in_progress"}）
     */
    private String volumeStatus;

    /**
     * 各卷提交时间（JSON对象，如：{"A":"2024-01-01 10:00:00","B":null}）
     */
    private String volumeSubmitTime;

    /**
     * 中场音频播放状态（JSON对象，如：{"A->B":true}）
     */
    private String intermissionPlayed;

    /**
     * 分配的机位号（后台配置）
     */
    private String assignedSeatNumber;

    /**
     * 实际坐的机位号（可能发生变化，暂时不处理）
     */
    private String actualSeatNumber;

    /**
     * 正确题数
     */
    private Integer correctCount;

    /**
     * 错误题数
     */
    private Integer wrongCount;

    /**
     * 是否提交 0 否 1是
     */
    private Integer isSubmit;
    
    /**
     * 题目顺序，逗号分隔的题目ID（保留用于兼容，新逻辑通过paper_question表查询）
     */
    private String questionOrder;

    /**
     * 是否启用 1-是 0-否
     */
    private Integer status;

    /**
     * 同步状态：0-未同步，1-已同步，2-同步失败
     */
    private Integer syncStatus;

    /**
     * 同步时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date syncTime;
} 