package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

/**
 * 分片上传初始化BO
 * 
 * @author ruoyi
 */
@Data
public class ChunkUploadInitBO {
    
    /**
     * 试卷ID
     */
    @NotNull(message = "试卷ID不能为空")
    private Integer paperId;
    
    /**
     * 文件名
     */
    @NotNull(message = "文件名不能为空")
    private String fileName;
    
    /**
     * 文件大小（字节）
     */
    @NotNull(message = "文件大小不能为空")
    @Positive(message = "文件大小必须大于0")
    private Long fileSize;
    
    /**
     * 分片大小（字节），默认10MB
     */
    @Positive(message = "分片大小必须大于0")
    private Long chunkSize = 10L * 1024 * 1024; // 默认10MB
}









