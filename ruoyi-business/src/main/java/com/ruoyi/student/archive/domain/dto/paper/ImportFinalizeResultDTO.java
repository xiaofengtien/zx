package com.ruoyi.student.archive.domain.dto.paper;

import lombok.Data;

import java.io.Serializable;

/**
 * 导入完成结果DTO
 *
 * @author ruoyi
 */
@Data
public class ImportFinalizeResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 创建的试卷ID
     */
    private Integer paperId;

    /**
     * 创建的题目数量
     */
    private Integer questionCount;

    /**
     * 创建的卷别数量
     */
    private Integer volumeCount;
}
