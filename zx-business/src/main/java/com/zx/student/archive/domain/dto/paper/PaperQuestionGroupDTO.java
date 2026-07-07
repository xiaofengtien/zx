package com.zx.student.archive.domain.dto.paper;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 题目组DTO（用于返回给前端）
 */
@Data
public class PaperQuestionGroupDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 题目组ID
     */
    private Integer id;

    /**
     * 所属大题ID
     */
    private Integer sectionId;

    /**
     * 组顺序
     */
    private Integer groupOrder;

    /**
     * 选中的题目ID列表
     */
    private List<Integer> selectedQuestionIds;

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
}
