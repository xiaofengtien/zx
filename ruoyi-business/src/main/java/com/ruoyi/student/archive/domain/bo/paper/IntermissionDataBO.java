package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;

/**
 * 中场配置数据BO
 * 
 * @author ruoyi
 */
@Data
public class IntermissionDataBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 中场配置ID (编辑时可能有值,新增时为空)
     */
    private Integer id;

    /**
     * 来源卷别ID（关联paper_volume.id，新增时可能为临时ID）
     */
    private Integer fromVolumeId;

    /**
     * 目标卷别ID（关联paper_volume.id，新增时可能为临时ID）
     */
    private Integer toVolumeId;

    /**
     * 来源卷别临时ID（用于新增时关联，生成真实ID后替换）
     */
    private String fromVolumeTempId;

    /**
     * 目标卷别临时ID（用于新增时关联，生成真实ID后替换）
     */
    private String toVolumeTempId;

    /**
     * 来源卷别（如：A，保留用于显示和兼容性）
     */
    private String fromVolume;

    /**
     * 目标卷别（如：B，保留用于显示和兼容性）
     */
    private String toVolume;

    /**
     * 中场提示文案
     */
    private String intermissionText;

    /**
     * 中场音频URL
     */
    private String intermissionAudioUrl;

    /**
     * 中场音频本地路径
     */
    private String intermissionAudioPath;

    /**
     * 中场音频时长（秒）
     */
    private Integer intermissionAudioDuration;

    /**
     * 是否可以跳过（0-否，1-是）
     */
    private Integer canSkip;
}
