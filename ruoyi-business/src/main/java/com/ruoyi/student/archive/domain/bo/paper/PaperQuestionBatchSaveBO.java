package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 批量保存试卷题目关联参数
 * 
 * @author ruoyi
 */
@Data
public class PaperQuestionBatchSaveBO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 试卷ID
     */
    @NotNull(message = "试卷ID不能为空")
    private Integer paperId;

    /**
     * 大题ID
     */
    @NotNull(message = "大题ID不能为空")
    private Integer sectionId;

    /**
     * 题目列表
     */
    private List<QuestionItem> questionList;

    /**
     * 题目项
     */
    @Data
    public static class QuestionItem implements Serializable {
        /**
         * 题目ID
         */
        @NotNull(message = "题目ID不能为空")
        private Integer questionId;

        /**
         * 在大题中的顺序
         */
        private Integer sectionOrder;

        /**
         * 分值
         */
        private BigDecimal score;
    }
}


