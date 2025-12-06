package com.ruoyi.student.archive.domain.dto.paper;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.enums.PackageTaskStatus;
import lombok.Data;

import java.io.Serializable;

/**
 * 试卷包生成任务信息
 * 
 * @author ruoyi
 */
@Data
public class PackageTaskInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 试卷ID
     */
    private Integer paperId;

    /**
     * 任务状态
     */
    private PackageTaskStatus status;

    /**
     * 进度（0-100）
     */
    private Integer progress;

    /**
     * 当前步骤描述
     */
    private String currentStep;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 开始时间（时间戳）
     */
    private Long startTime;

    /**
     * 完成时间（时间戳）
     */
    private Long finishTime;

    /**
     * 生成的新版本号（成功时返回）
     */
    private Integer newVersion;

    /**
     * 当前版本号（任务开始时试卷的版本号，用于任务显示）
     */
    private Integer currentVersion;

    /**
     * 试卷名称（用于任务显示）
     */
    private String paperName;

    /**
     * OSS上传ID（用于断点续传）
     */
    private String uploadId;

    /**
     * OSS对象键（文件路径，用于断点续传）
     */
    private String objectKey;

    /**
     * 已上传的分片列表（用于断点续传）
     * 格式：JSON字符串，包含已上传分片的partNumber和eTag
     */
    private String uploadedParts;

    /**
     * 文件大小（字节，用于断点续传）
     */
    private Long fileSize;

    /**
     * 分片大小（字节，用于断点续传）
     */
    private Long chunkSize;

    /**
     * 任务线程（用于取消）
     * 注意：此字段不序列化到JSON，避免无限递归
     */
    @JsonIgnore
    private transient Thread taskThread;
}
