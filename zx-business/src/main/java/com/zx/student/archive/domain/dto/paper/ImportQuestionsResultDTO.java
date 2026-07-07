package com.zx.student.archive.domain.dto.paper;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 批量导入题目的结果 DTO
 *
 * @author zx
 */
@Data
public class ImportQuestionsResultDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 消息
     */
    private String message;

    /**
     * 创建的题目数量
     */
    private Integer createdCount;

    /**
     * 创建的题目 ID 列表
     */
    private List<Integer> questionIds;

    /**
     * 试卷结构数据（用于跳转到试卷编辑页面时预填充）
     * 包含卷别、大题、题目的完整结构
     */
    private ParseResultDTO paperStructure;
}
