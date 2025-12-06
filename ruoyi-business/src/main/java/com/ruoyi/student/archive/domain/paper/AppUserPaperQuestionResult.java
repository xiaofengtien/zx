package com.ruoyi.student.archive.domain.paper;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户试卷题目结果
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_user_paper_question_result")
public class AppUserPaperQuestionResult extends BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Integer id;

    /**
     * 业务类型（保留用于兼容）
     */
    private Integer businessType;

    /**
     * 业务id（保留用于兼容）
     */
    private Integer businessId;

    /**
     * 试卷id
     */
    private Integer paperId;

    /**
     * 用户id
     */
    private Integer appUserId;

    /**
     * 问题id
     */
    private Integer questionId;

    /**
     * 用户选择的答案ID（多个英文逗号隔开）
     */
    private String answerIds;

    /**
     * 用户答案
     */
    private String userAnswer;

    /**
     * 结果 1 正确 0 错误
     */
    private Integer result;

    /**
     * 题目排序
     */
    private Integer questionSort;

    /**
     * 答题时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date answerTime;

    /**
     * 用时（秒）
     */
    private Integer timeSpent;

    /**
     * 是否回顾过：1-是，0-否
     */
    private Integer isReviewed;

    /**
     * 同步状态：0-未同步，1-已同步
     */
    private Integer syncStatus;
} 