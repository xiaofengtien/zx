package com.zx.student.archive.domain.paper;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zx.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 用户完形填空答题结果
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_user_paper_question_blank_result")
public class AppUserPaperQuestionBlankResult extends BaseEntity implements Serializable {

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
     * 完形填空区域ID
     */
    private Integer blankAreaId;
    
    /**
     * 空位序号
     */
    private Integer blankIndex;

    /**
     * 用户选择的答案ID（多个英文逗号隔开）
     */
    private String answerIds;

    /**
     * 结果 1 正确 0 错误
     */
    private Integer result;

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
     * 同步状态：0-未同步，1-已同步
     */
    private Integer syncStatus;
} 