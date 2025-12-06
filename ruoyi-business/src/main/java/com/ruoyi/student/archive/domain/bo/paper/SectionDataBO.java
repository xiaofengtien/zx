package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 大题数据BO
 * 
 * @author ruoyi
 */
@Data
public class SectionDataBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 大题ID (编辑时可能有值,新增时为空)
     */
    private Integer id;

    /**
     * 临时ID (前端生成,用于关联题目,格式如: "temp_section_1")
     */
    private String tempId;

    /**
     * 所属卷别ID (如果有实际ID则为Integer，临时ID则为String)
     */
    private Object volumeId;

    /**
     * 所属卷别临时ID (前端提交时使用)
     */
    private String volumeTempId;

    /**
     * 卷别代码（A、B、C等，保留用于显示）
     */
    private String volumeCode;

    /**
     * 大题名称（如：第一节、第二节）
     */
    @NotBlank(message = "大题名称不能为空")
    private String sectionName;

    /**
     * 大题顺序（1,2,3...，在同一卷内排序）
     */
    @NotNull(message = "大题顺序不能为空")
    private Integer sectionOrder;

    /**
     * 题目数量（自动计算）
     */
    private Integer questionCount;

    /**
     * 总分（自动计算）
     */
    private BigDecimal totalScore;

    /**
     * 每题分数（如果统一）
     */
    private BigDecimal scorePerQuestion;

    /**
     * 大题说明文字
     */
    private String instructionText;

    /**
     * 大题说明音频URL
     */
    private String instructionAudioUrl;

    /**
     * 大题说明音频本地路径
     */
    private String instructionAudioPath;

    /**
     * 大题说明音频时长（秒）
     */
    private Integer instructionAudioDuration;

    /**
     * 该大题下的题目列表（嵌套结构）
     */
    @Valid
    private List<QuestionDataBO> questions;
}
