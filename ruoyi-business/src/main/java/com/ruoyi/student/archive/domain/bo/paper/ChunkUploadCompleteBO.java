package com.ruoyi.student.archive.domain.bo.paper;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 分片上传完成BO
 * 
 * @author ruoyi
 */
@Data
public class ChunkUploadCompleteBO {
    
    /**
     * 上传ID（由初始化接口返回）
     */
    @NotNull(message = "上传ID不能为空")
    private String uploadId;
}









