package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 卷别数据BO
 * 
 * @author ruoyi
 */
@Data
public class VolumeDataBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 卷别ID (编辑时可能有值,新增时为空)
     */
    private Integer id;

    /**
     * 临时ID (前端生成,用于关联大题,格式如: "temp_volume_1")
     */
    private String tempId;

    /**
     * 卷别代码（A、B、C等，后端自动生成）
     */
    private String volumeCode;

    /**
     * 卷别名称（如：试卷A、试卷B）
     */
    @NotBlank(message = "卷别名称不能为空")
    private String volumeName;

    /**
     * 卷别顺序（1,2,3...）
     */
    @NotNull(message = "卷别顺序不能为空")
    private Integer volumeOrder;

    /**
     * 卷别名称音频URL
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
    private List<SectionDataBO> sections;
}
