package com.zx.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 试卷题目组参数
 * 
 * @author zx
 */
@Data
public class PaperQuestionGroupBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 题目组ID（修改时必填）
     */
    private Integer id;

    /**
     * 临时ID（用于导入时关联，格式：temp_grp_1）
     */
    private String tempId;

    /**
     * 大题ID
     */
    @NotNull(message = "大题ID不能为空")
    private Integer sectionId;

    /**
     * 大题临时ID（用于导入时关联）
     */
    private String sectionTempId;

    /**
     * 关联的题目组ID
     */
    private Integer questionGroupId;

    /**
     * 组顺序
     */
    @NotNull(message = "组顺序不能为空")
    private Integer groupOrder;

    /**
     * 起始题号（如67）
     */
    private Integer startQuestionNum;

    /**
     * 结束题号（如69）
     */
    private Integer endQuestionNum;

    /**
     * 组音频URL
     */
    private String audioUrl;

    /**
     * 组音频路径
     */
    private String audioPath;

    /**
     * 音频时长（秒）
     */
    private Integer audioDuration;

    /**
     * 组说明文本
     */
    private String introText;

    /**
     * 选中的题目ID列表（前端传入为数组，后端存储为JSON字符串）
     */
    private java.util.List<Integer> selectedQuestionIds;

    /**
     * 题目组名称
     */
    private String groupName;

    /**
     * 组答题时间
     */
    private Integer answerTime;
}
