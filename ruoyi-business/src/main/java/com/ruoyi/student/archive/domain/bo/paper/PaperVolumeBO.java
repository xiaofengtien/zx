package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 试卷卷别参数
 * 
 * @author ruoyi
 */
@Data
public class PaperVolumeBO implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 卷别ID（修改时必填）
     */
    private Integer id;

    /**
     * 试卷ID
     */
    @NotNull(message = "试卷ID不能为空")
    private Integer paperId;

    /**
     * 卷别代码（A、B、C等，可选，后端会自动生成）
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
}


