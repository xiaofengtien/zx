package com.zx.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 题目数据BO
 * 
 * @author zx
 */
@Data
public class QuestionDataBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 题目ID
     */
    @NotNull(message = "题目ID不能为空")
    private Integer questionId;

    /**
     * 所属大题ID (如果有实际ID则为Integer，临时ID则为String)
     */
    private Object sectionId;

    /**
     * 所属大题临时ID (前端提交时使用)
     */
    private String sectionTempId;

    /**
     * 在大题中的顺序
     */
    @NotNull(message = "题目顺序不能为空")
    private Integer sectionOrder;

    /**
     * 分值
     */
    @NotNull(message = "题目分值不能为空")
    private BigDecimal score;
}
