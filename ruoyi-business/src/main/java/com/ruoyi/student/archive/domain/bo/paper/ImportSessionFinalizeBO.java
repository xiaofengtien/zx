package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 完成导入会话的请求参数
 *
 * @author ruoyi
 */
@Data
public class ImportSessionFinalizeBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 会话标识
     */
    @NotBlank(message = "会话标识不能为空")
    private String sessionKey;

    /**
     * 试卷名称
     */
    @NotBlank(message = "试卷名称不能为空")
    private String paperName;

    /**
     * 题目分类ID
     */
    @NotNull(message = "题目分类不能为空")
    private Long categoryId;

    /**
     * 学科ID
     */
    @NotNull(message = "学科不能为空")
    private Integer subjectId;

    /**
     * 默认题目类型（-1表示使用解析结果）
     */
    private Integer defaultQuestionType = -1;

    /**
     * 是否仅导入听力部分
     */
    private Boolean listeningOnly = false;
}
