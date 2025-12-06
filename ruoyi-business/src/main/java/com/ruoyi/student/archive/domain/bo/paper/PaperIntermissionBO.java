package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 试卷中场配置参数
 * 
 * @author ruoyi
 */
@Data
public class PaperIntermissionBO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 中场配置ID（修改时必填）
     */
    private Integer id;

    /**
     * 试卷ID
     */
    @NotNull(message = "试卷ID不能为空")
    private Integer paperId;

    /**
     * 来源卷别ID（关联paper_volume.id）
     */
    private Integer fromVolumeId;

    /**
     * 目标卷别ID（关联paper_volume.id）
     */
    private Integer toVolumeId;

    /**
     * 来源卷别（A、B、C等，保留用于显示和兼容性）
     */
    private String fromVolume;

    /**
     * 目标卷别（A、B、C等，保留用于显示和兼容性）
     */
    private String toVolume;

    /**
     * 中场提示文字
     */
    private String intermissionText;

    /**
     * 是否允许跳过（0-否，1-是）
     */
    private Integer canSkip;
}



