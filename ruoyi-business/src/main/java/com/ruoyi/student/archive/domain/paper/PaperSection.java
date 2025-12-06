package com.ruoyi.student.archive.domain.paper;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 试卷大题实体
 * 
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("paper_section")
public class PaperSection extends BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 大题ID
     */
    @TableId(type = IdType.AUTO)
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


}


