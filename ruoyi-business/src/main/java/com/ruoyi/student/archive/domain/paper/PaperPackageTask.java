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
 * 试卷包生成任务实体
 * 
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@TableName("paper_package_task")
public class PaperPackageTask extends BaseEntity implements Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * 任务ID（主键）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 试卷ID
     */
    private Integer paperId;

    /**
     * 试卷名称（用于任务显示）
     */
    private String paperName;

    /**
     * 当前版本号（任务开始时试卷的版本号）
     */
    private Integer currentVersion;

    /**
     * 生成的新版本号（成功时返回）
     */
    private Integer newVersion;

    /**
     * 任务状态：PENDING-等待中，RUNNING-执行中，SUCCESS-成功，FAILED-失败，CANCELLED-已取消
     */
    private String status;

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
     * 开始时间（时间戳，毫秒）
     */
    private Long startTime;

    /**
     * 完成时间（时间戳，毫秒）
     */
    private Long finishTime;

    /**
     * OSS上传ID（用于断点续传）
     */
    private String uploadId;

    /**
     * OSS对象键（文件路径，用于断点续传）
     */
    private String objectKey;

    /**
     * 已上传的分片列表（JSON字符串，用于断点续传）
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
}









