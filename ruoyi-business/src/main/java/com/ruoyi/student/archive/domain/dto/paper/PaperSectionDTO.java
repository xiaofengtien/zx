package com.ruoyi.student.archive.domain.dto.paper;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * 大题DTO（嵌套结构，包含题目列表）
 */
@Data
public class PaperSectionDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 大题ID
     */
    private Integer id;

    /**
     * 试卷ID
     */
    private Integer paperId;

    /**
     * 所属卷别ID（关联paper_volume.id）
     */
    private Integer volumeId;

    /**
     * 卷别代码（A、B、C等，保留用于显示和兼容）
     */
    private String volumeCode;

    /**
     * 大题名称（如：第一节、第二节）
     */
    private String sectionName;

    /**
     * 大题顺序（1,2,3...，在同一卷内排序）
     */
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
     * 音频播放次数（每道题有音频的情况下播放多少次，默认1次）
     */
    private Integer audioPlayCount;

    /**
     * 该大题下的题目列表（嵌套结构）
     */
    private List<PaperQuestionDTO> questions;

    /**
     * 该大题下的题目组列表（嵌套结构）
     */
    private List<PaperQuestionGroupDTO> questionGroups;
}
