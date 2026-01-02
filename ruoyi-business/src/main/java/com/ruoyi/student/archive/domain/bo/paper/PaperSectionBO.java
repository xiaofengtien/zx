package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 试卷大题参数
 * 
 * @author ruoyi
 */
@Data
public class PaperSectionBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 大题ID（修改时必填）
     */
    private Integer id;

    /**
     * 临时ID（用于导入时关联，格式：temp_sec_1）
     */
    private String tempId;

    /**
     * 试卷ID
     */
    private Integer paperId;

    /**
     * 所属卷别ID（关联paper_volume.id）
     */
    private Integer volumeId;

    /**
     * 所属卷别临时ID（用于导入时关联，格式：temp_vol_1）
     */
    private String volumeTempId;

    /**
     * 卷别代码（A、B、C等，保留用于显示和兼容）
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
     * 作答时间（单位：秒，默认5秒）
     */
    private Integer answerTime;

    /**
     * 音频播放次数（每道题有音频的情况下播放多少次，默认1次）
     */
    private Integer audioPlayCount;

    /**
     * 题目列表（用于导入时关联）
     */
    private List<PaperQuestionBO> questions;

    /**
     * 题目组列表（用于保存时包含题目组信息）
     */
    private List<PaperQuestionGroupBO> questionGroups;
}
