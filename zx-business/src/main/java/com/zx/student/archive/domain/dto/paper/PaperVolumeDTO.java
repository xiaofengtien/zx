package com.zx.student.archive.domain.dto.paper;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 卷别DTO（嵌套结构，包含大题列表）
 */
@Data
public class PaperVolumeDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 卷别ID
     */
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

    /**
     * 该卷别下的大题列表（嵌套结构）
     */
    private List<PaperSectionDTO> sections;
}









