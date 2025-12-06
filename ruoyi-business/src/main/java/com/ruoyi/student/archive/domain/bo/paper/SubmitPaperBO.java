package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 提交试卷参数BO
 */
@Data
public class SubmitPaperBO extends BasicPaperBO implements Serializable {

    /**
     * 用户ID
     */
    
    private Integer appUserId;

    /**
     * 评测项ID
     */
    
    private Integer appEvaluationId;

    /**
     * 普通题目作答结果列表
     */
    
    private List<SubmitQuestionResultBO> questionResults;

    /**
     * 完形填空作答结果列表
     */
    
    private List<SubmitBlankResultBO> blankResults;

}


