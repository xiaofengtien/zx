package com.zx.student.archive.domain.bo.question;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 查询题目正确答案请求BO
 */
@Data
public class QueryQuestionsCorrectAnswerBO implements Serializable {
    /**
     * 业务类型 1内容管理-音频专辑 2内容管理-视频专辑 3图书管理-图书资源 4图书管理-题库 5图书管理-音频专辑 6图书管理-视频专辑
     */
    private Integer businessType;

    /**
     * 业务类型ID
     */
    private Integer businessId;

    /**
     * 用户ID
     */
    private Integer appUserId;

    /**
     * 题目ID
     */
    private List<Integer> questionIds;
} 