package com.zx.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 试卷题目列表查询参数
 * 
 * @author zx
 */
@Data
public class PaperQuestionListBO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 试卷ID
     */
    @NotNull(message = "试卷ID不能为空")
    private Integer paperId;

    /**
     * 题目ID（可选，用于查询特定题目）
     */
    private Integer questionId;
}

