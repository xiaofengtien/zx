package com.zx.student.archive.domain.question.wrongquestion;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zx.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 用户完形填空错题本
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("app_user_wrong_question_blank")
public class AppUserWrongQuestionBlank extends BaseEntity implements Serializable {

    private Integer id;

    /**
     * 用户id
     */
    private Integer appUserId;

    /**
     * 问题id
     */
    private Integer questionId;

    /**
     * 题目内容（完形填空文章）
     */
    private String questionContent;

    /**
     * 学科ID
     */
    private Integer subjectId;

    /**
     * 学科名称
     */
    private String subjectName;

    /**
     * 来源类型（1:试卷,2:练习,3:题库等）
     */
    private Integer sourceType;

    /**
     * 来源ID
     */
    private Integer sourceId;

    /**
     * 填空题空位序号
     */
    private Integer blankIndex;

    /**
     * 空位区域ID
     */
    private Integer blankAreaId;

    /**
     * 用户错误答案
     */
    private String userAnswer;

    /**
     * 用户选择的答案ID（多个英文逗号隔开）
     */
    private String answerIds;
    /**
     * 唯一标识一次错题事件
     */
    private String stateId;
} 