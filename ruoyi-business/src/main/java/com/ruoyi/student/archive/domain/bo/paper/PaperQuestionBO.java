package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 试卷题目关联参数（用于导入时关联）
 * 
 * @author ruoyi
 */
@Data
public class PaperQuestionBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 关联ID
     */
    private Integer id;

    /**
     * 大题临时ID（用于导入时关联）
     */
    private String sectionTempId;

    /**
     * 在大题中的顺序
     */
    private Integer sectionOrder;

    /**
     * 题目ID（关联 question.id）
     */
    private Integer questionId;

    /**
     * 题目分数
     */
    private BigDecimal score;
}
