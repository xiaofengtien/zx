package com.zx.student.archive.domain.paper;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zx.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 卷间中场配置实体
 * 
 * @author zx
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("paper_intermission")
public class PaperIntermission extends BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 中场配置ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 试卷ID
     */
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
     * 是否可以跳过（0-否，1-是，当前统一为0）
     */
    private Integer canSkip;
}



