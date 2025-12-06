package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * 试卷完整数据提交BO
 * 用于新增和编辑试卷时一次性提交所有数据
 * 
 * @author ruoyi
 */
@Data
public class PaperFullDataBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 试卷基本信息
     */
    @Valid
    @NotNull(message = "试卷基本信息不能为空")
    private PaperBasicInfoBO paper;

    /**
     * 卷别列表（嵌套结构：卷别 -> 大题 -> 题目）
     */
    @Valid
    private List<VolumeDataBO> volumes;

    /**
     * 中场配置列表
     */
    @Valid
    private List<IntermissionDataBO> intermissions;
}
