package com.ruoyi.student.archive.domain.paper;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.io.Serializable;

/**
 * 试卷卷别实体
 * 
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("paper_volume")
public class PaperVolume extends BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 卷别ID
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 试卷ID
     */
    private Integer paperId;

    /**
     * 卷别代码（A、B、C等）
     */
    private String volumeCode;

    /**
     * 卷别名称（如：试卷A、试卷B）
     */
    private String volumeName;

    /**
     * 卷别顺序（1,2,3...）
     */
    private Integer volumeOrder;

    /**
     * 卷别名称音频URL（播放试卷名称）
     */
    private String volumeAudioUrl;

    /**
     * 卷别名称音频本地路径
     */
    private String volumeAudioPath;

    /**
     * 卷别名称音频时长（秒）
     */
    private Integer volumeAudioDuration;
}



