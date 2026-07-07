package com.zx.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 从试卷移除题目参数
 * 
 * @author zx
 */
@Data
public class PaperQuestionRemoveBO implements Serializable {
    
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



