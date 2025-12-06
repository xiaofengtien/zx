package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 添加题目到试卷参数
 * 
 * @author ruoyi
 */
@Data
public class PaperQuestionAddBO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 试卷ID
     */
    @NotNull(message = "试卷ID不能为空")
    private Integer paperId;

    /**
     * 题目ID列表
     */
    @NotEmpty(message = "题目ID列表不能为空")
    private List<Integer> questionIds;
}



