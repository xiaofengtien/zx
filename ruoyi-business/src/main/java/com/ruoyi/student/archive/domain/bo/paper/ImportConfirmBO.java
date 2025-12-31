package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;
import java.io.Serializable;

/**
 * 导入确认 BO
 *
 * @author ruoyi
 */
@Data
public class ImportConfirmBO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 试卷名称（用户可修改）
     */
    private String paperName;

    /**
     * 试卷编码（用户可修改）
     */
    private String paperCode;

    /**
     * 业务类型
     */
    private Integer businessType;

    /**
     * 业务ID
     */
    private Integer businessId;

    /**
     * 题目分类ID
     */
    private Integer questionCategoryId;

    /**
     * 完整的试卷数据（复用现有结构）
     */
    private PaperFullDataBO fullData;

    /**
     * 是否只导入听力部分
     */
    private Boolean listeningOnly;

    /**
     * 音频切分策略（vad/asr）
     */
    private String audioSplitStrategy;
}
