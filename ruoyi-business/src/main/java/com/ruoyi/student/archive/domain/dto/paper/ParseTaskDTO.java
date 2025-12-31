package com.ruoyi.student.archive.domain.dto.paper;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 解析任务状态 DTO
 */
@Data
public class ParseTaskDTO implements Serializable {

    /**
     * 任务ID
     */
    private String taskId;

    /**
     * 任务状态: PENDING-等待中, PROCESSING-处理中, COMPLETED-完成, FAILED-失败
     */
    private String status;

    /**
     * 进度百分比 (0-100)
     */
    private Integer progress;

    /**
     * 当前处理步骤描述
     */
    private String currentStep;

    /**
     * 错误信息（失败时）
     */
    private String errorMessage;

    /**
     * 解析结果（完成时）
     */
    private ParseResultDTO result;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;
}
